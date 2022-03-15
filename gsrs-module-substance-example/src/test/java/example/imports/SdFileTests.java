package example.imports;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.module.substance.importers.SDFImportAdaptorFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Slf4j
public class SdFileTests {

    @Test
    public void testSdfInstructions1() {
        SDFImportAdaptorFactory importAdaptorFactory = new SDFImportAdaptorFactory();

        List<String> fieldNames = Arrays.asList("CAS", "select_name", "alpha code");
        JsonNode importInfo = importAdaptorFactory.createDefaultSdfFileImport(new HashSet<>(fieldNames));
        String json =importInfo.toPrettyString();
        System.out.println(json);
        Assertions.assertTrue(json.length()>0);
    }

    @Test
    public void predictSettingsTest() throws IOException {
        String fileName= "testSDF/structures.molV2.sdf";
        File dataFile = new ClassPathResource(fileName).getFile();
        log.trace("using dataFile.getAbsoluteFile(): " + dataFile.getAbsoluteFile());

        InputStream fis = new FileInputStream(dataFile.getAbsoluteFile());
        SDFImportAdaptorFactory sDFImportAdaptorFactory = new SDFImportAdaptorFactory();
        AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics settings= sDFImportAdaptorFactory.predictSettings(fis);
        JsonNode adapter =settings.getAdapterSettings();
        log.trace("adapter: ");
        log.trace(adapter.toPrettyString());
        JsonNode schema = settings.getAdapterSchema();
        log.trace("schema: ");
        log.trace(schema.toPrettyString());
        Assertions.assertNotNull(adapter);
        Assertions.assertNotNull(schema);
    }
}
