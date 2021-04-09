package gsrs.module.substance.services;

import gsrs.module.substance.repository.NucleicAcidSubstanceRepository;
import gsrs.module.substance.repository.ProteinSubstanceRepository;
import gsrs.service.PayloadService;
import ix.core.cache.IxCache;
import ix.core.search.ResultProcessor;
import ix.core.search.SearchResultContext;
import ix.core.search.SearchResultProcessor;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.NucleicAcidSubstance;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Subunit;
import ix.seqaln.SequenceIndexer;
import ix.seqaln.service.LegacySequenceIndexerService;
import ix.utils.CallableUtil;
import lombok.extern.slf4j.Slf4j;
import org.jcvi.jillion.core.Range;
import org.jcvi.jillion.core.Ranges;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Slf4j
public class LegacySubstanceSequenceSearchService implements SubstanceSequenceSearchService{
    private static Pattern FASTA_FILE_PATTERN = Pattern.compile(">(.+)\\|.+");

    private LegacySequenceIndexerService indexerService;

    private ProteinSubstanceRepository proteinSubstanceRepository;
    private NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository;
    private IxCache ixCache;
    private PayloadService payloadService;

    public LegacySubstanceSequenceSearchService(LegacySequenceIndexerService indexerService, IxCache ixCache,
                                                PayloadService payloadService,
                                                ProteinSubstanceRepository proteinSubstanceRepository,
                                                NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository ) {
        this.indexerService = indexerService;
        this.ixCache = ixCache;
        this.proteinSubstanceRepository = proteinSubstanceRepository;
        this.nucleicAcidSubstanceRepository = nucleicAcidSubstanceRepository;
        this.payloadService = payloadService;
    }


    @Override
    public Object search(SanitizedSequenceSearchRequest request) throws IOException {

        //TODO save sequence as payload?

        SearchResultProcessor<SequenceIndexer.Result, ?> processor;
        if("protein".equalsIgnoreCase(request.getSeqType())){
            processor = new GinasSequenceResultProcessor(proteinSubstanceRepository, ixCache);
        }else{
            processor = new GinasNucleicSequenceResultProcessor(nucleicAcidSubstanceRepository, ixCache);
        }
        SequenceIndexer.ResultEnumeration resultEnumeration = indexerService.search(request.getQ(), request.getCutoff(), request.getType(), request.getSeqType());
        try {
            processor.setResults(1, resultEnumeration);
        } catch (Exception e) {
            throw new IOException("error setting results", e);
        }
        return null;
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
            extends SearchResultProcessor<SequenceIndexer.Result, ProteinSubstance> {
        private ProteinSubstanceRepository substanceRepository;
        private IxCache ixCache;

        public GinasSequenceResultProcessor(ProteinSubstanceRepository substanceRepository, IxCache ixCache) {
            this.substanceRepository = substanceRepository;
            this.ixCache = ixCache;
        }

        @Override
        protected ProteinSubstance instrument(SequenceIndexer.Result r) throws Exception {
            ProteinSubstance protein=null;

            //I don't understand the logic here ...
            //I don't think this does what it's supposed to.
            //katzelda - needed for large fasta sequences
            //but need tests to confirm
            if(r.id.startsWith(">")){
                Matcher m = FASTA_FILE_PATTERN.matcher(r.id);
                if(m.find()){
                    String parentId = m.group(1);
                    return substanceRepository.findById(UUID.fromString(parentId)).orElse(null);


                }
            }else {

                Optional<ProteinSubstance> proteinMatch = substanceRepository.findProteinSubstanceByProtein_Subunits_Uuid(UUID.fromString(r.id)).stream().findFirst();
                protein = !proteinMatch.isPresent() ? null : proteinMatch.get();
            }

            if (protein != null) {
                EntityUtils.Key key= EntityUtils.EntityWrapper.of(protein).getKey();
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
                UUID subunitUUID = UUID.fromString(r.id);
//                System.out.println("looking for subunit id = " + subunitUUID );
                Optional<Subunit> foundSubunit = protein.protein.getSubunits().stream()

                        .filter(sub-> Objects.equals(subunitUUID,sub.getUuid())).findAny();
//                System.out.println("found subunit ? ="+ foundSubunit);
                if(foundSubunit.isPresent()){
                    Subunit subunit = foundSubunit.get();
                    int index = subunit.subunitIndex==null? 0: subunit.subunitIndex;
//                    System.out.println("index = " + index);
                    Range.RangeAndCoordinateSystemToStringFunction function = (begin,end, ignored)-> index+"_"+begin + "-" +index+"_"+end;
                    r.alignments.stream().forEach(a->{
                        String shorthand = Ranges.asRanges(a.targetSites())
                                .stream()
                                .map(range-> range.toString(function, Range.CoordinateSystem.RESIDUE_BASED))
                                .collect(Collectors.joining(";","Target Sites: ","\n\n"));
//                            System.out.println("short hand -  " + shorthand);
                        //this check is because sometimes we get here twice?
                        if(a.alignment!=null && !a.alignment.startsWith("Target")) {
                            a.alignment = shorthand + a.alignment;
                        }
//                            System.out.println("new alignment =\n"+a.alignment);
                    });



                }

                ixCache.setMatchingContext(this.getContext().getId(), key, added);
            } else {
                log.warn("Can't retrieve protein for subunit " + r.id);
            }
            return protein;
        }
    }

    public static class GinasNucleicSequenceResultProcessor
            extends SearchResultProcessor<SequenceIndexer.Result, NucleicAcidSubstance> {

        private NucleicAcidSubstanceRepository substanceRepository;
        private IxCache ixCache;

        public GinasNucleicSequenceResultProcessor(NucleicAcidSubstanceRepository substanceRepository, IxCache ixCache) {
            this.substanceRepository = substanceRepository;
            this.ixCache = ixCache;
        }

        @Override
        protected NucleicAcidSubstance instrument(SequenceIndexer.Result r) throws Exception {
            NucleicAcidSubstance nuc= null;
//            if(r.id.startsWith(">")){
//                Matcher m = FASTA_FILE_PATTERN.matcher(r.id);
//                if(m.find()){
//                    String parentId = m.group(1);
//                    nuc = SubstanceFactory.nucfinder.get().byId(UUID.fromString(parentId));
//                }
//            }else {
            Optional<NucleicAcidSubstance> nucSubstance = substanceRepository.findNucleicAcidSubstanceByNucleicAcid_Subunits_Uuid(UUID.fromString(r.id))
                                                                                    .stream().findFirst() ; // also slow

            nuc = !nucSubstance.isPresent() ? null : nucSubstance.get();
//            }

            if (nuc != null) {
                EntityUtils.Key key= EntityUtils.EntityWrapper.of(nuc).getKey();
                Map<String,Object> added = ixCache.getMatchingContextByContextID(this.getContext().getId(), key);
                if(added==null){
                    added=new HashMap<>();
                }
                List<SequenceIndexer.Result> alignments = (List<SequenceIndexer.Result>)
                        added.computeIfAbsent("alignments", f->new ArrayList<SequenceIndexer.Result>());
                alignments.add(r);
                //GSRS 1512 add target site info
                // this is the only place in the alignment code we have the aligned sequence
                //AND we know what subunit it belongs to
                UUID subunitUUID = UUID.fromString(r.id);
//                System.out.println("looking for subunit id = " + subunitUUID );
                Optional<Subunit> foundSubunit = nuc.nucleicAcid.getSubunits().stream()
                        .filter(sub-> Objects.equals(subunitUUID,sub.getUuid())).findAny();
                if(foundSubunit.isPresent()) {
                    Subunit subunit = foundSubunit.get();
                    int index = subunit.subunitIndex == null ? 0 : subunit.subunitIndex;
//                    System.out.println("index = " + index);
                    Range.RangeAndCoordinateSystemToStringFunction function = (begin, end, ignored) -> index + "_" + begin + "-" + index + "_" + end;
                    r.alignments.stream().forEach(a -> {
                        String shorthand = Ranges.asRanges(a.targetSites())
                                .stream()
                                .map(range -> range.toString(function, Range.CoordinateSystem.RESIDUE_BASED))
                                .collect(Collectors.joining(";", "Target Sites: ", "\n\n"));
//                            System.out.println("short hand -  " + shorthand);
                        //this check is because sometimes we get here twice?
                        if (a.alignment != null && !a.alignment.startsWith("Target")) {
                            a.alignment = shorthand + a.alignment;
                        }
//                            System.out.println("new alignment =\n"+a.alignment);
                    });
                }
                ixCache.setMatchingContext(this.getContext().getId(), key, added);
            } else {
                log.warn("Can't retrieve nucleic for subunit " + r.id);
            }
            return nuc;
        }
    }
}
