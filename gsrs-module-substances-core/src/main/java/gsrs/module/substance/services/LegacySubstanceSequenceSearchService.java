package gsrs.module.substance.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jcvi.jillion.core.Range;
import org.jcvi.jillion.core.Ranges;

import gov.nih.ncats.common.Tuple;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.repository.NucleicAcidSubstanceRepository;
import gsrs.module.substance.repository.ProteinSubstanceRepository;
import gsrs.module.substance.repository.SubunitRepository;
import gsrs.service.PayloadService;
import gsrs.springUtils.AutowireHelper;
import ix.core.search.ResultProcessor;
import ix.core.search.SearchResultContext;
import ix.core.search.SearchResultProcessor;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.Subunit;
import ix.seqaln.SequenceIndexer;
import ix.seqaln.service.SequenceIndexerService;
import ix.utils.CallableUtil;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class LegacySubstanceSequenceSearchService implements SubstanceSequenceSearchService{
    private static Pattern FASTA_FILE_PATTERN = Pattern.compile(">(.+)\\|.+");

    private SequenceIndexerService indexerService;

    private ProteinSubstanceRepository proteinSubstanceRepository;
    private NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository;
    private SubunitRepository subunitRepository;
    private GsrsCache ixCache;
    private PayloadService payloadService;


    private SubunitRepositoryWrapper proteinAdapter;
    private SubunitRepositoryWrapper naAdapter;
    

    public LegacySubstanceSequenceSearchService(SequenceIndexerService indexerService, GsrsCache ixCache,
                                                PayloadService payloadService,
                                                ProteinSubstanceRepository proteinSubstanceRepository,
                                                NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository,
                                                SubunitRepository subunitRepository
                                                ) {
        this.indexerService = indexerService;
        this.ixCache = ixCache;
        this.proteinSubstanceRepository = proteinSubstanceRepository;
        this.nucleicAcidSubstanceRepository = nucleicAcidSubstanceRepository;
        this.subunitRepository=subunitRepository;
        
        this.payloadService = payloadService;
        
        this.proteinAdapter = SubunitRepositoryWrapper.fromProtein(proteinSubstanceRepository, subunitRepository);
        this.naAdapter = SubunitRepositoryWrapper.fromNA(nucleicAcidSubstanceRepository, subunitRepository);
        
    }


    @Override
    public SearchResultContext search(SanitizedSequenceSearchRequest request) throws IOException {

        String hashKey = request.computeKey();
        try {
            return ixCache.getOrElse(indexerService.getLastModified() , hashKey, ()-> {
                SearchResultProcessor<SequenceIndexer.Result, ?> processor;
                
                if ("protein".equalsIgnoreCase(request.getSeqType())) {
                    processor = new GinasSequenceResultProcessor(proteinAdapter, ixCache);
                } else {
                    processor = new GinasSequenceResultProcessor(naAdapter, ixCache);
                }
               
                AutowireHelper.getInstance().autowire(processor);
                SequenceIndexer.ResultEnumeration resultEnumeration = indexerService.search(request.getQ(), request.getCutoff(), request.getType(), request.getSeqType());
                try {
                    processor.setResults(1, resultEnumeration);
                    SearchResultContext ctx = processor.getContext();
                    ctx.setKey(hashKey);

                    return ctx;
                } catch (Exception e) {
                    throw new IOException("error setting results", e);
                }
            });
        } catch (Exception e) {
            throw new IOException("error performing search ", e);
        }
    }
    public interface SearcherTask{
        public String getKey();
        public void search(ResultProcessor processor) throws Exception;
        public long getLastUpdatedTime();
    }
    //TODO add this back

//    public static abstract class SequenceSeachTask implements SearcherTask{
//
//        @Override
//        public String getKey() {
//            return App.getKeyForCurrentRequest();
//        }
//
//        @Override
//        public long getLastUpdatedTime() {
//            return EntityPersistAdapter
//                    .getSequenceIndexer()
//                    .lastModified();
//        }
//
//    }

    
    private interface SubunitRepositoryWrapper {
        public Optional<Substance> getSubstanceFromSubunitUUID(UUID uuid);
        public Optional<Subunit> getSubunitFromSubunitUUID(UUID uuid);
        public Optional<Substance> getSubstanceByID(UUID uuid);
        default Optional<Tuple<Substance,Subunit>> getSubstanceAndSubunitFromSubunitUUID(UUID uuid){
            return this.getSubunitFromSubunitUUID(uuid)
                        .map(su->Tuple.of(getSubstanceFromSubunitUUID(uuid),su))
                        .filter(t->t.k().isPresent())
                        .map(Tuple.kmap(op->op.get()));            
        }
        
        public static SubunitRepositoryWrapper fromNA(NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository, SubunitRepository subunitRepository) {
            return NucleicAcidSubunitRepositoryWrapper.builder()
            .nucleicAcidSubstanceRepository(nucleicAcidSubstanceRepository)
            .subunitRepository(subunitRepository)
            .build();
        }
        public static SubunitRepositoryWrapper fromProtein(ProteinSubstanceRepository proteinSubstanceRepository, SubunitRepository subunitRepository) {
            return ProteinSubunitRepositoryWrapper.builder()
            .proteinSubstanceRepository(proteinSubstanceRepository)
            .subunitRepository(subunitRepository)
            .build();
        }
    }
    
    @Builder
    private static class NucleicAcidSubunitRepositoryWrapper implements SubunitRepositoryWrapper{

        private NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository;
        private SubunitRepository subunitRepository;
        
        
        @Override
        public Optional<Substance> getSubstanceFromSubunitUUID(UUID uuid) {
            return nucleicAcidSubstanceRepository.findNucleicAcidSubstanceByNucleicAcid_Subunits_Uuid(uuid)
                    .stream()
                    .findFirst()
                    .map(na->(Substance)na);        
        }

        @Override
        public Optional<Subunit> getSubunitFromSubunitUUID(UUID uuid) {
            return subunitRepository.findById(uuid);
        }

        @Override
        public Optional<Substance> getSubstanceByID(UUID uuid) {
            return nucleicAcidSubstanceRepository.findById(uuid).map(s->(Substance)s);
        }
    }
    
    @Builder
    private static class ProteinSubunitRepositoryWrapper implements SubunitRepositoryWrapper{

        private ProteinSubstanceRepository proteinSubstanceRepository;
        private SubunitRepository subunitRepository;
        
        
        @Override
        public Optional<Substance> getSubstanceFromSubunitUUID(UUID uuid) {
            return proteinSubstanceRepository.findProteinSubstanceByProtein_Subunits_Uuid(uuid)
                    .stream()
                    .findFirst()
                    .map(na->(Substance)na);        
        }

        @Override
        public Optional<Subunit> getSubunitFromSubunitUUID(UUID uuid) {
            return subunitRepository.findById(uuid);
        }
        

        @Override
        public Optional<Substance> getSubstanceByID(UUID uuid) {
            return proteinSubstanceRepository.findById(uuid).map(s->(Substance)s);
        }
        
    }
    
    private SearchResultContext search(SearcherTask task, ResultProcessor processor) {
        try {
            final String key = task.getKey();
            return ixCache.getOrElse(task.getLastUpdatedTime(), key,
                    CallableUtil.TypedCallable.of(() -> {
                        task.search(processor);
                        SearchResultContext ctx = processor.getContext();
                        ctx.setKey(key);
                        return ctx;
                    },SearchResultContext.class));
        }catch (Exception ex) {
            ex.printStackTrace();
            log.error("Can't perform advanced search", ex);
            throw new IllegalStateException("Can't perform advanced search", ex);
        }
    }

    public static class GinasSequenceResultProcessor
            extends SearchResultProcessor<SequenceIndexer.Result, Substance> {
        private SubunitRepositoryWrapper subunitRepoWrapper;
        private GsrsCache ixCache;

        public GinasSequenceResultProcessor(SubunitRepositoryWrapper subunitRepoWrapper, GsrsCache ixCache) {
            this.subunitRepoWrapper = subunitRepoWrapper;
            
            this.ixCache = ixCache;
        }

        @Override
        protected Substance instrument(SequenceIndexer.Result r) throws Exception {
            

            //I don't understand the logic here ...
            //I don't think this does what it's supposed to.
            //katzelda - needed for large fasta sequences
            //but need tests to confirm
            if(r.id.startsWith(">")){
                Matcher m = FASTA_FILE_PATTERN.matcher(r.id);
                if(m.find()){
                    String parentId = m.group(1);
                    return subunitRepoWrapper.getSubstanceByID(UUID.fromString(parentId)).orElse(null);
                }
                return null; //? maybe should do something else
            }
            UUID subunitUUID = UUID.fromString(r.id);
            
            Optional<Tuple<Substance,Subunit>> substanceAndSubunit = subunitRepoWrapper.getSubstanceAndSubunitFromSubunitUUID(subunitUUID);
            
            Substance matched = substanceAndSubunit.map(t->{
                Substance sub = t.k();
                Subunit subunit = t.v();

                EntityUtils.Key key= EntityUtils.EntityWrapper.of(sub).getKey();
                Map<String,Object> added = ixCache.getMatchingContextByContextID(this.getContext().getId(), key);
                if(added==null){
                    added=new HashMap<String,Object>();
                }
                List<SequenceIndexer.Result> alignments = (List<SequenceIndexer.Result>)
                        added.computeIfAbsent("alignments", f->new ArrayList<SequenceIndexer.Result>());
                alignments.add(r);

                //GSRS 1512 add target site info
                // this is the only place in the alignment code we have the aligned sequence
                //AND we know what subunit it belongs to
                int index = subunit.subunitIndex==null? 0: subunit.subunitIndex;

                Range.RangeAndCoordinateSystemToStringFunction function = (begin,end, ignored)-> index+"_"+begin + "-" +index+"_"+end;
                r.alignments.stream().forEach(a->{
                    String shorthand = Ranges.asRanges(a.targetSites())
                            .stream()
                            .map(range-> range.toString(function, Range.CoordinateSystem.RESIDUE_BASED))
                            .collect(Collectors.joining(";","Target Sites: ","\n\n"));
                    //this check is because sometimes we get here twice?
                    if(a.alignment!=null && !a.alignment.startsWith("Target")) {
                        a.alignment = shorthand + a.alignment;
                    }
                });

                ixCache.setMatchingContext(this.getContext().getId(), key, added);
                return sub;
            }).orElse(null);
            
            if(!substanceAndSubunit.isPresent()) {
                log.warn("Can't retrieve protein for subunit " + r.id);
            }
            
            return matched;
        }
    }

}
