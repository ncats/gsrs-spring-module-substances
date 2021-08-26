package example.substance.datasource;

import gsrs.module.substance.datasource.CodeSystemMeta;
import gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator;
import ix.ginas.models.v1.Code;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        Map map = generator.getMap();
        
        assertTrue(map.size() >0);
    }

    @Test
    public void DefaultCodeSystemUrlGeneratorConstructorMetaTest() throws IOException {
        String codeSystemForTest = "PubChem";
        String codeUrlForTest = "https://pubchem.ncbi.nlm.nih.gov/compound/$CODE$";

        Map<String, String> codeSystemUrl = new HashMap<>();
        codeSystemUrl.put(codeSystemForTest, codeUrlForTest);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("codeSystems", codeSystemUrl);
        DefaultCodeSystemUrlGenerator generator = new DefaultCodeSystemUrlGenerator(configMap);
        CodeSystemMeta meta = generator.fetch(codeSystemForTest);
        
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
    }

    @Test
    public void DefaultCodeSystemUrlGeneratorGenerationLowercaseTest() throws IOException {
        String codeSystemForTest = "DRUG BANK";
        String codeUrlForTest = "http://www.drugbank.ca/drugs/$CODE$";
        String codeValueForTest = "a5b2c8d8e2f6";
        String expectedUrl = "http://www.drugbank.ca/drugs/a5b2c8d8e2f6";

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
    }


}
