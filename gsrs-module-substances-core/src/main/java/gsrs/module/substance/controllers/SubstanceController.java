package gsrs.module.substance.controllers;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import gsrs.controller.*;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.utils.FeatureUtils;
import gsrs.module.substance.utils.ChemicalUtils;
import gsrs.service.AbstractGsrsEntityService;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.fda.gsrs.ndsri.FeaturizeNitrosamine;
import gov.fda.gsrs.ndsri.FeaturizeNitrosamine.FeatureResponse;
import gov.nih.ncats.common.io.IOUtil;
import gov.nih.ncats.molwitch.Atom;
import gov.nih.ncats.molwitch.Bond;
import gov.nih.ncats.molwitch.Bond.Stereo;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.MolwitchException;
import gov.nih.ncats.molwitch.io.CtTableCleaner;
import gov.nih.ncats.molwitch.renderer.ChemicalRenderer;
import gov.nih.ncats.molwitch.renderer.RendererOptions;
import gsrs.GsrsFactoryConfiguration;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.RendererOptionsConfig;
import gsrs.module.substance.RendererOptionsConfig.FullRenderOptions;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.approval.ApprovalService;
import gsrs.module.substance.comparators.ExportingSubstanceComparator;
import gsrs.module.substance.expanders.basic.BasicRecordExpanderFactory;
import gsrs.module.substance.hierarchy.SubstanceHierarchyFinder;
import gsrs.module.substance.repository.ChemicalSubstanceRepository;
import gsrs.module.substance.repository.StructuralUnitRepository;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.repository.SubunitRepository;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubberFactory;
import gsrs.module.substance.services.SubstanceSequenceSearchService;
import gsrs.module.substance.services.SubstanceSequenceSearchService.SanitizedSequenceSearchRequest;
import gsrs.module.substance.services.SubstanceStructureSearchService;
import gsrs.module.substance.utils.ImageInfo;
import gsrs.module.substance.utils.ImageUtilities;
import gsrs.module.substance.utils.SubstanceMatchViewGenerator;
import gsrs.repository.EditRepository;
import gsrs.security.hasApproverRole;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import gsrs.services.PrincipalServiceImpl;
import gsrs.springUtils.AutowireHelper;
import gsrs.stagingarea.model.ImportData;
import gsrs.stagingarea.service.StagingAreaService;
import ix.core.EntityFetcher;
import ix.core.chem.Chem;
import ix.core.chem.ChemAligner;
import ix.core.chem.ChemCleaner;
import ix.core.chem.PolymerDecode;
import ix.core.chem.StructureProcessor;
import ix.core.controllers.EntityFactory;
import ix.core.models.Payload;
import ix.core.models.Structure;
import ix.core.models.StructureRenderingParameters;
import ix.core.models.Text;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResultContext;
import ix.core.search.bulk.ResultListRecordGenerator;
import ix.core.search.text.TextIndexer;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.Key;
import ix.ginas.exporters.RecordExpanderFactory;
import ix.ginas.exporters.RecordScrubberFactory;
import ix.ginas.exporters.SpecificExporterSettings;
import ix.ginas.models.v1.AgentModification;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Component;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Material;
import ix.ginas.models.v1.Mixture;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.PolymerClassification;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.SpecifiedSubstanceComponent;
import ix.ginas.models.v1.StructuralModification;
import ix.ginas.models.v1.StructurallyDiverse;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.models.v1.Subunit;
import ix.ginas.models.v1.Unit;
import ix.ginas.utils.JsonSubstanceFactory;
import ix.seqaln.SequenceIndexer;
import ix.utils.CallableUtil.TypedCallable;
import ix.utils.UUIDUtil;
import ix.utils.Util;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * GSRS Rest API controller for the {@link Substance} entity.
 */
@DependsOn("SubstanceSequenceSearchService")
@Slf4j
@ExposesResourceFor(Substance.class)
@GsrsRestApiController(context = SubstanceEntityServiceImpl.CONTEXT,  idHelper = IdHelpers.UUID)
public class SubstanceController extends EtagLegacySearchEntityController<SubstanceController, Substance, UUID> {
	

	
	@Autowired 
	private SubstanceMatchViewGenerator matchViewGenerator;

	
	@Autowired
	private ResultListRecordGenerator resultListRecordGenerator;
	
	@Override	
	public ResultListRecordGenerator getResultListRecordGenerator() {
		return resultListRecordGenerator;
	}	
	
    @Autowired
    private PrincipalServiceImpl principalService;

    @Autowired
    private ChemicalUtils chemicalUtils;

    @Override
    public SearchOptions instrumentSearchOptions(SearchOptions so) {

        so= super.instrumentSearchOptions(so);
        so.addDateRangeFacet("root_lastEdited");
        so.addDateRangeFacet("root_approved");
        so.addDateRangeFacet("root_created");
        
        if (gsrsFactoryConfiguration != null) {
            Optional<Map<String, Object>> conf = gsrsFactoryConfiguration
                    .getSearchSettingsFor(SubstanceEntityServiceImpl.CONTEXT);
            
            String restrict = conf
                    .map(cc -> cc.get("restrictDefaultToIdentifiers"))
                    .filter(bb -> bb != null).map(bb -> bb.toString())
                    .orElse(null);
            if (restrict != null && "true".equalsIgnoreCase(restrict)) {
                so.setDefaultField(TextIndexer.FULL_IDENTIFIER_FIELD);
            }
        }
        

        return so;
    }

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
                // In CDK, this doesn't generate coordinates for query molecules.
		// We will need to fix this eventually.
                //

                c.makeHydrogensExplicit();

		// TODO this should probably only happen when needed, but
		// this will fix the CDK issue of 0,0,0 Hs
		ChemAligner.align2DClean(c);
		    
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
                log.warn("issue standardizing mol", e);
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
        public static Optional<StructureStandardizerPresets> value(String s){
            try {
                return Optional.of(StructureStandardizerPresets.valueOf(s.toUpperCase()));
            }catch(Exception e) {
                return Optional.empty();
            }
        }
    }
    @Autowired
    private SubstanceSequenceSearchService substanceSequenceSearchService;
    
    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    @Autowired
    private PlatformTransactionManager transactionManager;


    @Autowired
    private StructureRepository structureRepository;


    @Autowired
    private StructuralUnitRepository structuralUnitRepository;

    @Autowired
    private EditRepository editRepository;
    @Autowired
    private SubstanceLegacySearchService legacySearchService;

    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private PayloadService payloadService;

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

    @Autowired
    private ApprovalService approvalService;

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
        /* as of v. 3.0.3, public/private data selection is handled via the RecordScrubber
        if(publicOnly){
            return stream.filter(s-> s.getAccess().isEmpty());
        }*/
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


    @Override
    protected Optional<Object> handleSpecialFields(EntityUtils.EntityWrapper<Substance> entity, String field) {
        if("@hierarchy".equals(field)){
            return Optional.of(makeJsonTreeForAPI(entity.getValue()));
        }
        
        
        if("@dependencies".equals(field)){
            return Optional.of(makeSubstanceDependencies(entity.getValue()));
        }
        
        
        return null;
    }

   
    public static class SubstanceDependency{
    	public String role;
    	public SubstanceReference substanceRef;
    	public SubstanceDependency(String role, SubstanceReference sref) {
    		this.role=role;
    		this.substanceRef=sref;

    	}
    	public static SubstanceDependency of(String role, SubstanceReference sr) {
    		return new SubstanceDependency(role, sr);

    	}
    }

    private List<SubstanceDependency> makeSubstanceDependencies(Substance sub) {
    	return sub.getDependsOnSubstanceReferencesAndParents()
    			.stream()
    			.map(ss->{
    				String role = "Unknown";
    				Class<?> pcls = ss.k().getClass();

    				if(AgentModification.class.isAssignableFrom(pcls)) {
    					role="Agent Modification";
    				}else if(StructuralModification.class.isAssignableFrom(pcls)) {
    					role="Structural Modification";
    				}else if(Property.class.isAssignableFrom(pcls)) {
                                        Property prop = (Property)ss.k();
    					role="Property:" + prop.getName();
    				}else if(StructurallyDiverse.class.isAssignableFrom(pcls)) {
    					StructurallyDiverse par = (StructurallyDiverse)ss.k();
    					if(par.parentSubstance!=null && par.parentSubstance.uuid.equals(ss.v().uuid)){
    						role="Structurally Diverse Parent";
    					}else if(par.hybridSpeciesMaternalOrganism!=null && par.hybridSpeciesMaternalOrganism.uuid.equals(ss.v().uuid)){
    						role="Structurally Diverse Maternal Parent";
    					}else if(par.hybridSpeciesPaternalOrganism!=null && par.hybridSpeciesPaternalOrganism.uuid.equals(ss.v().uuid)){
    						role="Structurally Diverse Paternal Parent";
    					}else {
    						role="Unknown Structurally Diverse Parent";
    					}
    				}else if(Material.class.isAssignableFrom(pcls)) {
    					role="Monomer/Starting Material";
    				}else if(PolymerClassification.class.isAssignableFrom(pcls)) {
    					role="Polymer Parent";
    				}else if(SpecifiedSubstanceComponent.class.isAssignableFrom(pcls)) {
    					role="Specified Substance Component";
    				}else if(Component.class.isAssignableFrom(pcls)) {
    					role="Mixture Component";
    				}else if(Mixture.class.isAssignableFrom(pcls)) {
    					role="Mixture Parent";
    				}
    				return SubstanceDependency.of(role,  ss.v());
    			}).collect(Collectors.toList());	   
    }
    
    private List<SubstanceHierarchyFinder.TreeNode2> makeJsonTreeForAPI(Substance sub) {
    	
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.setReadOnly(true);
        return transactionTemplate.execute(s->{
            List<SubstanceHierarchyFinder.TreeNode<Substance>> tnlist = substanceHierarchyFinder.getHierarchies(sub);

            SubstanceHierarchyFinder.TreeNode2Builder builder = new SubstanceHierarchyFinder.TreeNode2Builder();
            for (SubstanceHierarchyFinder.TreeNode<Substance> n : tnlist) {
                n.traverseDepthFirst(l -> {
                    SubstanceHierarchyFinder.TreeNode<Substance> fin = l.get(l.size() - 1);
                    String text = ("[" + fin.getValue().getApprovalIDDisplay() + "] "
                            + fin.getValue().getName()
                            + (fin.getType().equals(SubstanceHierarchyFinder.getHierarchyRootType()) ? "" : " {" + fin.getType() + "}")).toUpperCase();

                    builder.addNode(text, fin.getType(), l.size() - 1, fin.getValue().asSubstanceReference());
                    return true;
                });
            }
            List<SubstanceHierarchyFinder.TreeNode2> nodes = builder.build();
            return nodes;
        });
    }

    /**
     * Produce a roughly canonical hash of the specific query parameters so that
     * it can be adequately cached and the results can be returned on subsequent calls.
     * This key is intended to preserve the "fundamental" parts of the query so that queries
     * which mean the same thing will return the same cached result, even if there are superficial
     * differences in how the query is built. This key takes into account:
     * <ul>
     * <li>the query: verbatim text OR a canonical SMILES/SMARTS OR a sequence on search type</li>
     * <li>the search type: substructure, similarity, sequence, etc</li>
     * <li>the similarity cutoff: for structure similarity and sequence searches</li>
     * <li>the order: sorting order given</li>
     * </ul>
     *
     * Note that the sequence searches no longer use this method for their hash keys. Only
     * the structure searches do.
     * @param request
     * @return
     */
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
                }else if(type.toLowerCase().startsWith("fle")) {
                    String sq = getSmiles(request.getParameter("q"));
                    key = "flex/"+Util.sha1(sq + request.getParameter("order"));

                    return Optional.of(signature (key, request));
                }else if(type.toLowerCase().startsWith("exa")) {
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
            qfacets.add("QFACET/"+q);
            query.put("facet", qfacets.toArray(new String[0]));
        }

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

    @PostGsrsRestApiMapping(path = "/structureSearch", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public Object structureSearchPost(
            @RequestParam MultiValueMap<String,String> body,
            HttpServletRequest httpRequest,
            RedirectAttributes attributes) throws Exception {
        log.trace("in structureSearchPost. total records: {}", substanceRepository.count());
        SubstanceStructureSearchService.SearchRequest.SearchRequestBuilder rb = SubstanceStructureSearchService.SearchRequest.builder();

        Optional.ofNullable(body.getFirst("cutoff")).map(s->Double.parseDouble(s)).ifPresent(c->rb.cutoff(c));
        Optional.ofNullable(body.getFirst("q")).ifPresent(c->rb.q(c));
        Optional.ofNullable(body.getFirst("top")).map(s->Integer.parseInt(s)).ifPresent(c->rb.top(c));
        Optional.ofNullable(body.getFirst("skip")).map(s->Integer.parseInt(s)).ifPresent(c->rb.skip(c));
        Optional.ofNullable(body.getFirst("fdim")).map(s->Integer.parseInt(s)).ifPresent(c->rb.fdim(c));
        Optional.ofNullable(body.getFirst("field")).ifPresent(c->rb.field(c));
        Optional.ofNullable(body.getFirst("type")).map(s->SubstanceStructureSearchService.StructureSearchType.parseType(s)).ifPresent(c->rb.type(c));
        Optional.ofNullable(body.getFirst("order")).ifPresent(c->rb.order(c));

        String qText = Optional.ofNullable(body.getFirst("qText")).orElse(null);

        SubstanceStructureSearchService.SanitizedSearchRequest sanitizedRequest = rb.build().sanitize();
        log.trace("sanitizedRequest.getType(): {}", sanitizedRequest.getType());

        boolean isHashQuery = sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.EXACT
                || sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.FLEX
                || sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.FLEX_PLUS
                || sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.EXACT_PLUS;
        Optional<Structure> structure = parseStructureQuery(sanitizedRequest.getQueryStructure(), !isHashQuery);
        if(!structure.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(body.toSingleValueMap(), "query structure not found : " + sanitizedRequest.getQueryStructure());
        }

        boolean sync = Optional.ofNullable(body.getFirst("sync")).map(b->Boolean.parseBoolean(b)).orElse(false);

        attributes.mergeAttributes(sanitizedRequest.getParameterMap());

        if(qText!=null){
        	attributes.addAttribute("qText", qText);
        }
        if(sync) {
        	attributes.addAttribute("sync", true);
        }
        Map<String,String> qmap = attributes.asMap().entrySet().stream().collect(Collectors.toMap(es->es.getKey(), es->es.getValue().toString()));
        log.trace("about to call structureSearchGet using {}", sanitizedRequest.getType().toString());
        return structureSearchGet(
        		sanitizedRequest.getQueryStructure(),
        		sanitizedRequest.getType().getValue(),
        		Optional.ofNullable(sanitizedRequest.getCutoff()).orElse(0.9),
        		sanitizedRequest.getTop(),
        		sanitizedRequest.getSkip(),
        		sanitizedRequest.getFdim(),
        		sanitizedRequest.getField(),
                Optional.ofNullable(attributes.getAttribute("sync")).map(s->(boolean)("true".equalsIgnoreCase(s.toString()))).orElse(false),
        		Optional.ofNullable(attributes.getAttribute("qText")).map(s->s.toString()).orElse(null),
                qmap,
                httpRequest,
                attributes);
    }
    

    @GetGsrsRestApiMapping("/structureSearch")
    public Object structureSearchGet(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "0.9") Double cutoff,
            @RequestParam(required = false) Integer top,
            @RequestParam(required = false) Integer skip,
            @RequestParam(required = false) Integer fdim,
            @RequestParam(required = false) String field,
            @RequestParam(value = "sync", required = false, defaultValue = "false") boolean sync,
            @RequestParam(required = false) String qText,
            @RequestParam Map<String, String> queryParameters,
            HttpServletRequest httpServletRequest,
            RedirectAttributes attributes) throws Exception {

        Optional<String> hashKey = getKeyForCurrentRequest(httpServletRequest);

        Optional<Structure> structureOp = parseStructureQuery(q, true);
        if(!structureOp.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(queryParameters, "query structure not found : " + q);
        }
        Structure structure = structureOp.get();

        String cleaned = CtTableCleaner.clean(structure.molfile);
        log.trace("type: {}", type);
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
        log.trace("in structureSearchGet, sanitizedRequest.type: {}", sanitizedRequest.getType());

        String hash=null;

        //If it's a flex or exact search, it may not have been standardized
        //yet, which means a lot for the hashing algorithm, so standardize
        //and regenerate the hashes for the query.

        //TODO: There should be a way to flag, on the structure, whether this
        //is the case or not. As-is there's some inconsistency on the use of
        //standardizing and generating hashes. We should not generate hashes
        //on non-standardized cases, but it appears we sometimes do.

        if(sanitizedRequest.isHashSearch()) {
            structure = structureProcessor.taskFor(structure.molfile)
                    .standardize(true)
                    .build()
                    .instrument()
                    .getStructure();
        }

        if(sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.EXACT) {
            hash = "root_structure_properties_EXACT_HASH:" + structure.getExactHash();
        } else if( sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.EXACT_PLUS) {
            Structure saltStrippedStructure = stripSalts(structure);
            log.trace("saltStrippedStructure formula: '{}'", saltStrippedStructure.formula);
            hash = saltStrippedStructure.formula == null || saltStrippedStructure.formula.length()==0  ?
                    "root_structure_properties_EXACT_HASH:" + structure.getExactHash()
                    :
                    "root_structure_properties_EXACT_HASH:" + structure.getExactHash() + " OR root_structure_properties_EXACT_HASH:"
                    + saltStrippedStructure.getExactHash();
        } else if(sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.FLEX
            || sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.FLEX_PLUS){
            if( sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.FLEX_PLUS) {
                Structure saltStripped = stripSalts(structure);
                log.trace("running a flex plus search - remove salts. saltStripped formula: '{}'", saltStripped.formula);
                //when we have a valid/non-blank structure after salt stripping, use it to make a full
                // flex search.
                // otherwise, use the original structure to make a simple search for stereo-insensitive hash
                hash = saltStripped.formula != null && saltStripped.formula.length() > 0
                        ?  makeSearch(saltStripped, true)
                        : "root_structure_properties_STEREO_INSENSITIVE_HASH" + ":\"" + structure.getStereoInsensitiveHash() + "\"";
                log.trace("flex plus hash: {}", hash);
            } else {
                hash= makeSearch(structure, true);
            }
            //note we purposefully don't have the lucene path so it finds moieties and polymers etc
            log.trace("search hash: {} for search of type {}", hash, sanitizedRequest.getType());
        }
        log.trace("hash: {}", hash);
        if(hash !=null){
            attributes.mergeAttributes(sanitizedRequest.getParameterMap());

            // Search for the hash and also add the qText (Query parameters).
            if (qText != null) {
                log.trace("we have qText as well");
                hash = hash + " AND (" + qText + ")";
            }

            attributes.addAttribute("q", hash);
            attributes.addAttribute("includeBreakdown", false);
            Optional.ofNullable(httpServletRequest.getParameterValues("facet")).ifPresent(ss->{
                //In the spring RedirectAttributes object, adding String[] arrays
                //directly turns the arrays into a single String comma separated.
                //Instead, we want to set multiple values. The "asMap" method
                //is tied to the real model as a Map and can be directly modified.
                //However, the "put" and "putAll" methods are still overridden to
                //serialize the String[] arrays. However, "putIfAbsent" is not
                //overridden, so the String[] can be stored directly in this way.
                //This works for when attributes is an instance of RedirectAttributesModelMap
                //which it currently is.
                attributes.asMap().putIfAbsent("facet", ss);
            });
            Optional.ofNullable(httpServletRequest.getParameterValues("order")).ifPresent(ss->{
                attributes.asMap().putIfAbsent("order", ss);
            });

            Optional.ofNullable(httpServletRequest.getParameterValues("view")).ifPresent(ss->{
                attributes.asMap().putIfAbsent("view", ss[0]);
            });

            Map<String,String> qmap = attributes.asMap().entrySet().stream().collect(Collectors.toMap(es->es.getKey(), es->es.getValue().toString()));
            //TODO: find a way to make this not be a redirect. If we remove redirect now
            // it will actually get rid of the extra parameters, and result in a null search
            //
            return searchV1(Optional.of(hash),
                            Optional.of(sanitizedRequest.getTop()),
                            Optional.of(sanitizedRequest.getSkip()),
                            Optional.of(sanitizedRequest.getFdim()), httpServletRequest, qmap);
        }
        SearchResultContext resultContext=null;
        if(sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.SUBSTRUCTURE
                || sanitizedRequest.getType() == SubstanceStructureSearchService.StructureSearchType.SIMILARITY) {
            resultContext = substanceStructureSearchService.search(sanitizedRequest, hashKey.get());
        }


        updateSearchContextGenerator(resultContext, queryParameters);

        //TODO move to service
	//TODO: need to add support for qText in the "focused" version of
	// all structure searches. This may require some deeper changes.
        SearchResultContext focused = resultContext.getFocused(sanitizedRequest.getTop(), sanitizedRequest.getSkip(), sanitizedRequest.getFdim(), sanitizedRequest.getField());
        return entityFactoryDetailedSearch(focused, sync);
    }

    private Optional<Structure> parseStructureQuery(String q, boolean store) throws IOException {
        if(UUIDUtil.isUUID(q)){
            Optional<Structure> opt = GsrsSubstanceControllerUtil.getTempObject(gsrscache, q, Structure.class);
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
            gsrscache.setRaw(struc.id.toString(), EntityUtils.EntityWrapper.of(struc).toInternalJson());
        }
        return Optional.of(struc);
    }

    //context: String, q: String ?= null, type: String ?= "GLOBAL", cutoff: Double ?= .9, top: Int ?= 10, skip: Int ?= 0, fdim: Int ?= 10, field: String ?= "", seqType: String ?="Protein")
    @GetGsrsRestApiMapping("/sequenceSearch")
    public ResponseEntity<Object> sequenceSearchGet(
            @RequestParam(required= false) String q,
            @RequestParam(required= false) String type,
            @RequestParam(required= false,  defaultValue = "0.9") Double cutoff,
            @RequestParam(required= false) Integer top,
            @RequestParam(required= false) Integer skip,
            @RequestParam(required= false) Integer fdim,
            @RequestParam(required= false) String field,
            @RequestParam(required= false, defaultValue = "GLOBAL") String searchType,
            @RequestParam(required= false, defaultValue = "Protein") String seqType,
            @RequestParam(value="sync", required= false, defaultValue="false") boolean sync,
            @RequestParam Map<String, String> queryParameters,
            HttpServletRequest httpServletRequest) throws IOException, ExecutionException, InterruptedException {

//        Optional<String> hashKey = getKeyForCurrentRequest(httpServletRequest);
//        //TODO use hashKey to store in gsrscache

        Optional<Subunit> subunit = convertQueryStringToSequence(q, false);
        if(!subunit.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(queryParameters, "query sequence not found : " + q);

        }
        SubstanceSequenceSearchService.SequenceSearchRequest request = SubstanceSequenceSearchService.SequenceSearchRequest.builder()
                .q(subunit.get().sequence)
                .type(SequenceIndexer.CutoffType.valueOfOrDefault(type))
                .seqType(seqType)
//                .searchType(SequenceSearchType.valueOf(searchType))
                .cutoff(cutoff)
                .top(top)
                .skip(skip)
                .fdim(fdim)
                .field(field)
                .build();

        SubstanceSequenceSearchService.SanitizedSequenceSearchRequest sanitizedRequest = request.sanitize();
        SearchResultContext resultContext = substanceSequenceSearchService.search(sanitizedRequest);

        updateSearchContextGenerator(resultContext, queryParameters);

        SearchResultContext focused = resultContext.getFocused(sanitizedRequest.getTop(), sanitizedRequest.getSkip(), sanitizedRequest.getFdim(), sanitizedRequest.getField());
        return entityFactoryDetailedSearch(focused, sync);
    }

    private Optional<Subunit> convertQueryStringToSequence(@RequestParam(required = false) String q, boolean store) {

        if(UUIDUtil.isUUID(q)){
            //query is a uuid of a subunit look it up
            Optional<Subunit> opt = GsrsSubstanceControllerUtil.getTempObject(gsrscache, q, Subunit.class);
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
            gsrscache.setRaw(sub.uuid.toString(), EntityUtils.EntityWrapper.of(sub).toInternalJson());

        }
        return Optional.of(sub);
    }

    @PostGsrsRestApiMapping(path = "/sequenceSearch", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public Object sequenceSearchPost(
            @RequestParam MultiValueMap<String,String> body,
            HttpServletRequest httpRequest,
            RedirectAttributes attributes) throws IOException, ExecutionException, InterruptedException {

        SubstanceSequenceSearchService.SequenceSearchRequest.SequenceSearchRequestBuilder rb = SubstanceSequenceSearchService.SequenceSearchRequest.builder();

        Optional.ofNullable(body.getFirst("cutoff")).map(s->Double.parseDouble(s)).ifPresent(c->rb.cutoff(c));
        Optional.ofNullable(body.getFirst("q")).ifPresent(c->rb.q(c));
        Optional.ofNullable(body.getFirst("top")).map(s->Integer.parseInt(s)).ifPresent(c->rb.top(c));
        Optional.ofNullable(body.getFirst("skip")).map(s->Integer.parseInt(s)).ifPresent(c->rb.skip(c));
        Optional.ofNullable(body.getFirst("fdim")).map(s->Integer.parseInt(s)).ifPresent(c->rb.fdim(c));
        Optional.ofNullable(body.getFirst("field")).ifPresent(c->rb.field(c));
        Optional.ofNullable(body.getFirst("seqType")).ifPresent(c->rb.seqType(c));
//        Optional.ofNullable(body.getFirst("searchType")).map(s->SequenceSearchType.valueOf(s)).ifPresent(c->rb.searchType(c));
        Optional.ofNullable(body.getFirst("order")).ifPresent(c->rb.order(c));



        //TODO: TP: I'm not sure these are actually used, may want to remove all mentions entirely
//        Optional.ofNullable(body.getFirst("identity")).map(s->Double.parseDouble(s)).ifPresent(c->rb.identity(c));
        Optional.ofNullable(body.getFirst("type")).map(s->SequenceIndexer.CutoffType.valueOfOrDefault(s)).ifPresent(c->rb.type(c));


        SubstanceSequenceSearchService.SequenceSearchRequest request = rb.build();

        Optional<Subunit> querySequence= convertQueryStringToSequence(request.getQ(), true);
        if(!querySequence.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(body.toSingleValueMap(), request.getQ());
        }

        boolean sync = Optional.ofNullable(body.getFirst("sync")).map(b->Boolean.parseBoolean(b)).orElse(false);
        SanitizedSequenceSearchRequest rr=request.sanitize();
        attributes.mergeAttributes(rr.toMap());
        attributes.addAttribute("q", querySequence.get().uuid.toString());
        if(sync) {
            attributes.addAttribute("sync", true);
        }
        Map<String,String> qmap = attributes.asMap().entrySet().stream().collect(Collectors.toMap(es->es.getKey(), es->es.getValue().toString()));
        return sequenceSearchGet(
        		attributes.getAttribute("q").toString(),
        		Optional.ofNullable(attributes.getAttribute("type")).map(s->s.toString()).orElse(null),
        		Optional.ofNullable(rr.getCutoff()).orElse(0.8),
                        rr.getTop(),
                        rr.getSkip(),
                        rr.getFdim(),
                        rr.getField(),
                        Optional.ofNullable(attributes.getAttribute("searchType")).map(s->s.toString()).orElse("GLOBAL"),
                        Optional.ofNullable(attributes.getAttribute("seqType")).map(s->s.toString()).orElse("Protein"),
                        Optional.ofNullable(attributes.getAttribute("sync")).map(s->(boolean)("true".equalsIgnoreCase(s.toString()))).orElse(false),
                        qmap,
                        httpRequest);
    }

    @PostGsrsRestApiMapping("/interpretStructure")
    public ResponseEntity<Object> interpretStructure(@NotBlank @RequestBody String mol, @RequestParam Map<String, String> queryParameters){
        String[] standardize = Optional.ofNullable(queryParameters.get("standardize"))
                .orElse("NONE")
                .split(",");
        SimpleStandardizer simpStd=Arrays.stream(standardize)
                .filter(s->!s.equals("NONE"))
                .map(val->val.toUpperCase())
                .map(val->StructureStandardizerPresets.value(val))
                .filter(v->v.isPresent())
                .map(v->v.get())
                .map(std->std.getStandardizer())
                .reduce(SimpleStandardizer::and).orElse(null);

        String mode = Optional.ofNullable(queryParameters.get("mode"))
                .orElse("basic");

        boolean isQuery="query".equalsIgnoreCase(mode);

        boolean appendFeatures = queryParameters.containsKey("appendNNOFeatures") && queryParameters.get("appendNNOFeatures").equalsIgnoreCase("true");

        try {
            String payload = ChemCleaner.getCleanMolfile(mol);
            List<Structure> moieties = new ArrayList<>();
            ObjectMapper mapper = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER();
            ObjectNode node = mapper.createObjectNode();
            try {
                Structure struc = structureProcessor.taskFor(payload)
                        .components(moieties)
                        .standardize(false)
                        .query(isQuery)
                        .build()
                        .instrument()
                        .getStructure();
                // don't standardize!
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
                if( appendFeatures) {
                    log.trace("going to append nitrosamine features");
                    appendFeatureStuff(struc.toChemical(), node);
                }
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

    @PostGsrsRestApiMapping("/interpretFeatures")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> interpretStructureFeatures(@NotBlank @RequestBody String mol, @RequestParam Map<String, String> queryParameters){
        String[] standardize = Optional.ofNullable(queryParameters.get("standardize"))
                .orElse("NONE")
                .split(",");
        SimpleStandardizer simpStd=Arrays.stream(standardize)
                .filter(s->!s.equals("NONE"))
                .map(val->val.toUpperCase())
                .map(val->StructureStandardizerPresets.value(val))
                .filter(v->v.isPresent())
                .map(v->v.get())
                .map(std->std.getStandardizer())
                .reduce(SimpleStandardizer::and).orElse(null);

        String mode = Optional.ofNullable(queryParameters.get("mode"))
                .orElse("basic");

        boolean isQuery="query".equalsIgnoreCase(mode);


        try {
            String payload = ChemCleaner.getCleanMolfile(mol);
            List<Structure> moieties = new ArrayList<>();
            ObjectMapper mapper = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER();
            ObjectNode node = mapper.createObjectNode();
            try {
                Structure struc = structureProcessor.taskFor(payload)
                        .components(moieties)
                        .standardize(false)
                        .query(isQuery)
                        .build()
                        .instrument()
                        .getStructure();
                // don't standardize!
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
                //TODO: fill in calculation
                //saveTempStructure(struc);
                node.put("structure", mapper.valueToTree(struc));
                node.put("moieties", an);
                appendFeatureStuff(struc.toChemical(), node);

            } catch (Exception e) {
                e.printStackTrace();
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
        gsrscache.setRaw(s.id.toString(), EntityFactory.EntityMapper.INTERNAL_ENTITY_MAPPER().toJson(s));

    }

    @GetGsrsRestApiMapping(value={"({id})/@isApprovable", "/{id}/@isApprovable" })
    public ResponseEntity isApprovableGetMethod(@PathVariable("id") String substanceUUIDOrName, @RequestParam Map<String, String> queryParameters) throws Exception {
        Optional<Substance> substance = getEntityService().getEntityBySomeIdentifier(substanceUUIDOrName);

        if(!substance.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(queryParameters);
        }
        boolean approvable = approvalService.isApprovable(substance.get());
        return new ResponseEntity<>(String.valueOf(approvable), HttpStatus.OK);
    }

    
    @GetGsrsRestApiMapping(value={"({id})/@approve", "/{id}/@approve" })
    @hasApproverRole
    public ResponseEntity approveGetMethod(@PathVariable("id") String substanceUUIDOrName, @RequestParam Map<String, String> queryParameters){

        Optional<Substance> substance = getEntityService().getEntityBySomeIdentifier(substanceUUIDOrName);

        if(!substance.isPresent()){
            return getGsrsControllerConfiguration().handleNotFound(queryParameters);
        }
        String[] error = new String[1];
		try {
			GsrsEntityService.UpdateResult<Substance> savedVersion = getEntityService().updateEntity(substance.get(), (s) -> {return approvalTransactions(s,error);});
			if(error[0] !=null){
				return getGsrsControllerConfiguration().handleBadRequest(400, error[0], queryParameters);
			}
			if(savedVersion.getStatus() != GsrsEntityService.UpdateResult.STATUS.UPDATED){
				return getGsrsControllerConfiguration().handleBadRequest(400, "error trying to approve substance " + substanceUUIDOrName, queryParameters);
			}
			//katzelda Nov 24 2021: @approval response in 2.x always returns full json no matter what the view says
			return new ResponseEntity<>(savedVersion.getUpdatedEntity().toFullJsonNode(), HttpStatus.OK);
		} catch (Exception e) {			
			return getGsrsControllerConfiguration().handleBadRequest(400, "error trying to approve substance", queryParameters);
		}        
    }

    
    @Transactional
    private Optional<?> approvalTransactions(Substance substance, String[] error) throws Exception {   	
            try {
                ApprovalService.ApprovalResult result = approvalService.approve(substance);
                return Optional.of(substanceRepository.saveAndFlush(result.getSubstance()));
            } catch (ApprovalService.ApprovalException e) {
                error[0] = "error approving substance " + substance.getBestId() + ": " + e.getMessage();
                return Optional.empty();
            }    	
    }

    @PreAuthorize("hasRole('SuperUpdate')")
    @PutGsrsRestApiMapping("/novalid")
    @Transactional
    public ResponseEntity<Object> updateEntityWithoutValidation(@RequestBody JsonNode updatedEntityJson,
                                               @RequestParam Map<String, String> queryParameters,
                                               Principal principal) throws Exception {
        if( getEntityService().isReadOnly()) {
            log.warn("detected forbidden operation in updateEntityWithoutValidation");
            String message = "Please use the parent object to perform this operation";
            return new ResponseEntity<>(message, this.getGsrsControllerConfiguration().getHttpStatusFor(HttpStatus.BAD_REQUEST, queryParameters));
        }
        log.trace("in updateEntityWithoutValidation");
        AbstractGsrsEntityService.UpdateResult<Substance> result = null;
        log.trace("Will call updateEntityIgnoreValidation");
        result = getEntityService().updateEntity(updatedEntityJson, true);
        if( result != null && result.getStatus()== AbstractGsrsEntityService.UpdateResult.STATUS.NOT_FOUND){
            return this.getGsrsControllerConfiguration().handleNotFound(queryParameters);
        }

        if( result.getStatus() == AbstractGsrsEntityService.UpdateResult.STATUS.UPDATED){
            new ResponseEntity<>(result.getUpdatedEntity(), HttpStatus.OK);
        }
        //match 200 status of old GSRS
        return new ResponseEntity<>(result.getUpdatedEntity(), HttpStatus.OK);
    }

    @Builder
    @Data
    public static class StructureToRender{
        private Key substanceKey;
        private String input;
        private Structure structure;
    }

    private StructureToRender getSubstanceAndStructure(String idOrSmiles, String version){
        Substance actualSubstance=null;
        String input=null;
        boolean history = (version!=null);

        //check cache?
        Optional<Structure> opStructure = (history)?Optional.empty():GsrsSubstanceControllerUtil.getTempObject(gsrscache, idOrSmiles, Structure.class);
        if(!opStructure.isPresent()) {
            UUID uuid = UUID.fromString(idOrSmiles);
            Key skey = Key.of(Substance.class, uuid);

            Optional<Substance> substance = EntityFetcher.of(skey).getIfPossible().map(o->(Substance)o);


            if (!substance.isPresent()) {
                opStructure = structureRepository.findById(uuid);
                if(opStructure.isPresent()) {
                    if (opStructure.get() instanceof GinasChemicalStructure) {
                        ChemicalSubstance cs = chemicalSubstanceRepository.findByStructure_Id(opStructure.get().id);
                        if (cs != null) {
                            actualSubstance = cs;
                        }
                    }
                }else {
                    //TODO: needs to support history, which requires finding the 
                    // parent record
                    Optional<Unit> unit = structuralUnitRepository.findById(uuid);
                    if(unit.isPresent()) {
                        Unit u = unit.get();
                        try {
                            input = CtTableCleaner.clean(u.structure);
                        } catch (Exception e) {
                            input=u.structure;
                        }
                    }else {
                        try {
                            log.trace("looking for structure in import data repo. ");
                            StagingAreaService service;
                            service=getDefaultStagingAreaService();

                            log.trace("retrieved service {}", service.getClass().getName());
                            int versionNum =0;
                            if( version !=null && version.length()>0){
                                try {
                                    versionNum=Integer.parseInt(version);
                                } catch (NumberFormatException ignore){}
                            }
                            log.trace("using versionNum {}",versionNum);

                            ImportData importData = service.getImportDataByInstanceIdOrRecordId(idOrSmiles, versionNum);
                            if( importData !=null){
                                log.trace("retrieved import data");
                                Substance importedSubstance= service.deserializeObject(importData.getEntityClassName(), importData.getData());
                                if(importedSubstance!=null ){
                                    log.trace("retrieved substance from import data repository");
                                    opStructure = importedSubstance.getStructureToRender();
                                    if(!opStructure.isPresent()) {
                                        log.trace("no structure found within SA substance; will create key");
                                        Key k = Key.of(EntityUtils.EntityWrapper.of(importedSubstance));
                                        StructureToRender.StructureToRenderBuilder builder = StructureToRender.builder().substanceKey(k).input(input);
                                        return builder.build();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error retrieving data from import data repository ", e);
                        }
                    }
                }
            } else {
                actualSubstance = substance.get();
                opStructure = actualSubstance.getStructureToRender();
            }
        }
        if(actualSubstance!=null && history) {
            if(!version.equals(actualSubstance.version)) {
                actualSubstance = editRepository.findByKeyAndVersion(actualSubstance.fetchKey(), version)
                        .map(e->e.getOldValueReference().rawJson())
                        .map(js->JsonSubstanceFactory.makeSubstance(js))
                        .get();
                opStructure = actualSubstance.getStructureToRender();
            }
        }

        if (opStructure.isPresent()) {
            input = fixMolIfNeeded(opStructure.get());
        }
        Key k = (actualSubstance!=null)?actualSubstance.fetchKey():null;


        StructureToRender.StructureToRenderBuilder builder = StructureToRender.builder().substanceKey(k).input(input);
        if(opStructure.isPresent()){
            builder.structure(opStructure.get());
        }
        return builder.build();
    }

    @GetGsrsRestApiMapping({"/render({ID})", "/render/{ID}"})
    public Object render(@PathVariable("ID") String idOrSmiles,
                         @RequestParam(value = "format", required = false, defaultValue = "svg") String format,
                         //basic stereo to empty string which spring returns as null Boolean object
                         @RequestParam(value = "version", required = false) String version,
                         @RequestParam(value = "stereo", required = false, defaultValue = "") Boolean stereo,
                         @RequestParam(value = "context", required = false) String contextId,
                         @RequestParam(value = "size", required = false, defaultValue = "150") int size,
                         @RequestParam(value = "minWidth", required = false) Integer minWidth,
                         @RequestParam(value = "maxWidth", required = false) Integer maxWidth,
                         @RequestParam(value = "minHeight", required = false) Integer minHeight,
                         @RequestParam(value = "maxHeight", required = false) Integer maxHeight,
                         @RequestParam(value = "bondLength", required = false) Double bondLength,
                         @RequestParam(value = "standardize", required = false, defaultValue = "") Boolean standardize,
                         @RequestParam Map<String, String> queryParameters) throws Exception {

        int[] amaps = null;
        StructureToRender s2r=null;

        String input=null;
        if (UUIDUtil.isUUID(idOrSmiles)) {
            s2r= gsrscache.getOrElseRawIfDirty("structForRender/" + idOrSmiles + "/" + version, ()->{
                return getSubstanceAndStructure(idOrSmiles,version);
            });

            if(s2r != null && s2r.substanceKey !=null &&
                    !(queryParameters.get("forceDefaultImage") !=null &&queryParameters.get("forceDefaultImage").equalsIgnoreCase("TRUE"))) {
                log.trace("going to call getSpecificImageForSubstance");
                ImageInfo imageInfo = getSpecificImageForSubstance(s2r.substanceKey);
                if (imageInfo.isHasData() && imageInfo.getImageData().length > 0) {
                    String formatToUse = format;
                    if (imageInfo.getFormat() != null && imageInfo.getFormat().trim().length() > 0) {
                        formatToUse = imageInfo.getFormat().trim();
                        log.trace("retrieved mimetype from repo: {}", formatToUse);
                    } else {
                        log.trace("no mimetype from repo");
                    }
                    String formatForResize = formatToUse;
                    byte[] resized =  imageInfo.getImageData();
                    if(!(queryParameters.get("skipResize") !=null &&queryParameters.get("skipResize").equalsIgnoreCase("TRUE"))) {
                        if (formatForResize.indexOf("/") > -1) {
                            formatForResize = formatToUse.split("/")[formatToUse.split("/").length - 1];
                        }
                        if (formatForResize.equalsIgnoreCase("svg+xml")) {
                            formatForResize = "svg";
                        }
                        log.trace("located image with {} bytes; format: {}; will resize to {} and return ResponseEntity",
                                imageInfo.getImageData().length, formatForResize, size);
                        resized = ImageUtilities.resizeImage(imageInfo.getImageData(), size, size, formatForResize);
                    }
                    if( resized==null || resized.length==0){
                        return new ResponseEntity<>("No image found where expected!", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    log.trace("resized size {}", resized.length);
                    HttpHeaders headers = new HttpHeaders();

                    log.trace("going to set content type to {}", parseContentType(formatToUse));
                    headers.set("Content-Type", parseContentType(formatToUse));
                    return new ResponseEntity<>(resized, headers, HttpStatus.OK);
                } else {
                    log.trace("no image found!");
                }
            }
            log.trace("going to return default image");

//            input, null)

            if (s2r.getInput()==null) {
                if(s2r.getSubstanceKey() ==null) {
                    //couldn't find a substance
                    return getGsrsControllerConfiguration().handleNotFound(queryParameters);
                }
                //if we're here, we have a substance but nothing to render return default for substance type
                return getDefaultImageForKey(s2r.getSubstanceKey(), format);
            }
            input = s2r.getInput();
        }else {
            //string must be a smiles (or mol ?)
            input = idOrSmiles;
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
            } else if (s2r.getSubstanceKey() != null) {
                Key k = s2r.getSubstanceKey();
                Map<String,Object> props = gsrscache.getMatchingContextByContextID(contextId, k.toRootKey());
                amaps = (int[]) props.getOrDefault("atomMaps", null);
            }
        }



        String dinput = input;
        int[] damaps = amaps;
        StructureToRender effectivelyFinalS2r = s2r;
        StructureRenderingParameters structureRenderingParameters = new StructureRenderingParameters();
        structureRenderingParameters.setBondLength(bondLength);
        structureRenderingParameters.setMaxHeight(maxHeight);
        structureRenderingParameters.setMinHeight(minHeight);
        structureRenderingParameters.setMaxWidth(maxWidth);
        structureRenderingParameters.setMinWidth(minWidth);
        ByteWrapper bdat = gsrscache.getOrElseRawIfDirty("image/" + Util.sha1(idOrSmiles) + "/" + size + "/" + format +"/" + standardize + "/" + stereo + "/" + contextId + "/" + version + "/" + structureRenderingParameters.toString(),TypedCallable.of(()->{
            byte[] b=renderChemical(effectivelyFinalS2r==null?null: effectivelyFinalS2r.getStructure(), parseAndComputeCoordsIfNeeded(dinput),
                    format, size, damaps, null, stereo, standardize, structureRenderingParameters);

            return ByteWrapper.of(b);
        }, ByteWrapper.class));

        HttpHeaders headers = new HttpHeaders();

        headers.set("Content-Type", parseContentType(format));
        return new ResponseEntity<>(bdat.dat, headers, HttpStatus.OK);

    }
    private static void renderInner(ChemicalRenderer renderer, Chemical chem, int minWidth, int maxWidth, int minHeight, int maxHeight,
                                    double bondLength, String format, ByteArrayOutputStream bos)
            throws IOException {
        log.trace(String.format("in renderInner, minWidth: %d, maxWidth: %d minHeight: %d, maxHeight: %d, bondLength: %f\n",
                minWidth, maxWidth, minHeight, maxHeight, bondLength));
        Rectangle2D.Double rect = renderer.getApproximateBoundsFor(chem, maxWidth, minWidth, maxHeight, minHeight, bondLength);
        int width = (int) Math.round(rect.getWidth());
        int height = (int) Math.round(rect.getHeight());

        if (format.equals("svg")) {
            SVGGraphics2D svg = new SVGGraphics2D
                    (bos, new Dimension(width, height));
            try {
                svg.startExport();
                renderer.render(svg, chem, 0, 0, width, height, false);
                svg.endExport();
            } finally {
                svg.dispose();
            }
        }else {
            BufferedImage bi = renderer.createImage(chem, width, height, false);
            log.trace("rendering using height: " + height + " and width: " + width);
            ImageIO.write(bi, format, bos);
        }
    }
    private static class ByteWrapper{
        byte[] dat;
        public static ByteWrapper of(byte[] dat) {
            ByteWrapper bw= new ByteWrapper();
            bw.dat=dat;
            return bw;
        }
        
    }



    @Deprecated
    private Object getDefaultImageFor(Substance s, String format) throws IOException {
        return getDefaultImageForKey(s.fetchKey(), format);
    }

    private Object getDefaultImageForKey(Key skey, String format) throws IOException {
        String placeholderFile = "polymer.svg";

        if (format != null) {
            format = format.toLowerCase();
        }
        
        if (skey != null) {
            switch (skey.getKind()) {
                case "ix.ginas.models.v1.ChemicalSubstance":
                    placeholderFile = "chemical." + format;
                    break;
                case "ix.ginas.models.v1.ProteinSubstance":
                    placeholderFile = "protein." + format;
                    break;
                case "ix.ginas.models.v1.MixtureSubstance":
                    placeholderFile = "mixture." + format;
                    break;
                case "ix.ginas.models.v1.PolymerSubstance":
                    placeholderFile = "polymer.svg";
                    break;
                case "ix.ginas.models.v1.StructurallyDiverseSubstance":
                    placeholderFile = "structurally-diverse." + format;
                    break;
                case "ix.ginas.models.v1.Substance":
                    placeholderFile = "concept." + format;
                    break;
                case "ix.ginas.models.v1.NucleicAcidSubstance":
                    placeholderFile = "nucleic-acid." + format;
                    break;
                case "ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance":
                    placeholderFile = "g1ss." + format;
                    break;
                default:
                    placeholderFile = "concept." + format;
            }
        } else {
            placeholderFile = "noimage." + format;
        }

        HttpHeaders headers = new HttpHeaders();

        headers.set("Content-Type", parseContentType(placeholderFile.substring(placeholderFile.length()-3)));
        log.trace("set content-type to {} for {}", parseContentType(placeholderFile.substring(placeholderFile.length()-3)),
                "images/\" + placeholderFile");
        try(InputStream in = new ClassPathResource("images/" + placeholderFile).getInputStream()) {
            byte[] bytes = IOUtil.toByteArray(in);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        }

    }

    private ImageInfo getSpecificImageForSubstance(EntityUtils.Key substanceKey){
        Optional<Substance> substance = EntityFetcher.of(substanceKey).getIfPossible().map(o->(Substance)o);
        if(substance.isPresent()) {
            ImageUtilities imageUtilities = new ImageUtilities();
            imageUtilities= AutowireHelper.getInstance().autowireAndProxy(imageUtilities);
            return imageUtilities.getSubstanceImage(substance.get());
        }
        return new ImageInfo(false, null, null);
    }

    private static String parseContentType(String format){
        if("svg".equalsIgnoreCase(format)){
            return "image/svg+xml";
        }
        if("png".equalsIgnoreCase(format)){
            return MediaType.IMAGE_PNG_VALUE;
        }
        if("jpg".equalsIgnoreCase(format)) {
            return MediaType.IMAGE_JPEG_VALUE;
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

    public byte[] renderChemical (Structure struc, Chemical chem, String format,
                                  int size, int[] amap, Map<String, Boolean> newDisplay, Boolean drawStereo, Boolean standardize,
                                  StructureRenderingParameters parameters)
            throws Exception {

        try {
            FullRenderOptions fullRendererOptions = rendererOptionsConfig.getDefaultRendererOptions().copy();

            RendererOptions rendererOptions = fullRendererOptions.getOptions();

            if (struc != null && newDisplay == null) {
                newDisplay = computeDisplayMap(struc, size, chem);
            }
            if (newDisplay != null) {
                rendererOptions.changeSettings(newDisplay);
            }

            //TODO: This would be nice to get back eventually, for standardization:
            //chem.reduceMultiples();

//		boolean highlight=false;
            if (amap != null && amap.length > 0) {
                Atom[] atoms = chem.atoms().toArray(i -> new Atom[i]);
                for (int i = 0; i < Math.min(atoms.length, amap.length); ++i) {
                    atoms[i].setAtomToAtomMap(amap[i]);
                    if (amap[i] != 0) {
                        rendererOptions.withSubstructureHighlight();
                    }
                }
            } else {
                if (chem.atoms().filter(Atom::hasAtomToAtomMap)
                        .findAny().isPresent()) {
                    rendererOptions.withSubstructureHighlight();
                }

            }
            preProcessChemical(chem, rendererOptions);

            if (Chem.isProblem(chem)) {
                rendererOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_STEREO_LABELS, false);

            } else {
                if (size > 250 /*&& !highlight*/) {
                    //katzelda March 21 2019
                    //after talking to Tyler we should just always
                    //turn on stereo labels because the renderer will determine if there's stereo
                    rendererOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_STEREO_LABELS, true);


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
                rendererOptions.setDrawOption(RendererOptions.DrawOptions.DRAW_STEREO_LABELS, drawStereo);

            }
//		if("true".equals(r.getQueryString("stereo"))){
//			rendererOptons.setDrawOption(RendererOptions.DrawOptions.DRAW_STEREO_LABELS, true);
//
//		}else if("false".equals(r.getQueryString("stereo"))){
//			rendererOptons.setDrawOption(RendererOptions.DrawOptions.DRAW_STEREO_LABELS, false);
//
//		}

            rendererOptions.captionTop(c -> c.getProperty("TOP_TEXT"));
            rendererOptions.captionBottom(c -> c.getProperty("BOTTOM_TEXT"));


            Chem.fixMetals(chem);

            if (Boolean.TRUE.equals(standardize)) {
                chem.kekulize();
                chem.makeHydrogensImplicit();
            }
            ChemicalRenderer renderer = new ChemicalRenderer(rendererOptions);

            if (!fullRendererOptions.isShowShadow()) {
                renderer.setShadowVisible(false);
            }
//
//            if (fullRendererOptions.isAddBorder()) {
//            	renderer.setBorderVisible(true);
//            	if(fullRendererOptions.getColorBorder()!=null) {
//            		renderer.setBorderColorARGB(fullRendererOptions.getColorBorder());	
//            	}            	
//            }
//            
//            if (fullRendererOptions.getColorBg()!=null) {
//            	renderer.setBorderColorARGB(fullRendererOptions.getColorBg());            	
//            }
            

            if (fullRendererOptions.isAddBorder()) {
            	renderer.setBorderVisible(true);
            	if(fullRendererOptions.getColorBorder()!=null) {
            		renderer.setBorderColorARGB(fullRendererOptions.getColorBorder());	
            	}            	
            }
            
            if (fullRendererOptions.getColorBg()!=null) {
            	renderer.setBorderColorARGB(fullRendererOptions.getColorBg());            	
            }
            

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            if (parameters != null && parameters.hasValuesForAll()) {
                log.trace("using latest rendering");
                //ChemicalRenderer renderer, Chemical chem, int minWidth, int maxWidth, int minHeight, int maxHeight,
                //                                    double bondLength, String format, ByteArrayOutputStream bos
                renderInner(renderer, chem, parameters.getMinWidth(), parameters.getMaxWidth(), parameters.getMinHeight(), parameters.getMaxHeight(),
                        parameters.getBondLength(), format, bos);
            }else {
                log.trace("using legacy rendering");
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
            } else {
                BufferedImage bi = renderer.createImage(chem, size);
                ImageIO.write(bi, format, bos);
            }
        }
            return bos.toByteArray();
        }catch(Exception e){
            e.printStackTrace();
            throw e;
        }
    }


    private static void preProcessChemical(Chemical c,  RendererOptions renderOptions){
	    
//        log.info("processing chemical");
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
                }else if(alias.startsWith("R")){
                    ca.setRGroup(Integer.parseInt(alias.replace("R", "")));
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
    private byte[] render (Structure struc, String format, int size, int[] amap, Boolean stereo, Boolean structure)
            throws Exception {
        Chemical c= parseAndComputeCoordsIfNeeded(fixMolIfNeeded(struc));

        Map<String, Boolean> newDisplay = computeDisplayMap(struc, size, c);


        return renderChemical (struc, c, format, size, amap,newDisplay,stereo, structure, null);
    }

    private Map<String, Boolean> computeDisplayMap(Structure struc, int size, Chemical c) {
        Map<String, Boolean> newDisplay = new HashMap<>();
        newDisplay.put(RendererOptions.DrawOptions.DRAW_STEREO_LABELS_AS_RELATIVE.name(),
                Structure.Stereo.RACEMIC.equals(struc.stereoChemistry) ||
                Structure.Optical.PLUS_MINUS.equals(struc.stereoChemistry)
                );


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

        if(size >250){
            if(!Structure.Stereo.ACHIRAL.equals(struc.stereoChemistry))
                newDisplay.put(RendererOptions.DrawOptions.DRAW_STEREO_LABELS.name(), true);
        }
        if(newDisplay.size()==0)newDisplay=null;
        return newDisplay;
    }


    @Override
    public RecordScrubberFactory<Substance> getScrubberFactory(){
        return new BasicSubstanceScrubberFactory();
    }

    @Override
    public RecordExpanderFactory<Substance> getExpanderFactory(){
        return new BasicRecordExpanderFactory();
    }


    @Override
    public Comparator<Substance> getComparator() {
        log.trace("returning substance-specific comparator");
        return new ExportingSubstanceComparator();
    }
    public List<Text> getHardcodedConfigsBackup() throws JsonProcessingException {
        List<Text> items = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        SpecificExporterSettings allDataSettings = new SpecificExporterSettings();
        allDataSettings.setExpanderSettings(JsonNodeFactory.instance.objectNode());
        allDataSettings.setScrubberSettings(JsonNodeFactory.instance.objectNode());
        allDataSettings.setExporterSettings(JsonNodeFactory.instance.objectNode());
        allDataSettings.setExporterKey("ALL_DATA");
        allDataSettings.setEntityClass("ix.ginas.models.v1.Substance");
        Text allItems = new Text("settings", mapper.writeValueAsString(allDataSettings));
        allItems.id=0l;
        items.add(allItems);


        SpecificExporterSettings publicDataSettings = new SpecificExporterSettings();
        publicDataSettings.setExpanderSettings(JsonNodeFactory.instance.objectNode());
        ObjectNode scrubberNode=JsonNodeFactory.instance.objectNode();
        scrubberNode.put("removeAllLocked", true);
        scrubberNode.set("removeAllLockedAccessGroupsToInclude", JsonNodeFactory.instance.arrayNode());
        publicDataSettings.setScrubberSettings(scrubberNode);
        publicDataSettings.setExporterSettings(JsonNodeFactory.instance.objectNode());
        publicDataSettings.setExporterKey("PUBLIC_DATA_ONLY");
        publicDataSettings.setEntityClass("ix.ginas.models.v1.Substance");
        Text publicItems = new Text("settings", mapper.writeValueAsString(publicDataSettings));
        publicItems.id=-1l;
        items.add(publicItems);
        return items;
    }

    public String makeSearch(Structure structure, boolean flex) {
        String sins= structure.getStereoInsensitiveHash();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        stringBuilder.append( flex ? "root_structure_properties_STEREO_INSENSITIVE_HASH" : "root_structure_properties_EXACT_HASH");
        stringBuilder.append(":\"");
        stringBuilder.append(sins);
        stringBuilder.append("\" OR ");
        stringBuilder.append(makeFlexSearchMoietyClauses(structure, flex));
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    public String makeFlexSearchMoietyClauses(Structure structure, boolean flex) {

        List<Structure> moieties = new ArrayList<>();
        try {
            structureProcessor.taskFor(structure.molfile)
                    .components(moieties)
                    .standardize(false)
                    .build()
                    .instrument()
                    .getStructure();
            log.trace(" created {} moieties", moieties.size());
            String moietySearchString= moieties.stream()
                    .map(m->{
                        StringBuilder sb = new StringBuilder();
                        sb.append( flex ? "root_moieties_properties_STEREO_INSENSITIVE_HASH:\"" : "root_moieties_properties_EXACT_HASH:\"");
                        sb.append( flex ? m.getStereoInsensitiveHash() : m.getExactHash());
                        sb.append("\"");
                        return sb.toString();
                    })
                    .collect(Collectors.joining(" AND "));
            if( moieties.size() > 1) {
                moietySearchString = "(" + moietySearchString + ")";
            }
            return moietySearchString;
        } catch (Exception e) {
            log.error("Error constructing query: ", e);
            throw new RuntimeException(e);
        }
    }

    private void appendFeatureStuff(Chemical chemical, ObjectNode topLevelNode ) throws Exception {
        log.trace("in appendFeatureStuff");
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> featureList = FeatureUtils.calculateFeatures(chemical);
        ObjectNode allFeatures = mapper.createObjectNode();
        ArrayNode featureArrayNode = mapper.createArrayNode();
        featureList.forEach(features ->{
            ObjectNode oneSet = mapper.createObjectNode();
            features.entrySet().forEach(f-> oneSet.put(f.getKey(), f.getValue()));
            featureArrayNode.add(oneSet);
        });
        allFeatures.put("nitrosamineAnalysisFeatures", featureArrayNode);
        topLevelNode.put("featureList", allFeatures);
    }

    public Structure stripSalts(Structure structure) throws IOException {
        Structure copy = new GinasChemicalStructure();
        copy.molfile = structure.molfile;
        Chemical clean = chemicalUtils.stripSalts(copy.toChemical());
        if( clean == null) {
            return new GinasChemicalStructure();
        }
        Structure saltStripped= structureProcessor.instrument(clean);
        return saltStripped;
    }

    @GetGsrsRestApiMapping("/evaluateSmiles")
    public ResponseEntity<Map<String, Object>> evaluateSmiles(@RequestParam("smiles") String smiles) {
        log.debug("Evaluating SMILES: {}", smiles);
        try {
            // Create a map to store the response
            Map<String, Object> response = new HashMap<>();
            
            // Parse the SMILES string
            Chemical chemical = Chemical.parse(smiles);
            
            // Check for multiple nitrosamine groups
            List<Integer> nitrosamineSites = FeaturizeNitrosamine.markAllNitrosamines(chemical);
            response.put("multipleNitrosamines", nitrosamineSites.size() > 1);
            response.put("nitrosamineCount", nitrosamineSites.size());
            
            // Get the feature response
            Optional<FeatureResponse> featureResponse = FeaturizeNitrosamine.forMostPotentNitrosamine(chemical);
            
            if (featureResponse.isPresent()) {
                FeatureResponse responseData = featureResponse.get();
                
                // Add the response data to the map
                response.put("potencyScore", responseData.getCategoryScore());
                
                // Build structure details
                StringBuilder details = new StringBuilder();
                details.append("Type: ").append(responseData.getType()).append("\n");
                responseData.getFeatureSet().forEach((key, value) -> {
                    details.append(key).append(": ").append(value).append("\n");
                });
                response.put("structureDetails", details.toString());
                
                // Determine which boxes to highlight
                List<String> boxes = new ArrayList<>();
                boxes.add("q1"); // Always add first question
                
                // Add category box based on potency score
                int categoryScore = responseData.getCategoryScore();
                if (categoryScore >= 5) {
                    boxes.add("cat5-1");
                } else if (categoryScore >= 4) {
                    boxes.add("cat4");
                } else if (categoryScore == 3) {
                    boxes.add("cat3");
                } else if (categoryScore == 2) {
                    boxes.add("cat2");
                } else {
                    boxes.add("cat1");
                }
                
                // Add additional boxes based on features
                Optional<String> alphaHydrogens = responseData.getFeature("Alpha-Hydrogens");
                if (alphaHydrogens.isPresent() && !alphaHydrogens.get().equals("0")) {
                    boxes.add("q2");
                }
                
                Optional<String> tertiaryAlpha = responseData.getFeature("Tertiary α-carbon");
                if (tertiaryAlpha.isPresent() && "YES".equals(tertiaryAlpha.get())) {
                    boxes.add("q3");
                    boxes.add("cat5-3");
                }
                // Add score boxes based on potency score
                if (categoryScore >= 4) {
                    boxes.add("score4");
                } else if (categoryScore == 3) {
                    boxes.add("score3");
                } else if (categoryScore == 2) {
                    boxes.add("score2");
                } else {    
                    boxes.add("score1");
                }
                
                response.put("highlightedBoxes", boxes);
                
                log.debug("Successfully evaluated nitrosamine structure with potency score: {}", categoryScore);
                return ResponseEntity.ok(response);
            } else {
                log.warn("No valid nitrosamine structure found for SMILES: {}", smiles);
                response.put("error", "No valid nitrosamine structure found");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error processing SMILES string: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing SMILES string: " + e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
}
