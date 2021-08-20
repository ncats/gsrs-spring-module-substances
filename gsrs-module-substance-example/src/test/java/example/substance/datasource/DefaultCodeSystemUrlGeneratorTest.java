package example.substance.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.datasource.CodeSystemMeta;
import gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator;
import ix.ginas.models.v1.Code;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author mitch
 */
@Slf4j
public class DefaultCodeSystemUrlGeneratorTest {

    public DefaultCodeSystemUrlGeneratorTest() {
    }

    @Test
    public void DefaultCodeSystemUrlGeneratorConstructorTest() throws IOException {
        String codeSystemForTest = "PubChem";
        String codeUrlForTest = "https://pubchem.ncbi.nlm.nih.gov/compound/$CODE$";

        Map<String, String> codeSystemUrl = new HashMap<>();
        codeSystemUrl.put(codeSystemForTest, codeUrlForTest);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("codeSystems", codeSystemUrl);
        DefaultCodeSystemUrlGenerator generator = new DefaultCodeSystemUrlGenerator(configMap);
        CodeSystemMeta meta = null;
        try {
            Field mapField = generator.getClass().getDeclaredField("map");
            mapField.setAccessible(true);
            Map<String, CodeSystemMeta> value = (Map) mapField.get(generator);
            meta = value.get(codeSystemForTest.toLowerCase());

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            log.error("Error testing code system url generator", ex);
        }

        assertNotNull(meta);
    }

    @Test
    public void DefaultCodeSystemUrlGeneratorGenerationTest() throws IOException {
        String codeSystemForTest = "PubChem";
        String codeUrlForTest = "https://pubchem.ncbi.nlm.nih.gov/compound/$CODE$";
        String codeValueForTest = "5288826";
        String expectedUrl = "https://pubchem.ncbi.nlm.nih.gov/compound/5288826";

        Map<String, String> codeSystemUrl = new HashMap<>();
        codeSystemUrl.put(codeSystemForTest, codeUrlForTest);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("codeSystems", codeSystemUrl);
        DefaultCodeSystemUrlGenerator generator = new DefaultCodeSystemUrlGenerator(configMap);
        Code code = new Code();
        code.code = codeValueForTest;
        code.codeSystem = codeSystemForTest;

        Optional<String> actualUrl = generator.generateUrlFor(code);
        Assert.assertEquals(expectedUrl, actualUrl.get());

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File("d:\\temp\\test1.json"), generator);
        System.out.println("serialized: ");
    }

}
