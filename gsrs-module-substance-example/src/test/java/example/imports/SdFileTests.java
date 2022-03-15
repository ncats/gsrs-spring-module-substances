package example.imports;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.module.substance.importers.SDFImportAdaptorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class SdFileTests {

    @Test
    public void testSdfInstructions1() {
        SDFImportAdaptorFactory importAdaptorFactory = new SDFImportAdaptorFactory();

        List<String> fieldNames = Arrays.asList("CAS", "select_name", "alpha code");
        JsonNode importInfo = importAdaptorFactory.createDefaultSdfFileImport(fieldNames);
        String json =importInfo.toPrettyString();
        System.out.println(json);
        Assertions.assertTrue(json.length()>0);
    }
}
