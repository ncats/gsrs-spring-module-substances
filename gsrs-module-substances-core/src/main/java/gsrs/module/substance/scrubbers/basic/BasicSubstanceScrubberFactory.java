package gsrs.module.substance.scrubbers.basic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
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

@Slf4j
public class BasicSubstanceScrubberFactory implements RecordScrubberFactory<Substance> {
    private final static String JSONSchema = getSchemaString();

    private static CachedSupplier<JsonNode> schemaSupplier = CachedSupplier.of(()->{
        ObjectMapper mapper =new ObjectMapper();
        try {
            return mapper.readTree(JSONSchema);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;//todo: alternate return?
    });

    @Override
    public RecordScrubber<Substance> createScrubber(JsonNode settings) {
        log.trace("in BasicSubstanceScrubberFactory.createScrubber");
        BasicSubstanceScrubberParameters settingsObject = (new ObjectMapper()).convertValue(settings, BasicSubstanceScrubberParameters.class);
        log.trace(" settingsObject: {}", (settingsObject==null ? "null" : "not null"));
        //hack for demo 28 September 2022
        //todo: make sure real settings get passe!
        if(settingsObject==null){
            settingsObject = new BasicSubstanceScrubberParameters();
            settingsObject.setRemoveNotes(true);
            settingsObject.setRemoveChangeReason(false);
            settingsObject.setRemoveDates(true);
            settingsObject.setRemoveCodesBySystem(true);
            settingsObject.setCodeSystemsToKeep(Arrays.asList( "CAS","Wikipedia"));
            settingsObject.setCodeSystemsToRemove(Arrays.asList("BDNUM"));
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
        ClassPathResource fileResource = new ClassPathResource("schemas/scrubberSchema.json");
        byte[] binaryData = FileCopyUtils.copyToByteArray(fileResource.getInputStream());
        String schemaString =new String(binaryData, StandardCharsets.UTF_8);
        return schemaString;
    }
}
