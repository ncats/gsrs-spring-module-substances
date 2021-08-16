/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package example.substance.codeprocessor;

import gsrs.module.substance.datasource.CodeSystemMeta;
//import gsrs.module.substance.datasource.;
import gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator;
import gsrs.module.substance.processors.CodeProcessor;
import gsrs.module.substance.processors.CodeSystemUrlGenerator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author mitch
 */
@Slf4j
public class CodeProcessorTest {

    public CodeProcessorTest() {
    }

    @Test
    public void CodeProcessorConstructorTest() throws IOException {

        Map<String, String> innerFileMap = new HashMap<>();
        innerFileMap.put("file", "codeSystem.json");
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("class", "gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator");
        configMap.put("json", innerFileMap);
        CodeProcessor processor = new CodeProcessor(configMap);
        CodeSystemMeta meta = null;
        CodeSystemUrlGenerator generator = processor.codeSystemData;
        assertNotNull(generator);
    }
    
       /* @Test
    public void CodeProcessorResolutionTest() throws IOException {

        Map<String, String> innerFileMap = new HashMap<>();
        innerFileMap.put("file", "codeSystem.json");
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("class", "gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator");
        configMap.put("json", innerFileMap);
        CodeProcessor processor = new CodeProcessor(configMap);
        CodeSystemMeta meta = null;
        CodeSystemUrlGenerator generator = processor.codeSystemData;

        Field mapField = generator.getClass().getDeclaredField("");
        mapField.setAccessible(true);
        Map<String, CodeSystemMeta> value = (Map) mapField.get(generator);
        meta = value.get(codeSystemForTest.toLowerCase());

    }*/

}
