package gsrs.module.substance.services;

import gov.nih.ncats.common.Tuple;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.repository.NucleicAcidSubstanceRepository;
import gsrs.module.substance.repository.ProteinSubstanceRepository;
import gsrs.module.substance.repository.SubunitRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.search.SearchResultContext;
import ix.core.search.SearchResultProcessor;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.Subunit;
import ix.seqaln.SequenceIndexer;
import ix.seqaln.service.SequenceIndexerService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.jcvi.jillion.core.Range;
import org.jcvi.jillion.core.Ranges;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@link SubstanceSequenceSearchService}
 * implementation that performs sequence search like
 * the legacy GSRS 2.x code did using a lucene
 * index and home grown pair wise alignment code.
 *
 * This should eventually be replaced with something like BLAST.
 */
@Slf4j
public class LegacySubstanceSequenceSearchService implements SubstanceSequenceSearchService{
    /**
     * Fasta files will have a pattern of ">$SubstanceUUID | sequence id
     * In order to link the match context back to the substance so the alignment
     * is displayed in search results we need this regex to pull out the substance uuid.
     */
    private static Pattern FASTA_FILE_PATTERN = Pattern.compile(">(.+?)\\|(.+?)\\|(.+)");

    private SequenceIndexerService indexerService;

    private GsrsCache ixCache;


    private SubunitRepositoryWrapper proteinAdapter;
    private SubunitRepositoryWrapper naAdapter;
    

    public LegacySubstanceSequenceSearchService(SequenceIndexerService indexerService, GsrsCache ixCache,
                                                ProteinSubstanceRepository proteinSubstanceRepository,
                                                NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository,
                                                SubunitRepository subunitRepository
                                                ) {
        this.indexerService = indexerService;
        this.ixCache = ixCache;
        
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
               
                processor = AutowireHelper.getInstance().autowireAndProxy(processor);
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

    
    private interface SubunitRepositoryWrapper {
        Optional<Substance> getSubstanceFromSubunitUUID(UUID uuid);
        Optional<Subunit> getSubunitFromSubunitUUID(UUID uuid);
        Optional<Substance> getSubstanceByID(UUID uuid);
        default Optional<Tuple<Substance,Subunit>> getSubstanceAndSubunitFromSubunitUUID(UUID uuid){
            return this.getSubunitFromSubunitUUID(uuid)
                        .map(su->Tuple.of(getSubstanceFromSubunitUUID(uuid),su))
                        .filter(t->t.k().isPresent())
                        .map(Tuple.kmap(op->op.get()));            
        }
        
        static SubunitRepositoryWrapper fromNA(NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository, SubunitRepository subunitRepository) {
            return NucleicAcidSubunitRepositoryWrapper.builder()
            .nucleicAcidSubstanceRepository(nucleicAcidSubstanceRepository)
            .subunitRepository(subunitRepository)
            .build();
        }
        static SubunitRepositoryWrapper fromProtein(ProteinSubstanceRepository proteinSubstanceRepository, SubunitRepository subunitRepository) {
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



            //katzelda - needed for large fasta sequences uploaded as references the indexer saves id
            //in format ">substanceID | file ID | record ID
            if(r.id.startsWith(">")){
                Matcher m = FASTA_FILE_PATTERN.matcher(r.id);
                if(m.find()){
                    String parentId = m.group(1);
                    String fileName = m.group(2);
                    String recordId = m.group(3);
                    Substance s= subunitRepoWrapper.getSubstanceByID(UUID.fromString(parentId)).orElse(null);
                    //right now the result id is ">substanceID | file ID | record ID
                    //we need that to know what the sequence is but we don't want to show that to the user
                    //just say we have a match in that file name?

                    addAlignmentToSubstanceMatchContext(r.copyWithNewId(fileName), s, (begin, end, coordinateSystem)->{
                        return recordId+": "+begin + ".." +end;
                    });
                    return s;
                }
                return null; //? maybe should do something else
            }
            UUID subunitUUID = UUID.fromString(r.id);
            
            Optional<Tuple<Substance,Subunit>> substanceAndSubunit = subunitRepoWrapper.getSubstanceAndSubunitFromSubunitUUID(subunitUUID);
            
            Substance matched = substanceAndSubunit.map(t->{
                Substance sub = t.k();
                Subunit subunit = t.v();


                //GSRS 1512 add target site info
                // this is the only place in the alignment code we have the aligned sequence
                //AND we know what subunit it belongs to
                int index = subunit.subunitIndex==null? 0: subunit.subunitIndex;

                Range.RangeAndCoordinateSystemToStringFunction function = (begin,end, ignored)-> index+"_"+begin + "-" +index+"_"+end;
                addAlignmentToSubstanceMatchContext(r, sub, function);
                return sub;
            }).orElse(null);
            
            if(!substanceAndSubunit.isPresent()) {
                log.warn("Can't retrieve protein for subunit " + r.id);
            }
            
            return matched;
        }

        private void addAlignmentToSubstanceMatchContext(SequenceIndexer.Result r, Substance sub, Range.RangeAndCoordinateSystemToStringFunction function) {
            EntityUtils.Key key= EntityUtils.EntityWrapper.of(sub).getKey();

            Map<String,Object> added = ixCache.getMatchingContextByContextID(this.getContext().getId(), key);
            if(added==null){
                added=new HashMap<>();
            }
            List<SequenceIndexer.Result> alignments = (List<SequenceIndexer.Result>)
                    added.computeIfAbsent("alignments", f->new ArrayList<SequenceIndexer.Result>());
            alignments.add(r);

            r.alignments.stream().forEach(a->{
                 String shorthand = Ranges.asRanges(a.targetSites())
                         .stream()
                         .map(range-> range.toString(function, Range.CoordinateSystem.RESIDUE_BASED))
                         .collect(Collectors.joining("; ","Target Sites: ","\n\n"));
                 //this check is because sometimes we get here twice?
                 if(a.alignment!=null && !a.alignment.startsWith("Target")) {
                     a.alignment = shorthand + a.alignment;
                 }
             });

            ixCache.setMatchingContext(this.getContext().getId(), key.toRootKey(), added);
        }
    }

}
