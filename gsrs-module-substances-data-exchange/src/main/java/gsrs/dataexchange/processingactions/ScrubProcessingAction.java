package gsrs.dataexchange.processingactions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.dataexchange.model.ProcessingAction;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubber;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubberParameters;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class ScrubProcessingAction implements ProcessingAction<Substance> {

    @Override
    public Substance process(Substance stagingAreaRecord, Substance additionalRecord, Map<String, Object> parameters, Consumer<String> logger) throws Exception {
        log.trace("Starting in process");
        ObjectMapper mapper = new ObjectMapper();
        if( !parameters.containsKey("scrubberSettings")) {
            log.warn("no scrubberSettings found!");
            return stagingAreaRecord;
        }
        BasicSubstanceScrubberParameters scrubberParameters= mapper.convertValue(parameters.get("scrubberSettings"), BasicSubstanceScrubberParameters.class);
        log.trace("converted scrubberParameters");
        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(scrubberParameters);
        Optional<Substance> scrubbed =scrubber.scrub(stagingAreaRecord);
        log.trace("completed call to scrub");
        return scrubbed.orElse(null);
    }

    @Override
    public String getActionName() {
        return "Scrub";
    }

    @Override
    public List<String> getOptions() {
        return Arrays.asList("RegenerateUUIDs", "RemoveApprovalId", "CreateCodeForApprovalId", "ApprovalIdCodeSystem", "ReplacementAuditUser");
    }


    private final static String JSONSchema = getSchemaString();

    private final static CachedSupplier<JsonNode> schemaSupplier = CachedSupplier.of(()->{
        ObjectMapper mapper =new ObjectMapper();
        try {
            JsonNode schemaNode=mapper.readTree(JSONSchema);
            return schemaNode;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;//todo: alternate return?
    });

    @SneakyThrows
    private static String getSchemaString() {
        log.trace("starting getSchemaString");
        ClassPathResource fileResource = new ClassPathResource("schemas/importScrubberSchema.json");
        byte[] binaryData = FileCopyUtils.copyToByteArray(fileResource.getInputStream());
        String schemaString =new String(binaryData, StandardCharsets.UTF_8);
        //log.trace("read schema:{}", schemaString);
        return schemaString;
    }

    @Override
    public JsonNode getAvailableSettingsSchema(){
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(getSchemaString());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
