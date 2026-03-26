package gsrs.module.substance.misc.emasmsfhir;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.SubstanceEntityService;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class EmaSmsFhirController {
    private static String DEFAULT_NAME_SOURCE = "FDA SUBSTANCE REGISTRATION SYSTEM";
    private static String DEFAULT_LANGUAGE = "en";
    private static String SodiumChlorideUuid = "306d24b9-a6b8-4091-8024-02f9ec24b705";
    // http://localhost:8080/api/v1/substances/306d24b9-a6b8-4091-8024-02f9ec24b705/@emaSmsRecord

    private static String sodiumGlutonateUuid = "90e9191d-1a81-4a53-b7ee-560bf9e68109";
    // http://localhost:8080/api/v1/substances/90e9191d-1a81-4a53-b7ee-560bf9e68109/@emaSmsRecord

    private static Map<String, String> FIELD_MAP;

    static {
        FIELD_MAP = new HashMap<>();
        FIELD_MAP.put("smsId", "SMS_ID");
        FIELD_MAP.put("substanceName", "Substance_Name");
        FIELD_MAP.put("isPreferredName", "Is_Preferred_Name");
        FIELD_MAP.put("language", "Language");
        FIELD_MAP.put("substanceType", "Substance_Type");
        FIELD_MAP.put("evCode", "EV_Code");
        FIELD_MAP.put("unii", "UNII");
        FIELD_MAP.put("innNumber", "INN_Number");
        FIELD_MAP.put("ecListNumber", "EC_List/Number");
    }


    ObjectMapper mapper = new ObjectMapper();

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




}

