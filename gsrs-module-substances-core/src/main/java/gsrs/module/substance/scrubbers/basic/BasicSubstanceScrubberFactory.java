package gsrs.module.substance.scrubbers.basic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.exporters.NoOpRecordScrubberFactory;
import ix.ginas.exporters.RecordScrubber;
import ix.ginas.exporters.RecordScrubberFactory;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class BasicSubstanceScrubberFactory implements RecordScrubberFactory<Substance> {
    private final static String JSONSchema = getSchemaString();

    private static CachedSupplier<JsonNode> schemaSupplier = CachedSupplier.of(()->{
        ObjectMapper mapper =new ObjectMapper();
        try {
            JsonNode schemaNode=mapper.readTree(JSONSchema);
            return schemaNode;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;//todo: alternate return?
    });

    @Override
    public RecordScrubber<Substance> createScrubber(JsonNode settings) {
        log.trace("in BasicSubstanceScrubberFactory.createScrubber");
        BasicSubstanceScrubberParameters settingsObject = (new ObjectMapper()).convertValue(settings, BasicSubstanceScrubberParameters.class);
        log.trace(" settingsObject: {}", (settingsObject==null || settings.size()==0 ? "null" : "not null"));

        if(settingsObject==null){
            log.warn("no settings supplied to createScrubber");
            RecordScrubber<Substance> identityScrubber = (t)-> Optional.of(t);
            return identityScrubber;
        }

        BasicSubstanceScrubber scrubber = new BasicSubstanceScrubber(settingsObject);
        scrubber= AutowireHelper.getInstance().autowireAndProxy(scrubber);

        return scrubber;
    }

    @Override
    public JsonNode getSettingsSchema() {
        return schemaSupplier.get();
    }

    private  static String readFileAsString(File textFile)throws Exception
    {
        String data = "";
        data = new String(Files.readAllBytes(textFile.toPath()));
        return data;
    }

    @SneakyThrows
    private static String getSchemaString() {
        log.trace("starting getSchemaString");
        ClassPathResource fileResource = new ClassPathResource("schemas/scrubberSchema.json");
        byte[] binaryData = FileCopyUtils.copyToByteArray(fileResource.getInputStream());
        String schemaString =new String(binaryData, StandardCharsets.UTF_8);
        //log.trace("read schema:{}", schemaString);
        return schemaString;
    }
}
