package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.ginas.models.v1.Substance;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class DeserializeTest {
    @Test
    public void testDeserialize() throws IOException {
        String testJsonFile ="testJSON//ba88b751-f44a-4b38-8d17-fbcd45591844.json";
        File resource=new ClassPathResource(testJsonFile).getFile();
        String substanceJson =FileUtils.readFileToString(resource, Charset.defaultCharset());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode substanceNode = mapper.valueToTree(substanceJson);
        Assertions.assertEquals(56818, substanceNode.asText().length());

    }
}
