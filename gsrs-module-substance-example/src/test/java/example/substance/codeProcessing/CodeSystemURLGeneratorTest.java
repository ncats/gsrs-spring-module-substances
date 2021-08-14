package example.substance.codeProcessing;

import gsrs.module.substance.datasource.CodeSystemMeta;
import gsrs.module.substance.datasource.CodeSystemURLGenerator;
import ix.ginas.models.v1.Code;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author mitch
 */
@Slf4j
public class CodeSystemURLGeneratorTest {
    
    public CodeSystemURLGeneratorTest() {
    }
    

    @Test
    public void testCodeSystemURLGeneratorConstructor() throws IOException {
        log.trace("Starting in testCodeSystemURLGeneratorConstructor");
        String codeSystemForTest="CAS";
        String codeUrlForTest= "https://commonchemistry.cas.org/detail?cas_rn=$CODE$";
        String codeValueForTest="50-00-0";
        String expectedUrl = "https://commonchemistry.cas.org/detail?cas_rn=50-00-0";
        CodeSystemMeta meta=null;
        Map<String, String> codeSystemUrl= new HashMap<>();
        codeSystemUrl.put(codeSystemForTest, codeUrlForTest);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("codeSystems", codeSystemUrl);
        CodeSystemURLGenerator generator = new CodeSystemURLGenerator(configMap);
        try {
            Field controlledListField= generator.getClass().getDeclaredField("controlledList");
            controlledListField.setAccessible(true);
            Map<String,CodeSystemMeta> value = (Map)controlledListField.get(generator);
            meta= value.get(codeSystemForTest);
            
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            log.error("Error testing code system url generator", ex);
        }
        
        Code testCode = new Code();
        testCode.codeSystem=codeSystemForTest;
        testCode.code=codeValueForTest;
        
        String codeUrl = meta.generateUrlFor(testCode);
        System.out.println("code URL: " + codeUrl);
        Assertions.assertEquals(expectedUrl, codeUrl);
    }
    
}
