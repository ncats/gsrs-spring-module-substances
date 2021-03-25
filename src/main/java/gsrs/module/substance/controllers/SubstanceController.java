package gsrs.module.substance.controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.controller.*;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.repository.EditRepository;
import ix.core.chem.ChemCleaner;
import ix.core.chem.PolymerDecode;
import ix.core.chem.StructureProcessor;
import ix.core.controllers.EntityFactory;
import ix.core.models.Structure;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.stream.Stream;

/**
 * GSRS Rest API controller for the {@link Substance} entity.
 */
@Slf4j
@ExposesResourceFor(Substance.class)
@GsrsRestApiController(context = SubstanceEntityService.CONTEXT,  idHelper = IdHelpers.UUID)
public class SubstanceController extends EtagLegacySearchEntityController<SubstanceController, Substance, Long> {


    @Autowired
    private EditRepository editRepository;
    @Autowired
    private SubstanceLegacySearchService legacySearchService;

    @Autowired
    private StructureProcessor structureProcessor;

    @Override
    protected LegacyGsrsSearchService<Substance> getlegacyGsrsSearchService() {
        return legacySearchService;
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
    @PostGsrsRestApiMapping("/interpretStructure")
    public ResponseEntity<Object> interpretStructure(@NotBlank @RequestBody String mol, @RequestParam Map<String, String> queryParameters){
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
