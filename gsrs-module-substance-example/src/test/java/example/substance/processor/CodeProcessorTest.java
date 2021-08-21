package example.substance.processor;

import gsrs.module.substance.datasource.CodeSystemMeta;
import gsrs.module.substance.processors.CodeProcessor;
import gsrs.module.substance.processors.CodeSystemUrlGenerator;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Code;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
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

        Code casCode = new Code();
        casCode.code = "AAAAAAA";
        casCode.codeSystem = "EC";
        casCode.type = "PRIMARY";

        processor.prePersist(casCode);
        assertTrue(casCode.url == null || casCode.url.isEmpty());
    }

}
