package gsrs.module.substance.controllers;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.nih.ncats.molwitch.io.CtTableCleaner;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.repository.StructureRepository;
import gov.nih.ncats.common.io.IOUtil;
import gov.nih.ncats.molwitch.Atom;
import gov.nih.ncats.molwitch.Bond;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.Bond.Stereo;
import gsrs.cache.GsrsCache;
import gsrs.controller.*;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.repository.SubunitRepository;
import gsrs.module.substance.services.ReindexService;
import gsrs.module.substance.services.SubstanceSequenceSearchService;
import gsrs.module.substance.services.SubstanceStructureSearchService;
import gsrs.repository.EditRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.hasAdminRole;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import gsrs.springUtils.GsrsSpringUtils;
import ix.core.EntityMapperOptions;
import ix.core.chem.*;
import ix.core.controllers.EntityFactory;
import ix.core.models.Payload;
import ix.core.models.Structure;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.*;
import ix.seqaln.SequenceIndexer;
import ix.utils.CallableUtil;
import ix.utils.UUIDUtil;
import ix.utils.Util;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * GSRS Rest API controller for the {@link Substance} entity.
 */
@DependsOn("SubstanceSequenceSearchService")
@Slf4j
@ExposesResourceFor(Substance.class)
@GsrsRestApiController(context = SubstanceEntityServiceImpl.CONTEXT,  idHelper = IdHelpers.UUID)
public class SubstanceController extends EtagLegacySearchEntityController<SubstanceController, Substance, UUID> {

    private static interface SimpleStandardizer{
        public Chemical standardize(Chemical c);
        public static SimpleStandardizer REMOVE_HYDROGENS() {
            return (c)->{
                c.removeNonDescriptHydrogens();
                return c;
            };
        }
        public static SimpleStandardizer ADD_HYDROGENS() {
            return (c)->{
                // TODO:
                // In CDK, this doesn't generate coordinates for the Hs, meaning you have to have an additional
                // clean call. Also, this method doesn't do anything for query molecules in CDK.
                // 
                // Both of the above problems will need to be fixed for this to work well.
                //
                
                c.makeHydrogensExplicit();
                return c;
            };
        }
        public static SimpleStandardizer STEREO_FLATTEN() {
            return (c)->{
                Chemical cc = c.copy();


                cc.getAllStereocenters().forEach(sc->{
                    Atom aa =sc.getCenterAtom();
                    @SuppressWarnings("unchecked")
                    Stream<Bond> sbonds = (Stream<Bond>) aa.getBonds().stream();

                    sbonds.forEach(bb->{
                        if(bb.getBondType().getOrder()==1) {
                            if(!bb.getStereo().equals(Stereo.NONE)) {
                                bb.setStereo(Stereo.NONE);
                            }
                        }
                    });
                });


                try {
                    //TODO molwitch bug makes this export/import
                    //necessary
                    return Chemical.parseMol(cc.toMol());
                } catch (IOException e) {
                    return cc;
                } 
            };
        }
        public static SimpleStandardizer CLEAN() {
            return (c)->{
                try {
                    ChemAligner.align2DClean(c);
//                    c.generateCoordinates();
                } catch (Exception e) {
//                    e.printStackTrace();
                }
                return c;
            };
        }
        
        public default SimpleStandardizer and(SimpleStandardizer std2) {
            SimpleStandardizer _this=this;
            return (c)->{
                return std2.standardize(_this.standardize(c));
            };            
        }
        
        public default String standardize(String mol) {
            
            try {
                Chemical c=Chemical.parseMol(mol);
                c=this.standardize(c);
                return c.toMol();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                
                return mol;
            }
        
        }
        
    }
    //TODO: could be its own microservice?
    private static enum StructureStandardizerPresets{
        REMOVE_HYDROGENS(SimpleStandardizer.REMOVE_HYDROGENS()),
        ADD_HYDROGENS(SimpleStandardizer.ADD_HYDROGENS()),
        STEREO_FLATTEN(SimpleStandardizer.STEREO_FLATTEN()),
        CLEAN(SimpleStandardizer.CLEAN());        
        public SimpleStandardizer std;        
        StructureStandardizerPresets(SimpleStandardizer s){
            this.std=s;
        }        
        public SimpleStandardizer getStandardizer() {
            return this.std;
        }
    }
    @Autowired
    private SubstanceSequenceSearchService substanceSequenceSearchService;
    
    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private EditRepository editRepository;
    @Autowired
    private SubstanceLegacySearchService legacySearchService;

    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private PayloadService payloadService;

    @Autowired
    private GsrsCache ixCache;

    @Autowired
    private EntityLinks entityLinks;

    @Autowired
    private SubunitRepository subunitRepository;

    @Autowired
    private SubstanceStructureSearchService substanceStructureSearchService;



    @Autowired
    private GsrsEntityService<Substance, UUID> substanceEntityService;
    @Override
    protected LegacyGsrsSearchService<Substance> getlegacyGsrsSearchService() {
        return legacySearchService;
    }

    @Override
    protected GsrsEntityService<Substance, UUID> getEntityService() {
        return substanceEntityService;
    }

    @Override
    protected Optional<EditRepository> editRepository() {
        return Optional.of(editRepository);
    }

    @Override
    protected Stream<Substance> filterStream(Stream<Substance> stream,boolean publicOnly, Map<String, String> parameters) {
        if(publicOnly){
            return stream.filter(s-> s.getAccess().isEmpty());
        }
        return stream;
    }

    public static Optional<HttpServletRequest> getCurrentHttpRequest() {
        return
                Optional.ofNullable(
                        RequestContextHolder.getRequestAttributes()
                )
                        .filter(ServletRequestAttributes.class::isInstance)
                        .map(ServletRequestAttributes.class::cast)
                        .map(ServletRequestAttributes::getRequest);
    }

    public String getSmiles(String id) {
        return getSmiles(id, 0);
    }

    public String getSmiles(String id, int max) {
        if (id != null) {
            String seq=null;
            if(!UUIDUtil.isUUID(id)){
                seq= id;
            }else{
                Structure structure=structureRepository.findById(UUID.fromString(id)).orElse(null);

                if(structure!=null){
                    seq = structure.smiles;
                }
            }

            if (seq != null) {
                seq = seq.replaceAll("[\n\t\\s]", "");
                if (max > 0 && max + 3 < seq.length()) {
                    return seq.substring(0, max) + "...";
                }
                return seq;
            }
        }
        return id;
    }
    public static String getKey (String q, double t) {
        return Util.sha1(q) + "/"+String.format("%1$d", (int)(1000*t+.5));
    }

    public Optional<String> getKeyForCurrentRequest(HttpServletRequest request){



        String query = request.getParameter("q") + request.getParameter("order");
        String type = request.getParameter("type");



        log.debug("checkStatus: q=" + query + " type=" + type);
        if (type != null && request.getParameter("q") != null) {
            try {
                String key = null;
                if (type.toLowerCase().startsWith("sub")) {
                    String sq = getSmiles(request.getParameter("q"));
                    key = "substructure/"+ Util.sha1(sq + request.getParameter("order"));
                }
                else if (type.toLowerCase().startsWith("sim")) {
                    String c = request.getParameter("cutoff");
                    String sq = getSmiles(request.getParameter("q"));
                    key = "similarity/"+getKey (sq + request.getParameter("order"), Double.parseDouble(c));
                }
                else if (type.toLowerCase().startsWith("seq")) {
                    String iden = request.getParameter("identity");
                    if (iden == null) {
                        iden = "0.5";
                    }
                    String idenType = request.getParameter("identityType");
                    if(idenType==null){
                        idenType="GLOBAL";
                    }
                    key = "sequence/"+getKey (getSequence(request.getParameter("q")) +idenType + request.getParameter("order"), Double.parseDouble(iden));

                }else if(type.toLowerCase().startsWith("flex")) {
                    String sq = getSmiles(request.getParameter("q"));
                    key = "flex/"+Util.sha1(sq + request.getParameter("order"));

                    return Optional.of(signature (key, request));
                }else if(type.toLowerCase().startsWith("exact")) {
                    String sq = getSmiles(request.getParameter("q"));
                    key = "exact/"+Util.sha1(sq + request.getParameter("order"));
                    return Optional.of(signature (key, request));
                }else{
                    key = type + "/"+Util.sha1(query);
                }

                return Optional.of(Util.sha1(key));

            }catch (Exception ex) {
                log.error("Error creating key for request" , ex);
            }
        }else {

            return Optional.ofNullable(signature (query, request));
        }
        return Optional.empty();
    }

    private String getSequence(String q) throws IOException {
        if(UUIDUtil.isUUID(q)){
            Payload p = new Payload();
            p.id = UUID.fromString(q);
            Optional<InputStream> opt = payloadService.getPayloadAsUncompressedInputStream(p);
            if(opt.isPresent()){
                return new String(IOUtil.toByteArray( opt.get()), "utf-8");
            }
        }
        return q;
    }

    public static String signature (String q, HttpServletRequest request) {
        Map<String, String[]> query = request.getParameterMap();
        List<String> qfacets = new ArrayList<String>();
        if (query.get("facet") != null) {
            for (String f : query.get("facet"))
                qfacets.add(f);

        }
        final boolean hasFacets = q != null
                && q.indexOf('/') > 0 && q.indexOf("\"") < 0;
        if (hasFacets) {
            // treat this as facet
            qfacets.add("MeSH/"+q);
            query.put("facet", qfacets.toArray(new String[0]));
        }
        //query.put("drill", new String[]{"down"});

        List<String> args = new ArrayList<String>();
        args.add(request.getRequestURI());
        if (q != null)
            args.add(q);
        for (String f : qfacets)
            args.add(f);

        if (query.get("order") != null) {
            for (String f : query.get("order"))
                args.add(f);
        }

        String dep = query.getOrDefault("showDeprecated", new String[]{"false"})[0];
        args.add("dep" + dep);



        Collections.sort(args);
        return Util.sha1(args.toArray(new String[0]));
    }

    @PostGsrsRestApiMapping("/structureSearch")
    public Object structureSearchPost(@NotNull @RequestBody SubstanceStructureSearchService.SearchRequest request,
                                                      @RequestParam(value="sync", required= false, defaultValue="true") boolean sync,
                                                      @RequestParam Map<String, String> queryParameters,
                                                      HttpServletRequest httpRequest,
                                      RedirectAttributes attributes) throws IOException, ExecutionException, InterruptedException {

        SubstanceStructureSearchService.SanitizedSearchRequest sanitizedRequest = request.sanitize();

        Optional<Structure> structure = parseStructureQuery(sanitizedRequest.getQueryStructure(), true);
        if(!structure.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(queryParameters, "query structure not found : " + sanitizedRequest.getQueryStructure());
        }
        httpRequest.setAttribute(
                View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.FOUND);

        attributes.mergeAttributes(sanitizedRequest.getParameterMap());
        attributes.addAttribute("q", structure.get().id.toString());
        return new ModelAndView("redirect:/api/v1/substances/structureSearch");
    }
    @GetGsrsRestApiMapping("/structureSearch")
    public ResponseEntity<Object> structureSearchGet(
            @RequestParam(required = false) String q, @RequestParam(required = false) String type, @RequestParam(required = false, defaultValue = "0.9") Double cutoff,
            @RequestParam(required = false) Integer top, @RequestParam(required = false) Integer skip, @RequestParam(required = false) Integer fdim, @RequestParam(required = false) String field,

            @RequestParam(value = "sync", required = false, defaultValue = "true") boolean sync,
            @RequestParam Map<String, String> queryParameters,
            HttpServletRequest httpServletRequest) throws Exception {

        Optional<String> hashKey = getKeyForCurrentRequest(httpServletRequest);

        Optional<Structure> structure = parseStructureQuery(q, false);
        if(!structure.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(queryParameters, "query structure not found : " + q);
        }

        String cleaned = CtTableCleaner.clean(structure.get().molfile);

        SubstanceStructureSearchService.SanitizedSearchRequest sanitizedRequest = SubstanceStructureSearchService.SearchRequest.builder()
                        .queryStructure(cleaned)
                        .type(SubstanceStructureSearchService.StructureSearchType.parseType(type))
                        .cutoff(cutoff)
                        .fdim(fdim)
                        .top(top)
                        .skip(skip)
                        .field(field)
                        .build()
                        .sanitize();
        SearchResultContext resultContext=null;
        if(sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.SUBSTRUCTURE) {
            resultContext = substanceStructureSearchService.substructureSearch(sanitizedRequest, hashKey.get());
        }
        //TODO add other search types here

        //we have to manually set the actual request uri here as it's the only place we know it!!
        //for some reason the spring boot methods to get the current quest  URI don't include the parameters
        //so we have to append them manually here from our controller
        StringBuilder queryParamBuilder = new StringBuilder();
        queryParameters.forEach((k,v)->{
            if(queryParamBuilder.length()==0){
                queryParamBuilder.append("?");
            }else{
                queryParamBuilder.append("&");
            }
            queryParamBuilder.append(k).append("=").append(v);
        });
        resultContext.setGeneratingUrl(resultContext.getGeneratingUrl() + queryParamBuilder);
        //TODO move to service
        SearchResultContext focused = resultContext.getFocused(sanitizedRequest.getTop(), sanitizedRequest.getSkip(), sanitizedRequest.getFdim(), sanitizedRequest.getField());
        return substanceFactoryDetailedSearch(focused, sync);
    }

    private Optional<Structure> parseStructureQuery(String q, boolean store) throws IOException {
        if(UUIDUtil.isUUID(q)){
            String json = (String) ixCache.getTemp(q);
            if(json !=null){
                return Optional.of(EntityUtils.getEntityInfoFor(Structure.class).fromJson(json));
            }
            //it's a UUID that isn't a temp structure try the database
            Optional<Structure> opt = structureRepository.findById(UUID.fromString(q));
            return opt;

        }
        Structure struc= structureProcessor.instrument(q);


            if(store){

                if(struc.id ==null){
                    struc.id = UUID.randomUUID();
                }
                ixCache.setTemp(struc.id.toString(), EntityUtils.EntityWrapper.of(struc).toInternalJson());
            }
        return Optional.of(struc);
    }

    //context: String, q: String ?= null, type: String ?= "GLOBAL", cutoff: Double ?= .9, top: Int ?= 10, skip: Int ?= 0, fdim: Int ?= 10, field: String ?= "", seqType: String ?="Protein")
    @GetGsrsRestApiMapping("/sequenceSearch")
    public ResponseEntity<Object> sequenceSearchGet(
            @RequestParam(required= false) String q,  @RequestParam(required= false) String type, @RequestParam(required= false,  defaultValue = "0.9") Double cutoff,
            @RequestParam(required= false) Integer top, @RequestParam(required= false) Integer skip, @RequestParam(required= false) Integer fdim, @RequestParam(required= false) String field,
            @RequestParam(required= false, defaultValue = "Protein") String seqType,
                                                 @RequestParam(value="sync", required= false, defaultValue="true") boolean sync,
            @RequestParam Map<String, String> queryParameters,
            HttpServletRequest httpServletRequest) throws IOException, ExecutionException, InterruptedException {

        String sequenceQuery = convertQueryStringToSequence(q);
        SubstanceSequenceSearchService.SequenceSearchRequest request = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q(sequenceQuery)
                .type(SequenceIndexer.CutoffType.valueOfOrDefault(type))
                .cutoff(cutoff)
                .top(top)
                .skip(skip)
                .fdim(fdim)
                .field(field)
                .seqType(seqType)
                .build();

        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sanitizedRequest = request.sanitize();
        SearchResultContext resultContext = substanceSequenceSearchService.search(sanitizedRequest);
        //we have to manually set the actual request uri here as it's the only place we know it!!
        //for some reason the spring boot methods to get the current quest  URI don't include the parameters
        //so we have to append them manually here from our controller
        StringBuilder queryParamBuilder = new StringBuilder();
        queryParameters.forEach((k,v)->{
            if(queryParamBuilder.length()==0){
                queryParamBuilder.append("?");
            }else{
                queryParamBuilder.append("&");
            }
            queryParamBuilder.append(k).append("=").append(v);
        });
        resultContext.setGeneratingUrl(resultContext.getGeneratingUrl() + queryParamBuilder);
        //TODO move to service
        SearchResultContext focused = resultContext.getFocused(sanitizedRequest.getTop(), sanitizedRequest.getSkip(), sanitizedRequest.getFdim(), sanitizedRequest.getField());
        return substanceFactoryDetailedSearch(focused, sync);
    }

    private String convertQueryStringToSequence(@RequestParam(required = false) String q) {
        String sequenceQuery = q;
        if(UUIDUtil.isUUID(q)){
            //query is a uuid of a subunit look it up
            String json = (String) ixCache.getTemp(q);

            if(json !=null){
                //get as Subunit
                Subunit subunit = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().convertValue(json, Subunit.class);
                sequenceQuery = subunit.sequence;
            }else{
                Optional<Subunit> opt = subunitRepository.findById(UUID.fromString(q));
                if(opt.isPresent()){
                    sequenceQuery = opt.get().sequence;
                }
            }
        }
        return sequenceQuery;
    }

    @PostGsrsRestApiMapping("/sequenceSearch")
    public ResponseEntity<Object> sequenceSearchPost(@NotNull @RequestBody SubstanceSequenceSearchService.SequenceSearchRequest request,
                                         @RequestParam(value="sync", required= false, defaultValue="true") boolean sync, @RequestParam Map<String, String> queryParameters) throws IOException, ExecutionException, InterruptedException {

        String querySequence= convertQueryStringToSequence(request.getQ());
        request.setQ(querySequence);
        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sanitizedRequest = request.sanitize();
/*
        String q = sanitizedRequest.getQ();
        if(!UUIDUtil.isUUID(q)){
            //store the query as a temp in the cache
            Subunit s = new Subunit();
            s.sequence = q;
            s.subunitIndex = 1;
            s.uuid = UUID.randomUUID();
            ixCache.setTemp(s.uuid.toString(), EntityUtils.EntityWrapper.of(s).toFullJson());
            sanitizedRequest.setQ(s.uuid.toString());
        }

        HttpHeaders headers = new HttpHeaders();
//                String url = .toUri().toString();
        //this feels wierd that we are do a re-direct to anothe method in this same controller but if we do a direct method call
        //our request() object won't change to what we need to pull the data out of the request object and cache ?
        StringBuilder builder = new StringBuilder();
        for(Map.Entry<String,String> entry : sanitizedRequest.toMap().entrySet()){
            if(builder.length()==0){
                builder.append("?");
            }else{
                builder.append("&");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        headers.add("Location", GsrsLinkUtil.adapt(entityLinks.linkFor(Substance.class)
                .slash("sequenceSearch" + builder.toString())
                .withSelfRel())
                .toUri().toString() );
        return new ResponseEntity<>(headers,HttpStatus.FOUND);
        */
        SearchResultContext resultContext = substanceSequenceSearchService.search(sanitizedRequest);
        //TODO move to service
        SearchResultContext focused = resultContext.getFocused(sanitizedRequest.getTop(), sanitizedRequest.getSkip(), sanitizedRequest.getFdim(), sanitizedRequest.getField());
        return substanceFactoryDetailedSearch(focused, sync);
    }
    static String getOrderedKey (SearchResultContext context, SearchRequest request) {
        return "fetchResult/"+context.getId() + "/" + request.getOrderedSetSha1();
    }
    static String getKey (SearchResultContext context, SearchRequest request) {
        return "fetchResult/"+context.getId() + "/" + request.getDefiningSetSha1();
    }

    public ResponseEntity<Object> substanceFactoryDetailedSearch(SearchResultContext context, boolean sync) throws InterruptedException, ExecutionException {
        context.setAdapter((srequest, ctx) -> {
            try {
                SearchResult sr = getResultFor(ctx, srequest,true);

                List<Substance> rlist = new ArrayList<Substance>();

                sr.copyTo(rlist, srequest.getOptions().getSkip(), srequest.getOptions().getTop(), true); // synchronous
                for (Substance s : rlist) {

                    s.setMatchContextProperty(ixCache.getMatchingContextByContextID(ctx.getId(), EntityUtils.EntityWrapper.of(s).getKey()));
                }
                return sr;
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("Error fetching search result", e);
            }
        });


        if (sync) {
            try {

                context.getDeterminedFuture().get(1, TimeUnit.MINUTES);

//                return play.mvc.Controller.redirect(context.getResultCall());
                HttpHeaders headers = new HttpHeaders();
//                String url = .toUri().toString();
                headers.add("Location", GsrsLinkUtil.adapt(context.getKey(),entityLinks.linkFor(SearchResultContext.class).slash(context.getKey()).slash("result").withSelfRel())
                        .toUri().toString() );
                return new ResponseEntity<>(headers,HttpStatus.FOUND);
            } catch (TimeoutException e) {
                log.warn("Structure search timed out!", e);
            }
        }
        return new ResponseEntity<>(context, HttpStatus.OK);
    }

    public SearchResult getResultFor(SearchResultContext ctx, SearchRequest req, boolean preserveOrder)
            throws IOException, Exception{

        final String key = (preserveOrder)? getOrderedKey(ctx,req):getKey (ctx, req);

        CallableUtil.TypedCallable<SearchResult> tc = CallableUtil.TypedCallable.of(() -> {
            Collection results = ctx.getResults();
            SearchRequest request = new SearchRequest.Builder()
                    .subset(results)
                    .options(req.getOptions())
                    .skip(0)
                    .top(results.size())
                    .query(req.getQuery())
                    .build();


            SearchResult searchResult =null;

            if (results.isEmpty()) {
                searchResult= SearchResult.createEmptyBuilder(req.getOptions())
                        .build();
            }else{
                //katzelda : run it through the text indexer for the facets?
//                searchResult = SearchFactory.search (request);
                searchResult = legacySearchService.search(null, request.getOptions(), request.getSubset());
                log.debug("Cache misses: "
                        +key+" size="+results.size()
                        +" class="+searchResult);
            }

            // make an alias for the context.id to this search
            // result
            searchResult.setKey(ctx.getId());
            return searchResult;
        }, SearchResult.class);

        if(ctx.isDetermined()) {
            return ixCache.getOrElse(key, tc);
        }else {
            return tc.call();
        }
    }




        @PostGsrsRestApiMapping("/interpretStructure")
    public ResponseEntity<Object> interpretStructure(@NotBlank @RequestBody String mol, @RequestParam Map<String, String> queryParameters){
        String[] standardize = Optional.ofNullable(queryParameters.get("standardize"))
                                     .orElse("NONE")
                                     .split(",");
        SimpleStandardizer simpStd=Arrays.stream(standardize)
                .filter(s->!s.equals("NONE"))
           .map(val->val.toUpperCase())
           .map(val->StructureStandardizerPresets.valueOf(val))
           .map(std->std.getStandardizer())
           .reduce(SimpleStandardizer::and).orElse(null);
        
        try {
            String payload = ChemCleaner.getCleanMolfile(mol);
            List<Structure> moieties = new ArrayList<>();
            ObjectMapper mapper = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER();
            ObjectNode node = mapper.createObjectNode();
            try {
                Structure struc = structureProcessor.instrument(payload, moieties, false); // don't
                // standardize!
                // we should be really use the PersistenceQueue to do this
                // so that it doesn't block
                // in fact, it probably shouldn't be saving this at all
                if (payload.contains("\n") && payload.contains("M  END")) {
                    struc.molfile = payload;
                }
                
                if(simpStd!=null) {
                    struc.molfile=simpStd.standardize(struc.molfile);
                }
                
                
                ArrayNode an = mapper.createArrayNode();
                for (Structure m : moieties) {
                    // m.save();
                    saveTempStructure(m);
                    ObjectNode on = mapper.valueToTree(m);
                    Amount c1 = Moiety.intToAmount(m.count);
                    JsonNode amt = mapper.valueToTree(c1);
                    on.set("countAmount", amt);
                    an.add(on);
                }
                node.put("structure", mapper.valueToTree(struc));
                node.put("moieties", an);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Collection<PolymerDecode.StructuralUnit> o = PolymerDecode.DecomposePolymerSU(payload, true);
                for (PolymerDecode.StructuralUnit su : o) {
                    Structure struc = structureProcessor.instrument(su.structure, null, false);
                    // struc.save();
                    saveTempStructure(struc);
                    su._structure = struc;
                }
                node.put("structuralUnits", mapper.valueToTree(o));
            } catch (Throwable e) {
                e.printStackTrace();
                log.error("Can't enumerate polymer", e);
            }
            return new ResponseEntity<>(node, HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Can't process payload", ex);
            return new ResponseEntity<>("Can't process mol payload",
                    this.getGsrsControllerConfiguration().getHttpStatusFor(HttpStatus.INTERNAL_SERVER_ERROR, queryParameters));
        }

    }

    public static void saveTempStructure(Structure s) {
        if (s.id == null){
            s.id = UUID.randomUUID();
        }
        //TODO save in EhCache
    }
    /*
     public static void saveTempStructure(Structure s){
        if(s.id==null)s.id=UUID.randomUUID();

        AccessLogger.info("{} {} {} {} \"{}\"",
                UserFetcher.getActingUser(true).username,
                "unknown",
                "unknown",
                "structure search:" + s.id,
                s.molfile.trim().replace("\n", "\\n").replace("\r", ""));
        //IxCache.setTemp(s.id.toString(), EntityWrapper.of(s).toFullJson());
        IxCache.setTemp(s.id.toString(), EntityWrapper.of(s).toInternalJson());
    }

    public static Result interpretStructure(String contextIgnored) {
            ObjectMapper mapper = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER();
            ObjectNode node = mapper.createObjectNode();
            try {
                String opayload = request().body().asText();
                String payload = ChemCleaner.getCleanMolfile(opayload);
                if (payload != null) {
                    List<Structure> moieties = new ArrayList<Structure>();
                    try {
                        Structure struc = StructureProcessor.instrument(payload, moieties, false); // don't
                        // standardize!
                        // we should be really use the PersistenceQueue to do this
                        // so that it doesn't block
                        // in fact, it probably shouldn't be saving this at all
                        if (payload.contains("\n") && payload.contains("M  END")) {
                            struc.molfile = payload;
                        }
                        StructureFactory.saveTempStructure(struc);
                        ArrayNode an = mapper.createArrayNode();
                        for (Structure m : moieties) {
                            // m.save();
                            StructureFactory.saveTempStructure(m);
                            ObjectNode on = mapper.valueToTree(m);
                            Amount c1 = Moiety.intToAmount(m.count);
                            JsonNode amt = mapper.valueToTree(c1);
                            on.set("countAmount", amt);
                            an.add(on);
                        }
                        node.put("structure", mapper.valueToTree(struc));
                        node.put("moieties", an);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        Collection<PolymerDecode.StructuralUnit> o = PolymerDecode.DecomposePolymerSU(payload, true);
                        for (PolymerDecode.StructuralUnit su : o) {
                            Structure struc = StructureProcessor.instrument(su.structure, null, false);
                            // struc.save();
                            StructureFactory.saveTempStructure(struc);
                            su._structure = struc;
                        }
                        node.put("structuralUnits", mapper.valueToTree(o));
                    } catch (Throwable e) {
                    	e.printStackTrace();
                        Logger.error("Can't enumerate polymer", e);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.error("Can't process payload", ex);
                return internalServerError("Can't process mol payload");
            }
            return ok(node);
        }
     */
}
