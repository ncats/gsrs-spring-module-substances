package gsrs.module.substance.misc.emasmsfhir;

import ca.uhn.fhir.context.FhirContext;
import gsrs.module.substance.SubstanceEntityService;
import ix.ginas.models.v1.Substance;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.SubstanceDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@RestController
@Slf4j
@Data
public class EmaSmsFhirController {

    private static final String SodiumChlorideUuid = "306d24b9-a6b8-4091-8024-02f9ec24b705";
    // http://localhost:8080/api/v1/substances/306d24b9-a6b8-4091-8024-02f9ec24b705/@emaSmsSimpleRecord
    // http://localhost:8080/api/v1/substances/306d24b9-a6b8-4091-8024-02f9ec24b705/@emaSmsSubstanceDefinition

    private static final String sodiumGlutonateUuid = "90e9191d-1a81-4a53-b7ee-560bf9e68109";
    // http://localhost:8080/api/v1/substances/90e9191d-1a81-4a53-b7ee-560bf9e68109/@emaSmsSimpleRecord
    // http://localhost:8080/api/v1/substances/90e9191d-1a81-4a53-b7ee-560bf9e68109/@emaSmsSubstanceDefinition

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @Autowired
    private EmaSmsFhirConfiguration emaSmsFhirConfiguration;

    @Autowired
    private EmaSmsSimpleRecordFhirMapper emaSmsSimpleRecordFhirMapper;

    @Autowired
    private EmaSmsSubstanceDefinitionFhirMapper emaSmsSubstanceDefinitionFhirMapper;

    private boolean prettyJson = false;

    @GetMapping(value = "/api/v1/substances/{id}/@emaSmsSimpleRecord")
    // This was made a void method, returning method because HAPI is producing the json and encoding
    public void makeSimpleEmaSmsRecord(
            @PathVariable("id") String id,
            HttpServletResponse response
    ) throws IOException {
        String jsonEncoded;
        try {
            Optional<Substance> gsrsSubstance = substanceEntityService.flexLookup(id);
            if (!gsrsSubstance.isPresent()) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\": \"Substance entity for FHIR resource not found\"}");
                return;
            }
            FhirContext ctx = FhirContext.forR5();
            EmaSmsSimpleRecord emaSmsSimpleRecord = emaSmsSimpleRecordFhirMapper.generateEmaSmsSimpleRecordFromSubstance(gsrsSubstance.get());
            jsonEncoded = ctx.newJsonParser().setPrettyPrint(prettyJson).encodeResourceToString(emaSmsSimpleRecord);
            response.setStatus(HttpStatus.OK.value());
            response.getWriter().write(jsonEncoded);
            return;
        } catch (Exception e) {
            String message = "Internal error generating FHIR resource.";
            log.info("{} {}", message, String.valueOf(e.getCause()));
            log.trace("{} {}", message, Arrays.toString(e.getStackTrace()));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(message);
        }
    }

    @GetMapping(value = "/api/v1/substances/{id}/@emaSmsSubstanceDefinition")
    // This was made a void returning method because HAPI is producing the json and encoding
    public void makeEmaSmsSubstanceDefinition(
            @PathVariable String id,
            HttpServletResponse response
    ) throws IOException {
        String jsonEncoded;
        try {
            Optional<Substance> gsrsSubstance = substanceEntityService.flexLookup(id);
            if (!gsrsSubstance.isPresent()) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"message\": \"Substance entity for FHIR resource not found\"}");
                return;
            }
            FhirContext ctx = FhirContext.forR5();
            SubstanceDefinition substanceDefinition = emaSmsSubstanceDefinitionFhirMapper.generateEmaSmsSubstanceDefinitionFromSubstance(gsrsSubstance.get());
            jsonEncoded = ctx.newJsonParser().setPrettyPrint(prettyJson).encodeResourceToString(substanceDefinition);
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(jsonEncoded);
            return;
        } catch (Exception e) {
            String message = "Internal error generating FHIR resource.";
            log.info("{} {}", message, String.valueOf(e.getCause()));
            log.trace("{} {}", message, Arrays.toString(e.getStackTrace()));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(message);
        }
    }
}
