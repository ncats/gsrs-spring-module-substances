package gsrs.module.substance.controllers;

import gsrs.cache.GsrsCache;
import gsrs.controller.GetGsrsRestApiMapping;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.repository.ChemicalSubstanceRepository;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.models.Structure;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Substance;
import ix.utils.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.JsonNode;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Some heavily used API calls in GSRS 2.x did not get moved over to a `api/v1` route
 * so during the transition from GSRS 2 to GSRS 3 they have to be supported.
 *
 * All these legacy routes will redirect to their api/v1 counterparts.
 */
@RestController
public class LegacyGinasAppController {

    @Autowired
    private SubstanceEntityService substanceService;
    
    
    @Autowired
    private EntityLinks entityLinks;

    @Autowired
    private ChemicalSubstanceRepository chemicalSubstanceRepository;
    @Autowired
    private SubstanceRepository substanceRepository;
    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private GsrsCache ixCache;
    
    //POST        /register/duplicateCheck          ix.ginas.controllers.GinasFactory.validateChemicalDuplicates
    @PostMapping({"register/duplicateCheck", "/ginas/app/register/duplicateCheck"})
    public ValidationResponse<Substance> duplicateCheck(@RequestBody JsonNode updatedEntityJson) throws Exception {
        return substanceService.validateEntity(updatedEntityJson, ValidatorCategory.CATEGORY_DEFINITION());
    }

    //GET         /export/$id<[a-f0-9\-]+>.$format<(mol|sdf|smi|smiles|fas)>
    // ix.ginas.controllers.GinasApp.structureExport(id: String, format: String, context: String ?= null)
    @GetMapping({"export/{id:[a-f0-9\\-]+}.{format}","/ginas/app/export/{id:[a-f0-9\\-]+}.{format}"})
    public Object exportStructure(@PathVariable String id, @PathVariable String format,
                              @RequestParam(value = "size", required = false, defaultValue = "150") int size,
                              @RequestParam("context") Optional<String> context,
                              @RequestParam("version") Optional<String> version,
                              @RequestParam(value = "stereo", required = false, defaultValue = "") Boolean stereo,
                              HttpServletRequest httpRequest, RedirectAttributes attributes,
                                  @RequestParam Map<String, String> queryParameters){
        if("mol".equalsIgnoreCase(format) || "sdf".equalsIgnoreCase(format) ||
                "smi".equalsIgnoreCase(format) ||  "smiles".equalsIgnoreCase(format) ) {
            //this is a copy and paste of SubstanceController#render but without caring about the
            //parent substance since we don't need to set context ?
            if (UUIDUtil.isUUID(id)) {
                Substance actualSubstance = null;

                //katzelda June 2021: first check if temp structure the UI uses this to render everything
                //including temp structure searches
                Optional<Structure> structure = GsrsSubstanceControllerUtil.getTempObject(ixCache, id, Structure.class);
                if(!structure.isPresent()) {


                    UUID uuid = UUID.fromString(id);
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
                    return gsrsControllerConfiguration.handleNotFound(queryParameters);
                }
                //keep legacy results sdf returns the mol !?
                if ("mol".equalsIgnoreCase(format) || "sdf".equalsIgnoreCase(format)) {
                    return structure.get().molfile;
                } else {
                    return structure.get().smiles;
                }
            }
        }else{
            //TODO add support for fasta ? do we need to ?
        }
            return gsrsControllerConfiguration.handleBadRequest(400, "unknown id " + id, queryParameters);
    }

    //ginas/app/img/$id<[a-f0-9\-]+>.$format<(svg|png|mol|sdf|smi|smiles)>
    //id: String, format: String, size: Int ?= 150, context: String ?= null, version: String ?= null
    @GetMapping({"img/{id:[a-f0-9\\-]+}.{format}","/ginas/app/img/{id:[a-f0-9\\-]+}.{format}"})
    public Object renderImage(@PathVariable String id, @PathVariable String format,
                              @RequestParam(value = "size", required = false, defaultValue = "150") int size,
                              @RequestParam("context") Optional<String> context,
                              @RequestParam("version") Optional<String> version,
                              @RequestParam(value = "stereo", required = false, defaultValue = "") Boolean stereo,
                              HttpServletRequest httpRequest, RedirectAttributes attributes,
                              @RequestParam Map<String, String> queryParameters){

        //Beta UI calls this method img/$id.mol  !? redirect to other method
        if("mol".equalsIgnoreCase(format) || "sdf".equalsIgnoreCase(format) ||
                "smi".equalsIgnoreCase(format) ||  "smiles".equalsIgnoreCase(format) ) {
            return exportStructure(id, format, size, context, version, stereo, httpRequest, attributes, queryParameters);
        }
        httpRequest.setAttribute(
                View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.MOVED_PERMANENTLY);

        attributes.addAttribute("format", format);
        attributes.addAttribute("size", size);
        if(context.isPresent()) {
            attributes.addAttribute("context", context.get());
        }
        if(version.isPresent()) {
            attributes.addAttribute("version", version.get());
        }
        if(stereo !=null){
            attributes.addAttribute("stereo", stereo);
        }
        return new ModelAndView("redirect:/api/v1/substances/render(" +id +")");
    }

    @GetGsrsRestApiMapping("/suggest/@fields")
    public Object suggestFields(HttpServletRequest httpRequest, RedirectAttributes attributes) throws IOException {
        httpRequest.setAttribute(
                View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.MOVED_PERMANENTLY);
        return new ModelAndView("redirect:/api/v1/substances/suggest/@fields");
    }
    @GetGsrsRestApiMapping("/suggest")
    public Object suggest(@RequestParam(value ="q") String q, @RequestParam(value ="max", defaultValue = "10") int max,
                          HttpServletRequest httpRequest, RedirectAttributes attributes) throws IOException {
        httpRequest.setAttribute(
                View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.MOVED_PERMANENTLY);
        attributes.addAttribute("q", q);
        attributes.addAttribute("max", max);
        return new ModelAndView("redirect:/api/v1/substances/suggest");
    }
    @GetGsrsRestApiMapping("/suggest/{field}")
    public Object suggestField(@PathVariable("field") String field,  @RequestParam("q") String q, @RequestParam(value ="max", defaultValue = "10") int max,
        HttpServletRequest httpRequest, RedirectAttributes attributes) throws IOException {
            httpRequest.setAttribute(
                    View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.MOVED_PERMANENTLY);
            attributes.addAttribute("q", q);
            attributes.addAttribute("max", max);
            return new ModelAndView("redirect:/api/v1/substances/suggest/" + field);
    }
}
