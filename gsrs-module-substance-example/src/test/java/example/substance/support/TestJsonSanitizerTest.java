package example.substance.support;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestJsonSanitizerTest {

    private static final JsonMapper MAPPER = JsonMapper.builderWithJackson2Defaults()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void stripAccessFieldsRemovesAuditFieldsRecursivelyWithoutMutatingSource() {
        JsonNode source = MAPPER.readTree("{\"access\":\"admin\",\"createdBy\":\"u1\",\"nested\":{\"approvedBy\":\"u2\",\"arr\":[{\"lastEditedBy\":\"u3\",\"value\":1}]},\"keep\":\"ok\"}");

        JsonNode sanitized = TestJsonSanitizer.stripAccessFields(source);

        assertNotSame(source, sanitized);
        assertFalse(sanitized.has("access"));
        assertFalse(sanitized.has("createdBy"));
        assertFalse(sanitized.path("nested").has("approvedBy"));
        assertFalse(sanitized.path("nested").path("arr").get(0).has("lastEditedBy"));
        assertEquals("ok", sanitized.path("keep").asString());

        // Ensure the source tree remains unchanged.
        assertTrue(source.has("access"));
        assertTrue(source.path("nested").has("approvedBy"));
    }

    @Test
    void stripAccessFieldsSupportsScalarNodes()  {
        JsonNode scalar = MAPPER.readTree("\"value\"");

        JsonNode sanitized = TestJsonSanitizer.stripAccessFields(scalar);

        assertEquals("value", sanitized.asString());
    }

    @Test
    void stripAccessFieldsThrowsOnNullInput() {
        assertThrows(NullPointerException.class, () -> TestJsonSanitizer.stripAccessFields(null));
    }
}

