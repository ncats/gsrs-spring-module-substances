package gsrs.module.substance.misc.emasmsfhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.OutputFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmaSmsFhirExporterFactory Tests")
class EmaSmsFhirExporterFactoryTest {

    private EmaSmsFhirExporterFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EmaSmsFhirExporterFactory();
    }

    @Test
    @DisplayName("getPrimaryCodeSystem returns null by default")
    void primaryCodeSystem_defaultNull() {
        assertNull(factory.getPrimaryCodeSystem());
    }

    @Test
    @DisplayName("setPrimaryCodeSystem and getPrimaryCodeSystem round-trip")
    void primaryCodeSystem_setAndGet() {
        factory.setPrimaryCodeSystem("BDNUM");
        assertEquals("BDNUM", factory.getPrimaryCodeSystem());
    }

    @Test
    @DisplayName("getSupportedFormats returns a singleton set with the emasmsfhir format")
    void getSupportedFormats_returnsSingleton() {
        Set<OutputFormat> formats = factory.getSupportedFormats();
        assertNotNull(formats);
        assertEquals(1, formats.size());
        OutputFormat fmt = formats.iterator().next();
        assertEquals("emasmsfhirsd.txt", fmt.getExtension());
    }

    @Test
    @DisplayName("supports returns true for the configured format")
    void supports_matchingFormat_returnsTrue() {
        OutputFormat format = factory.getSupportedFormats().iterator().next();
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        DefaultParameters params = new DefaultParameters(format, false, details);
        assertTrue(factory.supports(params));
    }

    @Test
    @DisplayName("supports returns false for an unrecognized format")
    void supports_nonMatchingFormat_returnsFalse() {
        OutputFormat other = new OutputFormat("other.txt", "Other");
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        DefaultParameters params = new DefaultParameters(other, false, details);
        assertFalse(factory.supports(params));
    }

    @Test
    @DisplayName("getSchema returns non-null JsonNode")
    void getSchema_returnsNonNull() {
        JsonNode schema = factory.getSchema();
        assertNotNull(schema);
        assertTrue(schema.isObject(), "Schema should be a JSON object");
    }

    @Test
    @DisplayName("getSchema with primaryCodeSystem set returns non-null schema")
    void getSchema_withPrimaryCodeSystem_returnsNonNull() {
        factory.setPrimaryCodeSystem("BDNUM");
        JsonNode schema = factory.getSchema();
        assertNotNull(schema);
        assertTrue(schema.isObject());
    }

    @Test
    @DisplayName("getSchema without primaryCodeSystem returns non-null schema")
    void getSchema_withoutPrimaryCodeSystem_returnsNonNull() {
        JsonNode schema = factory.getSchema();
        assertNotNull(schema);
    }

    @Test
    @DisplayName("Constants have expected values")
    void constants_haveExpectedValues() {
        assertEquals("omitPrimaryCodeSystemField", EmaSmsFhirExporterFactory.PRIMARY_CODE_SYSTEM_PARAMETERS);
        assertEquals("approvalIdName", EmaSmsFhirExporterFactory.APPROVAL_ID_NAME_PARAMETERS);
        assertEquals("APPROVAL_ID", EmaSmsFhirExporterFactory.DEFAULT_APPROVAL_ID_NAME);
    }
}


