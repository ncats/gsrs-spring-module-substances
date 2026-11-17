package example.substance;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class DeserializeTest {

    @Test
    public void testDeserialize() throws IOException {
        String testJsonFile ="testJSON//ba88b751-f44a-4b38-8d17-fbcd45591844.json";
        File resource=new ClassPathResource(testJsonFile).getFile();
        String substanceJson =FileUtils.readFileToString(resource, Charset.defaultCharset());
        JsonMapper mapper = JsonMapper.builderWithJackson2Defaults()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        JsonNode substanceNode = mapper.valueToTree(substanceJson);
        Assertions.assertEquals(56818, substanceNode.asString().length());

    }
}
