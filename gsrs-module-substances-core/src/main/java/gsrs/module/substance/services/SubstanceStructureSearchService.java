package gsrs.module.substance.services;

import com.fasterxml.jackson.annotation.JsonValue;
import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.molwitch.Atom;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.structureIndexer.StructureIndexer;
import gsrs.cache.GsrsCache;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.repository.MixtureSubstanceRepository;
import gsrs.module.substance.repository.ModificationRepository;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.utils.SanitizerUtil;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Structure;
import ix.core.search.SearchResultContext;
import ix.core.search.SearchResultProcessor;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.*;
import ix.seqaln.SequenceIndexer;
import ix.utils.UUIDUtil;
import ix.utils.Util;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class SubstanceStructureSearchService {
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchRequest{

        private String q;

        private Integer top;

        private Integer skip;

        private Integer fdim;

        private Double cutoff;

        private StructureSearchType type;

        private String order;

        private String field;
        /*

        String q =      getLastStringOrElse(params.get("q"), null);
        String type =   getLastStringOrElse(params.get("type"), "sub");
        Double co =     getLastDoubleOrElse(params.get("cutoff"), 0.8);
        Integer top =   getLastIntegerOrElse(params.get("top"), 10);
        Integer skip=   getLastIntegerOrElse(params.get("skip"), 0);
        Integer fdim =  getLastIntegerOrElse(params.get("fdim"), 10);
        String field =  getLastStringOrElse(params.get("field"), "");
         */

        public SanitizedSearchRequest sanitize(){
            return new SanitizedSearchRequest(this);
        }
    }

    //TODO right now the key/signature hash key is computed inside the controller
    //maybe we should move it to here but it requires the http request to get facet info
    public enum StructureSearchType{

        SUBSTRUCTURE("sub"),
        SIMILARITY("sim"),
        FLEX("flex"),
        EXACT("exact")
        ;

        private final String value;

        private static Map<String, StructureSearchType> lookup = new ConcurrentHashMap<>();
        static{
            for(StructureSearchType t : values()){
                lookup.put(t.value, t);
            }
        }
        private StructureSearchType(String value){
            this.value = value;
        }

        public static StructureSearchType parseType(String type) {
            if(type ==null){
                return null;
            }
            return lookup.computeIfAbsent(type, t->{
                for(StructureSearchType s : values()){
                    if(s.value.equalsIgnoreCase(type)){
                        return s;
                    }
                }
                return null;
            });

        }

        @JsonValue
        public String getValue(){
            return value;
        }


    }
    @Getter
    @EqualsAndHashCode
    public static class SanitizedSearchRequest{
        private static final int DEFAULT_TOP =10;
        private static final int DEFAULT_FDIM =10;
        private static final double DEFAULT_CUTOFF = 0.8D;
        private static final StructureSearchType DEFAULT_TYPE = StructureSearchType.SUBSTRUCTURE;

        private static final String DEFAULT_FIELD= "";
        private String queryStructure;

        private int top;

        private int skip;

        private int fdim;
        private String field;
        private double cutoff;
        private String order;
        private StructureSearchType type;

        private SanitizedSearchRequest(SearchRequest request){
            this.top = SanitizerUtil.sanitizeNumber(request.top, DEFAULT_TOP);
            this.skip = SanitizerUtil.sanitizeNumber(request.skip, 0);
            this.fdim = SanitizerUtil.sanitizeNumber(request.fdim, DEFAULT_FDIM);
            this.cutoff = SanitizerUtil.sanitizeCutOff(request.cutoff, DEFAULT_CUTOFF);
            this.type = request.type ==null? StructureSearchType.SUBSTRUCTURE: request.getType();
            this.queryStructure = request.q ==null? null: request.q;//don't trim it breaks mol format!
            this.order = request.order;
            this.field = request.field ==null? DEFAULT_FIELD: request.field;
        }

        public static String getDefaultField() {
            return DEFAULT_FIELD;
        }

        public static int getDefaultTop() {
            return DEFAULT_TOP;
        }

        public static int getDefaultFdim() {
            return DEFAULT_FDIM;
        }

        public static double getDefaultCutoff() {
            return DEFAULT_CUTOFF;
        }

        public static StructureSearchType getDefaultType() {
            return DEFAULT_TYPE;
        }

        public Map<String,Object> getParameterMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("top", top);
            map.put("skip", skip);
            map.put("fdim", fdim);
            map.put("type", type.value);
            if(order !=null) {
                map.put("order", order);
            }
            if(!field.trim().isEmpty()){
                map.put("field", field.trim());
            }
            map.put("cutoff", cutoff);
            return map;
        }
    }
    @Autowired
    private StructureIndexerService structureIndexerService;
    @Autowired
    private StructureSearchConfiguration structureSearchConfiguration;
    @Autowired
    private ModificationRepository modificationRepository;
    @Autowired
    private SubstanceRepository substanceRepository;
    @Autowired
    private MixtureSubstanceRepository mixtureSubstanceRepository;
    @Autowired
    private GsrsCache gsrsCache;
    @Autowired
    private EntityManager entityManager;

    public SearchResultContext search(SanitizedSearchRequest request, String hashKey) throws Exception {

        return gsrsCache.getOrElse(structureIndexerService.lastModified(), hashKey, ()->{
            SearchResultProcessor<StructureIndexer.Result, Substance> processor = new StructureSearchResultProcessor(
                    structureSearchConfiguration,
                    modificationRepository,
                    substanceRepository,
                    mixtureSubstanceRepository,
                    gsrsCache,
                    entityManager);

            AutowireHelper.getInstance().autowire(processor);
            StructureIndexer.ResultEnumeration resultEnumeration=null;
            if(request.getType() == StructureSearchType.SUBSTRUCTURE) {
                resultEnumeration = structureIndexerService.substructure(request.getQueryStructure());
            }else if(request.getType() == StructureSearchType.SIMILARITY){
                resultEnumeration = structureIndexerService.similarity(request.getQueryStructure(), request.cutoff);
            }
            if(resultEnumeration ==null){
                throw new Exception("invalid request type "+ request.getType());
            }
            processor.setResults(1, resultEnumeration);
            SearchResultContext ctx = processor.getContext();
            ctx.setKey(hashKey);

            return ctx;

        });





    }
    @Slf4j
    public static class StructureSearchResultProcessor
            extends SearchResultProcessor<StructureIndexer.Result, Substance> {
        int index;
        public static EntityUtils.EntityInfo<ChemicalSubstance> chemMeta = EntityUtils.getEntityInfoFor(ChemicalSubstance.class);
        public static EntityUtils.EntityInfo<MixtureSubstance> mixMeta = EntityUtils.getEntityInfoFor(MixtureSubstance.class);
        public static EntityUtils.EntityInfo<Substance> subMeta = EntityUtils.getEntityInfoFor(Substance.class);
        public static EntityUtils.EntityInfo<Modifications> modMeta = EntityUtils.getEntityInfoFor(Modifications.class);

        private StructureSearchConfiguration structureSearchConfiguration;
        private ModificationRepository modificationRepository;
        private SubstanceRepository substanceRepository;
        private MixtureSubstanceRepository mixtureSubstanceRepository;
        private GsrsCache gsrsCache;
        private EntityManager entityManager;

        public StructureSearchResultProcessor(StructureSearchConfiguration structureSearchConfiguration,
                                              ModificationRepository modificationRepository,
                                               SubstanceRepository substanceRepository,
                                              MixtureSubstanceRepository mixtureSubstanceRepository,
                                              GsrsCache gsrsCache,
                                              EntityManager entityManager
                                              ) {
            this.structureSearchConfiguration  = Objects.requireNonNull(structureSearchConfiguration);
            this.modificationRepository = Objects.requireNonNull(modificationRepository);
            this.substanceRepository = Objects.requireNonNull(substanceRepository);
            this.mixtureSubstanceRepository = Objects.requireNonNull(mixtureSubstanceRepository);
            this.gsrsCache = Objects.requireNonNull(gsrsCache);
            this.entityManager = Objects.requireNonNull(entityManager);
        }

        @Override
        public Stream<? extends Substance> map(StructureIndexer.Result result) {
            try{
                Substance r=instrument(result);
                if(r==null)return Stream.empty();

                StreamUtil.StreamConcatter< Substance> sstream = StreamUtil.with(Stream.of(r));

                if(!structureSearchConfiguration.includePolymers()){
                    if(r instanceof PolymerSubstance){
                        return Stream.empty();
                    }
                }

                if(structureSearchConfiguration.includeModifications()){
                    //add modifications results as well
                    //This is likely to be a source of slow-down
                    //due to possibly missing indexes
                    List<Modifications> modlist = modificationRepository.findByStructuralModifications_molecularFragment_refuuid(result.getId());


                    Stream<Substance> rStream = modlist.stream().map(m -> {
                                Substance ff = substanceRepository.findByModifications_Uuid(m.uuid);
                                //TODO shouldn't this return empty stream if null?
                                return ff;
                            }
                    );
                    sstream = sstream.and(rStream);
                }


                if(structureSearchConfiguration.includeMixtures()){
                    //add mixture results as well
                    List<Substance> mixlist = new ArrayList<>(mixtureSubstanceRepository.findByMixture_Components_Substance_Refuuid(result.getId()));
                    sstream = sstream.and(mixlist.stream());
                }
                return sstream.stream();
            }catch(Exception e){
                log.error("error processing record", e);
                return Stream.empty();
            }
        }

        protected Substance instrument(StructureIndexer.Result r) throws Exception {

            EntityUtils.Key k = EntityUtils.Key.of(subMeta, UUID.fromString(r.getId()));

            Optional<EntityUtils.EntityWrapper<?>> efetch = k.fetch(entityManager);


            if (efetch.isPresent()) {
                Substance chem = (Substance) efetch.get().getValue();
                Map<String, Object> matchingContext = new HashMap<>();

                double similarity = r.getSimilarity();
                log.debug(String.format("%1$ 5d: matched %2$s %3$.3f", ++index, r.getId(), r.getSimilarity()));
                Chemical mol = r.getMol();


//                int[] amap = r.getHits();
                int[] amap = new int[mol.getAtomCount()];
                int i = 0, nmaps = 0;
                for (Atom ma : mol.getAtoms()) {
                    amap[i] = ma.getAtomToAtomMap().orElse(0);
                    if (amap[i] > 0) {
                        ++nmaps;
                    }
                    ++i;
                }
                if (nmaps > 0) {
                    matchingContext.put("atomMaps", amap);
                }
                matchingContext.put("similarity", similarity);
                //Util.debugSpin(100);
                EntityUtils.EntityWrapper<?> ew = EntityUtils.EntityWrapper.of(chem);
                gsrsCache.setMatchingContext(this.getContext().getId(), ew.getKey(), matchingContext);
                return chem;
            }
            return null;
        }
    }
}
