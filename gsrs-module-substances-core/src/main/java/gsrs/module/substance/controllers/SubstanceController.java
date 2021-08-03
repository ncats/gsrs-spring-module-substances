package gsrs.module.substance.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.MolwitchException;
import gov.nih.ncats.molwitch.io.CtTableCleaner;
import gov.nih.ncats.molwitch.renderer.ChemicalRenderer;
import gov.nih.ncats.molwitch.renderer.RendererOptions;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.RendererOptionsConfig;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.hierarchy.SubstanceHierarchyFinder;
import gsrs.module.substance.repository.ChemicalSubstanceRepository;
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
import gsrs.module.substance.repository.SubstanceRepository;
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
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
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
    private enum StructureStandardizerPresets{
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
    private ChemicalSubstanceRepository chemicalSubstanceRepository;
    @Autowired
    private SubstanceRepository substanceRepository;
    @Autowired
    private GsrsEntityService<Substance, UUID> substanceEntityService;

    @Autowired
    private SubstanceHierarchyFinder substanceHierarchyFinder;


    @Autowired
    private RendererOptionsConfig rendererOptionsConfig;



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
//    @GetGsrsRestApiMapping("({ID}/@hierarchy")
//    public Object getHierarchy(@PathVariable("ID") String id){
//
//    }


    @Override
    protected Optional<Object> handleSpecialFields(EntityUtils.EntityWrapper<Substance> entity, String field) {
        if("@hierarchy".equals(field)){
            return Optional.of(makeJsonTreeForAPI(entity.getValue()));
        }
        return null;
    }

    private List<SubstanceHierarchyFinder.TreeNode2> makeJsonTreeForAPI(Substance sub) {

        List<SubstanceHierarchyFinder.TreeNode<Substance>> tnlist = substanceHierarchyFinder.getHierarchies(sub);


        SubstanceHierarchyFinder.TreeNode2Builder builder = new SubstanceHierarchyFinder.TreeNode2Builder();
        for (SubstanceHierarchyFinder.TreeNode<Substance> n : tnlist) {
            n.traverseDepthFirst(l -> {
                SubstanceHierarchyFinder.TreeNode<Substance> fin = l.get(l.size() - 1);
                String text = ("[" + fin.getValue().getApprovalIDDisplay() + "] "
                        + fin.getValue().getName()
                        + (fin.getType().equals(SubstanceHierarchyFinder.getHierarchyRootType()) ? "" : " {" + fin.getType() + "}")).toUpperCase();

                builder.addNode(text, fin.getType(), l.size() - 1, fin.getValue().asSubstanceReference());
//				System.out.println(text + "\n  " + namer.apply(fin) + "  depth = " + l.size() );
                return true;
            });
        }
        List<SubstanceHierarchyFinder.TreeNode2> nodes = builder.build();
//		try {
//			System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(nodes));
//		} catch (JsonProcessingException e) {
//			e.printStackTrace();
//		}

        return nodes;
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
        Map<String, String[]> query = new HashMap<>(request.getParameterMap());
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

        boolean isHashQuery = sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.EXACT ||
                sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.FLEX ;
        Optional<Structure> structure = parseStructureQuery(sanitizedRequest.getQueryStructure(), !isHashQuery);
        if(!structure.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(queryParameters, "query structure not found : " + sanitizedRequest.getQueryStructure());
        }
        httpRequest.setAttribute(
                View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.FOUND);


        attributes.mergeAttributes(sanitizedRequest.getParameterMap());
        if(isHashQuery){
            if(sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.EXACT){
                attributes.addAttribute("q", "root_structure_properties_term:"+structure.get().getExactHash());

            }else{
                attributes.addAttribute("q", structure.get().getStereoInsensitiveHash());

            }
            return new ModelAndView("redirect:/api/v1/substances/search");
        }else {
            attributes.addAttribute("q", structure.get().id.toString());
            return new ModelAndView("redirect:/api/v1/substances/structureSearch");
        }
    }
    @GetGsrsRestApiMapping("/structureSearch")
    public Object structureSearchGet(
            @RequestParam(required = false) String q, @RequestParam(required = false) String type, @RequestParam(required = false, defaultValue = "0.9") Double cutoff,
            @RequestParam(required = false) Integer top, @RequestParam(required = false) Integer skip, @RequestParam(required = false) Integer fdim, @RequestParam(required = false) String field,

            @RequestParam(value = "sync", required = false, defaultValue = "true") boolean sync,
            @RequestParam Map<String, String> queryParameters,
            HttpServletRequest httpServletRequest,
            HttpServletRequest httpRequest,
            RedirectAttributes attributes) throws Exception {

        Optional<String> hashKey = getKeyForCurrentRequest(httpServletRequest);

        Optional<Structure> structureOp = parseStructureQuery(q, true);
        if(!structureOp.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(queryParameters, "query structure not found : " + q);
        }
        Structure structure = structureOp.get();
        


        String cleaned = CtTableCleaner.clean(structure.molfile);


        SubstanceStructureSearchService.SanitizedSearchRequest sanitizedRequest = SubstanceStructureSearchService.SearchRequest.builder()
                        .q(cleaned)
                        .type(SubstanceStructureSearchService.StructureSearchType.parseType(type))
                        .cutoff(cutoff)
                        .fdim(fdim)
                        .top(top)
                        .skip(skip)
                        .field(field)
                        .build()
                        .sanitize();


        String hash=null;
        
        if(sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.EXACT){
            hash = "root_structure_properties_term:" + structure.getExactHash();
        }else if(sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.FLEX){
            //note we purposefully don't have the lucene path so it finds moieties and polymers etc
            hash= structure.getStereoInsensitiveHash();
        }

        if(hash !=null){
            httpRequest.setAttribute(
                    View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.FOUND);

            attributes.mergeAttributes(sanitizedRequest.getParameterMap());
            attributes.addAttribute("q", hash);
            attributes.addAttribute("includeBreakdown", false);
            //do a text search for that hash value?
            // This technically breaks things, but is probably okay for now
            //
            return new ModelAndView("redirect:/api/v1/substances/search");
        }
        SearchResultContext resultContext=null;
        if(sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.SUBSTRUCTURE
        || sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.SIMILARITY) {
            resultContext = substanceStructureSearchService.search(sanitizedRequest, hashKey.get());
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
            Optional<Structure> opt = GsrsSubstanceControllerUtil.getTempObject(ixCache, q, Structure.class);
            if(opt.isPresent()){
                return opt;
            }
            //it's a UUID that isn't a temp structure try the database
            return structureRepository.findById(UUID.fromString(q));

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

        Optional<String> hashKey = getKeyForCurrentRequest(httpServletRequest);
        //TODO use hashKey to store in ixcache
        Optional<Subunit> subunit = convertQueryStringToSequence(q, false);
        if(!subunit.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(queryParameters, "query sequence not found : " + q);

        }
        SubstanceSequenceSearchService.SequenceSearchRequest request = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q(subunit.get().sequence)
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

    private Optional<Subunit> convertQueryStringToSequence(@RequestParam(required = false) String q, boolean store) {

        if(UUIDUtil.isUUID(q)){
            //query is a uuid of a subunit look it up
            Optional<Subunit> opt = GsrsSubstanceControllerUtil.getTempObject(ixCache, q, Subunit.class);
            if(opt.isPresent()){
                return opt;
            }else{
                return subunitRepository.findById(UUID.fromString(q));
            }
        }

        Subunit sub = new Subunit();
        sub.uuid = UUID.randomUUID();
        sub.sequence = q;
        if(store){
            ixCache.setTemp(sub.uuid.toString(), EntityUtils.EntityWrapper.of(sub).toInternalJson());

        }
        return Optional.of(sub);
    }

    @PostGsrsRestApiMapping("/sequenceSearch")
    public Object sequenceSearchPost(@NotNull @RequestBody SubstanceSequenceSearchService.SequenceSearchRequest request,
                                         @RequestParam(value="sync", required= false, defaultValue="true") boolean sync,
                                                     @RequestParam Map<String, String> queryParameters,
                                                     HttpServletRequest httpRequest,
                                                     RedirectAttributes attributes) throws IOException, ExecutionException, InterruptedException {

        Optional<Subunit> querySequence= convertQueryStringToSequence(request.getQ(), true);
        if(!querySequence.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(queryParameters, request.getQ());
        }

        httpRequest.setAttribute(
                View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.FOUND);

        attributes.mergeAttributes(request.sanitize().toMap());
        attributes.addAttribute("q", querySequence.get().uuid.toString());
        return new ModelAndView("redirect:/api/v1/substances/sequenceSearch");

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
                    saveTempStructure(m);
                    ObjectNode on = mapper.valueToTree(m);
                    Amount c1 = Moiety.intToAmount(m.count);
                    JsonNode amt = mapper.valueToTree(c1);
                    on.set("countAmount", amt);
                    an.add(on);
                }
                saveTempStructure(struc);
                node.put("structure", mapper.valueToTree(struc));
                node.put("moieties", an);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Collection<PolymerDecode.StructuralUnit> o = PolymerDecode.DecomposePolymerSU(payload, true);
                for (PolymerDecode.StructuralUnit su : o) {
                    Structure struc = structureProcessor.instrument(su.structure, null, false);
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

    public void saveTempStructure(Structure s) {
        if (s.id == null){
            s.id = UUID.randomUUID();
        }
        ixCache.setTemp(s.id.toString(), EntityFactory.EntityMapper.INTERNAL_ENTITY_MAPPER().toJson(s));

    }


    @GetGsrsRestApiMapping({"/render({ID})", "/render/{ID}"})
    public Object render(@PathVariable("ID") String idOrSmiles,
                         @RequestParam(value = "format", required = false, defaultValue = "svg") String format,
                         //default stereo to empty string which spring returns as null Boolean object
                         @RequestParam(value = "stereo", required = false, defaultValue = "") Boolean stereo,
                         @RequestParam(value = "context", required = false) String contextId,
                         @RequestParam(value = "size", required = false, defaultValue = "150") int size,
                         @RequestParam Map<String, String> queryParameters) throws Exception {
        int[] amaps = null;
        String input=null;
        Substance actualSubstance = null;
        if (UUIDUtil.isUUID(idOrSmiles)) {
            //check cache?
            Optional<Structure> structure = GsrsSubstanceControllerUtil.getTempObject(ixCache, idOrSmiles,Structure.class);
            if(!structure.isPresent()) {
                UUID uuid = UUID.fromString(idOrSmiles);
                structure = structureRepository.findById(uuid);
                if (structure.isPresent()) {
                    if (structure.get() instanceof GinasChemicalStructure) {
                        ChemicalSubstance cs = chemicalSubstanceRepository.findByStructure_Id(structure.get().id);
                        if (cs != null) {
                            actualSubstance = cs;
                        }
                    }
                } else {
                    Optional<Substance> substance = substanceRepository.findById(uuid);
                    if (substance.isPresent()) {
                        actualSubstance = substance.get();
                        structure = actualSubstance.getStructureToRender();


                    }


                }
            }

            if (!structure.isPresent()) {
                if(actualSubstance ==null) {
                    //couldn't find a substance
                    return getGsrsControllerConfiguration().handleNotFound(queryParameters);
                }
                //if we're here, we have a substance but nothing to render return default for substance type
                return getDefaultImageFor(actualSubstance);
            }
            //context id is either a hash or a comma sep list of offsets
            if (contextId != null) {
                if (contextId.contains(",")) {
                    try {
                        amaps = Arrays.stream(contextId.split(","))
                                .mapToInt(Integer::parseInt)
                                .toArray();
                    } catch (Exception e) {
                        //ignore
                    }


                } else if (actualSubstance != null) {
                    actualSubstance.setMatchContextProperty(ixCache.getMatchingContextByContextID(contextId, EntityUtils.EntityWrapper.of(actualSubstance).getKey()));
                    amaps = actualSubstance.getMatchContextPropertyOr("atomMaps", null);

                }

            }
            input = fixMolIfNeeded(structure.get());
        }else {
            //string must be a smiles (or mol ?)
            input = idOrSmiles;
            if (contextId != null && contextId.contains(",")) {
                try {
                    amaps = Arrays.stream(contextId.split(","))
                            .mapToInt(Integer::parseInt)
                            .toArray();
                } catch (Exception e) {
                    //ignore
                }
            }
        }

        byte[] data = renderChemical(parseAndComputeCoordsIfNeeded(input), format, size, amaps, null, stereo);
        HttpHeaders headers = new HttpHeaders();

        headers.set("Content-Type", parseContentType(format));
        return new ResponseEntity<>(data, headers, HttpStatus.OK);

    }



    private Object getDefaultImageFor(Substance s) throws IOException {
        String placeholderFile = "polymer.svg";
        if (s != null) {
            switch (s.substanceClass) {
                case chemical:
                    placeholderFile = "chemical.svg";
                    break;
                case protein:
                    placeholderFile = "protein.svg";
                    break;
                case mixture:
                    placeholderFile = "mixture.svg";
                    break;
                case polymer:
                    placeholderFile = "polymer.svg";
                    break;
                case structurallyDiverse:
                    placeholderFile = "structurally-diverse.svg";
                    break;
                case concept:
                    placeholderFile = "concept.svg";
                    break;
                case nucleicAcid:
                    placeholderFile = "nucleic-acid.svg";
                    break;
                case specifiedSubstanceG1:
                    placeholderFile = "g1ss.svg";
                    break;
                default:
                    placeholderFile = "polymer.svg";
            }
        } else {
            placeholderFile = "noimage.svg";
        }

        HttpHeaders headers = new HttpHeaders();

        headers.set("Content-Type", parseContentType(placeholderFile.substring(placeholderFile.length()-3)));

        try(InputStream in = new ClassPathResource("images/" + placeholderFile).getInputStream()) {
            byte[] bytes = IOUtil.toByteArray(in);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        }

    }

    private static String parseContentType(String format){
        if("svg".equalsIgnoreCase(format)){
            return "image/svg+xml";
        }
        if("png".equalsIgnoreCase(format)){
            return MediaType.IMAGE_PNG_VALUE;
        }
        return MediaType.parseMediaType(format).toString();
    }
    private static String fixMolIfNeeded(Structure struc ){
        if(struc.molfile ==null){
            return struc.smiles;
        }
        try {
            return CtTableCleaner.clean(struc.molfile);
        } catch (IOException e) {
            e.printStackTrace();
            return struc.molfile;
        }
    }

    private byte[] renderChemical (Chemical chem, String format,
                           int size, int[] amap, Map<String, Boolean> newDisplay, Boolean drawStereo)
            throws Exception {

        try {
            RendererOptions rendererOptons = rendererOptionsConfig.getDefaultRendererOptions().copy();

            if (newDisplay != null) {

                rendererOptons.changeSettings(newDisplay);
            }


            //chem.reduceMultiples();
//		boolean highlight=false;
            if(amap!=null && amap.length>0){
                Atom[] atoms = chem.atoms().toArray(i -> new Atom[i]);
                for (int i = 0; i < Math.min(atoms.length, amap.length); ++i) {
                    atoms[i].setAtomToAtomMap(amap[i]);
                    if(amap[i]!=0){
                        rendererOptons.withSubstructureHighlight();
                    }
                }
            }else{
                if (chem.atoms().filter(Atom::hasAtomToAtomMap)
                        .findAny().isPresent()) {
                    rendererOptons.withSubstructureHighlight();
                }

            }
            preProcessChemical(chem, rendererOptons);

            if(Chem.isProblem(chem)){
                rendererOptons.setDrawOption(RendererOptions.DrawOptions.DRAW_STEREO_LABELS, false);

            }else{
                if (size > 250 /*&& !highlight*/) {
                    //katzelda March 21 2019
                    //after talking to Tyler we should just always
                    //turn on stereo labels because the renderer will determine if there's stereo
                    rendererOptons.setDrawOption(RendererOptions.DrawOptions.DRAW_STEREO_LABELS, true);


//				try{
//					if(chem.hasStereoIsomers()){
//						dp.changeProperty(DisplayParams.PROP_KEY_DRAW_STEREO_LABELS, true);
//					}
//				}catch(Exception e){
//					e.printStackTrace();
//					Logger.error("Can't generate stereo flags for structure", e);
//				}
                }
            }
            if (drawStereo != null) {
                rendererOptons.setDrawOption(RendererOptions.DrawOptions.DRAW_STEREO_LABELS, drawStereo);

            }
//		if("true".equals(r.getQueryString("stereo"))){
//			rendererOptons.setDrawOption(RendererOptions.DrawOptions.DRAW_STEREO_LABELS, true);
//
//		}else if("false".equals(r.getQueryString("stereo"))){
//			rendererOptons.setDrawOption(RendererOptions.DrawOptions.DRAW_STEREO_LABELS, false);
//
//		}

            rendererOptons.captionTop(c -> c.getProperty("TOP_TEXT"));
            rendererOptons.captionBottom(c -> c.getProperty("BOTTOM_TEXT"));



            Chem.fixMetals(chem);

            ChemicalRenderer renderer = new ChemicalRenderer(rendererOptons);

            ByteArrayOutputStream bos = new ByteArrayOutputStream ();

            if (format.equals("svg")) {
                SVGGraphics2D svg = new SVGGraphics2D
                        (bos, new Dimension(size, size));
                try {
                    svg.startExport();
                    renderer.render(svg, chem, 0, 0, size, size, false);
                    svg.endExport();
                } finally {
                    svg.dispose();
                }
            }else {
                BufferedImage bi = renderer.createImage(chem, size);
                ImageIO.write(bi, format, bos);
            }

            return bos.toByteArray();
        }catch(Exception e){
            e.printStackTrace();
            throw e;
        }
    }


    private static void preProcessChemical(Chemical c,  RendererOptions renderOptions){
        if(c!=null){


            boolean compColor=false;
            boolean fuse=false;
            boolean hasRgroups=hasRGroups(c);
            int rgroupColor=1;

            if(hasRgroups){
                if(fuse){
                    compColor=colorChemicalComponents(c);
                    if(compColor){
						/*
						dp.changeProperty(PROP_KEY_DRAW_HIGHLIGHT_MAPPED,true);
			dp.changeProperty(PROP_KEY_DRAW_HIGHLIGHT_MONOCHROMATIC,false);
			dp.DEF_STROKE_PERCENT=.095f;

			PROP_KEYS_VALUES.put( PROP_KEY_BOND_EXPECTED_LENGTH,DEF_BOND_AVG );
			 PROP_KEYS_VALUES.put( PROP_KEY_BOND_STROKE_WIDTH_FRACTION,DEF_STROKE_PERCENT );
			 PROP_KEYS_VALUES.put( PROP_KEY_ATOM_LABEL_FONT_FRACTION,DEF_FONT_PERCENT );
			 PROP_KEYS_VALUES.put( PROP_KEY_BOND_DOUBLE_GAP_FRACTION,DEF_DBL_BOND_GAP );
			 PROP_KEYS_VALUES.put( PROP_KEY_BOND_DOUBLE_LENGTH_FRACTION,DEF_DBL_BOND_DISTANCE );
			 PROP_KEYS_VALUES.put( PROP_KEY_ATOM_LABEL_BOND_GAP_FRACTION,DEF_FONT_GAP_PERCENT );
			 PROP_KEYS_VALUES.put( PROP_KEY_BOND_STEREO_WEDGE_ANGLE,DEF_WEDGE_ANG );
			 PROP_KEYS_VALUES.put( PROP_KEY_BOND_OVERLAP_SPACING_FRACTION,DEF_SPLIT_RATIO );
			 PROP_KEYS_VALUES.put( PROP_KEY_BOND_STEREO_DASH_NUMBER,DEF_NUM_DASH );
						 */
//						dp=dp.withSpecialColor2();
                        renderOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_HIGHLIGHT_MONOCHROMATIC, false);
                        renderOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_HIGHLIGHT_MAPPED, true);

                        renderOptions.setDrawPropertyValue(RendererOptions.DrawProperties.BOND_STROKE_WIDTH_FRACTION, .095F);

                    }
                    if(fuseChemical(c)){
                        try {
                            c.generateCoordinates();
                        }catch(Exception e){
                            e.printStackTrace();

                        }
                    }
                }
                if(rgroupColor==1){
                    if(!compColor && mapChemicalRgroup(c)){
						/*

			dp.changeProperty(PROP_KEY_DRAW_HIGHLIGHT_MAPPED,true);
			dp.changeProperty(PROP_KEY_DRAW_HIGHLIGHT_WITH_HALO,true);
			dp.changeProperty(PROP_KEY_DRAW_HIGHLIGHT_MONOCHROMATIC,false);
						 */
                        renderOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_HIGHLIGHT_MAPPED, true);
                        renderOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_HIGHLIGHT_WITH_HALO, true);
                        renderOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_HIGHLIGHT_MONOCHROMATIC, false);

//						dp=dp.withSpecialColor();
                    }
                }else if(rgroupColor==2){ //katzelda TODO DOES THIS EVER HAPPEN? rgroupColor hardcoded to 1
                    if(!compColor && mapChemicalRgroup(c)){
//						dp=dp.withSpecialColorMON();
						/*
						dp.changeProperty(PROP_KEY_DRAW_HIGHLIGHT_MAPPED,true);
			dp.changeProperty(PROP_KEY_DRAW_HIGHLIGHT_WITH_HALO,true);
			dp.changeProperty(PROP_KEY_DRAW_HIGHLIGHT_MONOCHROMATIC,true);
						 */
                        renderOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_HIGHLIGHT_MAPPED, true);
                        renderOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_HIGHLIGHT_WITH_HALO, true);
                        renderOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_HIGHLIGHT_MONOCHROMATIC, true);

                    }
                }
            }

        }
    }




    public static boolean hasRGroups(Chemical c){
//		return c.atoms()
//				.filter(a-> a.getRGroupIndex().isPresent()).findAny().isPresent();

//		else{
//			//r=true;
//			if(ca.getAlias().startsWith("_")){
//				ca.setRgroupIndex(Integer.parseInt(ca.getAlias().replace("_R", "")));
//				ca.setAlias(ca.getAlias().replace("_",""));
//				r= true;
//			}
//		}
        boolean r=false;
        for(Atom ca:c.getAtoms()){
            if(ca.getRGroupIndex().isPresent()){
                r= true;
            }
            else{
                //r=true;
                String alias = ca.getAlias().orElse("");
                if(alias.startsWith("_R")){
                    ca.setRGroup(Integer.parseInt(alias.replace("_R", "")));
                    r= true;
                }
            }
        }
        return r;
    }
    public static boolean colorChemicalComponents(Chemical c){


        long numberOfConnectedComponents =c.connectedComponentsAsStream().count();
        if(numberOfConnectedComponents <2){
            return false;
        }

        c.setAtomMapToPosition();
        int i = 2;
        int con=80;

        Iterator<Chemical> components = c.connectedComponents();
        while(components.hasNext()){
            for(Atom a : components.next().getAtoms()){
                a.setAtomToAtomMap( i+con);
            }
            i--;
        }

        return true;
    }
    public static boolean mapChemicalRgroup(Chemical c){
        AtomicBoolean change=new AtomicBoolean(false);
        for(Atom ca:c.getAtoms()){
            ca.getRGroupIndex().ifPresent( rindex ->{
                ca.setAtomToAtomMap(rindex);
                change.set(true);
            });
        }
        return change.get();
    }
    public static boolean fuseChemical(Chemical c){
        Map<Integer,Atom> needLink = new HashMap<>();
        Set<Atom> toRemove=new HashSet<>();


        for(Atom ca:c.getAtoms()){

            ca.getRGroupIndex().ifPresent(rindex->{
                Atom newNeighbor = needLink.get(rindex);
                if(newNeighbor==null){
                    needLink.put(rindex, newNeighbor);
                }else{
                    needLink.remove(rindex);
                    for(Atom ca2 : ca.getNeighbors()){
                        for(Atom ca3: newNeighbor.getNeighbors()){
                            c.addBond(ca2, ca3, Bond.BondType.SINGLE);
                        }
                    }
                    toRemove.add(ca);
                    toRemove.add(newNeighbor);
                }
            });
        }

        toRemove.forEach(ca -> c.removeAtom(ca));
        return !toRemove.isEmpty();
    }


    private static Chemical parseAndComputeCoordsIfNeeded(String input) throws IOException{
        Chemical c = Chemical.parse(input);
        if(!c.getSource().get().getType().includesCoordinates()){
            try {
                c.generateCoordinates();
            } catch (MolwitchException e) {
                throw new IOException("error generating coordinates",e);
            }
        }
        return c;
    }
    private byte[] render (Structure struc, String format, int size, int[] amap, Boolean stereo)
            throws Exception {
        Map<String, Boolean> newDisplay = new HashMap<>();
        newDisplay.put(RendererOptions.DrawOptions.DRAW_STEREO_LABELS_AS_RELATIVE.name(),
                Structure.Stereo.RACEMIC.equals(struc.stereoChemistry));
        Chemical c= parseAndComputeCoordsIfNeeded(fixMolIfNeeded(struc));

        if(!Structure.Optical.UNSPECIFIED.equals(struc.opticalActivity)
                && struc.opticalActivity!=null){
            if(struc.definedStereo>0){
                if(Structure.Optical.PLUS_MINUS.equals(struc.opticalActivity)){
                    if(Structure.Stereo.EPIMERIC.equals(struc.stereoChemistry)
                            || Structure.Stereo.RACEMIC.equals(struc.stereoChemistry)
                            || Structure.Stereo.MIXED.equals(struc.stereoChemistry)){
                        c.setProperty("BOTTOM_TEXT","relative stereochemistry");
                    }
                }
            }
            if(struc.opticalActivity== Structure.Optical.PLUS){
                c.setProperty("BOTTOM_TEXT","optical activity: (+)");
                if(Structure.Stereo.UNKNOWN.equals(struc.stereoChemistry)){
                    newDisplay.put(RendererOptions.DrawOptions.DRAW_STEREO_LABELS_AS_STARRED.name(), true);
                }
            } else if(struc.opticalActivity== Structure.Optical.MINUS) {
                c.setProperty("BOTTOM_TEXT","optical activity: (-)");
                if(Structure.Stereo.UNKNOWN.equals(struc.stereoChemistry)){
                    newDisplay.put(RendererOptions.DrawOptions.DRAW_STEREO_LABELS_AS_STARRED.name(), true);
                }
            }
        }

        if(size>250){
            if(!Structure.Stereo.ACHIRAL.equals(struc.stereoChemistry))
                newDisplay.put(RendererOptions.DrawOptions.DRAW_STEREO_LABELS.name(), true);
        }
        if(newDisplay.size()==0)newDisplay=null;


        return renderChemical (c, format, size, amap,newDisplay,stereo);
    }

}
