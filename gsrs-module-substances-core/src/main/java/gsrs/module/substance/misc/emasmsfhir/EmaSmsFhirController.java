package gsrs.module.substance.misc.emasmsfhir;

import ca.uhn.fhir.context.FhirContext;
import gsrs.module.substance.SubstanceEntityService;
import ix.ginas.models.v1.Substance;
import org.hl7.fhir.r5.model.SubstanceDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

@RestController
public class EmaSmsFhirController {


    // To do
    // domain, identifier, multiple names

    private static final String SodiumChlorideUuid = "306d24b9-a6b8-4091-8024-02f9ec24b705";
    // http://localhost:8080/api/v1/substances/306d24b9-a6b8-4091-8024-02f9ec24b705/@emaSmsRecord
    // http://localhost:8080/api/v1/substances/306d24b9-a6b8-4091-8024-02f9ec24b705/@emaSmsSubstanceDefinition

    private static final String sodiumGlutonateUuid = "90e9191d-1a81-4a53-b7ee-560bf9e68109";
    // http://localhost:8080/api/v1/substances/90e9191d-1a81-4a53-b7ee-560bf9e68109/@emaSmsRecord
    // http://localhost:8080/api/v1/substances/90e9191d-1a81-4a53-b7ee-560bf9e68109/@emaSmsSubstanceDefinition

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @GetMapping(value = "/api/v1/substances/{id}/@emaSmsRecord")
    public ResponseEntity<?> makeSimpleEmaSmsRecord(@PathVariable("id") String id) {
        String jsonEncoded;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            Optional<Substance> gsrsSubstance = substanceEntityService.flexLookup(id);
            if (!gsrsSubstance.isPresent()) {
                return new ResponseEntity<>("{\"message\": \"Substance entity for FHIR resource not found\"}", headers, HttpStatus.NOT_FOUND);
            }
            FhirContext ctx = FhirContext.forR5();
            EmaSmsSimpleRecordFhirMapper emaSmsSimpleRecordFhirMapper = new EmaSmsSimpleRecordFhirMapper();
            EmaSmsSimpleRecord emaSmsSimpleRecord = emaSmsSimpleRecordFhirMapper.generateEmaSmsSimpleRecordFromSubstance(gsrsSubstance.get());
            jsonEncoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(emaSmsSimpleRecord);
            return new ResponseEntity<>(jsonEncoded, headers, HttpStatus.FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("{\"message\": \"Internal error generating FHIR resource\"}", headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/api/v1/substances/{id}/@emaSmsSubstanceDefinition")
    public ResponseEntity<?> makeEmaSmsSubstanceDefinition(@PathVariable("id") String id) {
        String jsonEncoded;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            Optional<Substance> gsrsSubstance = substanceEntityService.flexLookup(id);
            if (!gsrsSubstance.isPresent()) {
                return new ResponseEntity<>("{\"message\": \"Substance entity for FHIR resource not found\"}", headers, HttpStatus.NOT_FOUND);
            }
            FhirContext ctx = FhirContext.forR5();
            EmaSmsSubstanceDefinitionFhirMapper emaSmsSubstanceDefinitionFhirMapper = new EmaSmsSubstanceDefinitionFhirMapper();
            SubstanceDefinition substanceDefinition = emaSmsSubstanceDefinitionFhirMapper.generateEmaSmsSubstanceDefinitionFromSubstance(gsrsSubstance.get());
            jsonEncoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(substanceDefinition);
            return new ResponseEntity<>(jsonEncoded, headers, HttpStatus.FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("{\"message\": \"Internal error generating FHIR resource\"}", headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

