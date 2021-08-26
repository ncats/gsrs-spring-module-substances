package example.substance.processor;

import gsrs.module.substance.datasource.CodeSystemMeta;
import gsrs.module.substance.processors.CodeProcessor;
import gsrs.module.substance.processors.CodeSystemUrlGenerator;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Code;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.context.ContextConfiguration;

/**
 *
 * @author mitch
 */
@Slf4j
@ContextConfiguration()
public class CodeProcessorTest {

    public CodeProcessorTest() {
    }

    @Test
    public void CodeProcessorConstructorTest() throws IOException {

        Map<String, String> innerFileMap = new HashMap<>();
        innerFileMap.put("filename", "codeSystem.json");
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("class", "gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator");
        configMap.put("json", innerFileMap);
        CodeProcessor processor = new CodeProcessor(configMap);
        CodeSystemMeta meta = null;
        CodeSystemUrlGenerator generator = processor.codeSystemData;
        assertNotNull(generator);
    }

    @Test
    public void CodeProcessorGeneratorTest() throws IOException {

        Map<String, String> innerFileMap = new HashMap<>();
        innerFileMap.put("filename", "codeSystem.json");
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("class", "gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator");
        configMap.put("json", innerFileMap);
        CodeProcessor processor = new CodeProcessor(configMap);
        CodeSystemUrlGenerator generator = processor.codeSystemData;
        assertNotNull(generator);
    }

    @Test
    public void CodeProcessorResolutionTest() throws IOException, EntityProcessor.FailProcessingException {

        Map<String, String> innerFileMap = new HashMap<>();
        innerFileMap.put("filename", "codeSystem.json");
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("class", "gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator");
        configMap.put("json", innerFileMap);
        CodeProcessor processor = new CodeProcessor(configMap);

        Code casCode = new Code();
        casCode.code = "999999";
        casCode.codeSystem = "MERCK INDEX";
        casCode.type = "PRIMARY";

        processor.prePersist(casCode);
        assertFalse(casCode.url.isEmpty());
    }

    @Test
    public void CodeProcessorResolutionNoneTest() throws IOException, EntityProcessor.FailProcessingException {

        Map<String, String> innerFileMap = new HashMap<>();
        innerFileMap.put("filename", "codeSystem.json");
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("class", "gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator");
        configMap.put("json", innerFileMap);
        CodeProcessor processor = new CodeProcessor(configMap);

        Code testCode = new Code();
        testCode.code = "AAAAAAA";
        testCode.codeSystem = "EC";
        testCode.type = "PRIMARY";

        processor.prePersist(testCode);
        assertTrue(testCode.url == null || testCode.url.isEmpty());
    }

    @Test
    public void CodeProcessorTextTest() throws IOException, EntityProcessor.FailProcessingException {

        Map<String, String> innerFileMap = new HashMap<>();
        innerFileMap.put("filename", "codeSystem.json");
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("class", "gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator");
        configMap.put("json", innerFileMap);
        CodeProcessor processor = new CodeProcessor(configMap);

        Code testCode = new Code();
        testCode.code = "999999";
        testCode.codeSystem = "EVMPD";
        testCode.type = "PRIMARY";
        testCode.codeText="lookup";
        String expectedUrl = "https://www.google.com/?q=EVMPD:lookup";

        processor.prePersist(testCode);
        assertEquals(expectedUrl, testCode.url);
    }

}
