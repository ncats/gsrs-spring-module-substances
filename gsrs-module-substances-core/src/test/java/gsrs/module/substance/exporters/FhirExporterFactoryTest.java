package gsrs.module.substance.exporters;

import gsrs.module.substance.exporters.profiles.*;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FhirExporterFactory
 */
@DisplayName("FhirExporterFactory Tests")
public class FhirExporterFactoryTest {
    
    private FhirExporterFactory factory;
    
    @BeforeEach
    void setUp() {
        factory = new FhirExporterFactory();
    }
    
    @Test
    @DisplayName("Should support FHIR format")
    void testSupportsFormat() {
        // Arrange
        ExporterFactory.Parameters params = new ExporterFactory.Parameters();
        OutputFormat format = new OutputFormat("fhir", "FHIR");
        params.setFormat(format);
        
        // Act
        boolean supports = factory.supports(params);
        
        // Assert
        assertTrue(supports);
    }
    
    @Test
    @DisplayName("Should not support non-FHIR formats")
    void testDoesNotSupportOtherFormats() {
        // Arrange
        ExporterFactory.Parameters params = new ExporterFactory.Parameters();
        OutputFormat format = new OutputFormat("json", "JSON");
        params.setFormat(format);
        
        // Act
        boolean supports = factory.supports(params);
        
        // Assert
        assertFalse(supports);
    }
    
    @Test
    @DisplayName("Should return supported formats")
    void testGetSupportedFormats() {
        // Act
        Set<OutputFormat> formats = factory.getSupportedFormats();
        
        // Assert
        assertEquals(1, formats.size());
        assertTrue(formats.stream()
            .anyMatch(f -> "fhir".equals(f.getExtension())));
    }
    
    @Test
    @DisplayName("Should create exporter with default profile")
    void testCreateExporterWithDefaultProfile() throws Exception {
        // Arrange
        ExporterFactory.Parameters params = new ExporterFactory.Parameters();
        OutputFormat format = new OutputFormat("fhir", "FHIR");
        params.setFormat(format);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Act
        Exporter<Substance> exporter = factory.createNewExporter(out, params);
        
        // Assert
        assertNotNull(exporter);
        assertTrue(exporter instanceof FhirExporter);
    }
    
    @Test
    @DisplayName("Should create exporter with EMA SMS profile")
    void testCreateExporterWithEmaProfile() throws Exception {
        // Arrange
        ExporterFactory.Parameters params = new ExporterFactory.Parameters();
        OutputFormat format = new OutputFormat("fhir", "FHIR");
        params.setFormat(format);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("profile", "EMA_SMS");
        params.setParameters(paramMap);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Act
        Exporter<Substance> exporter = factory.createNewExporter(out, params);
        
        // Assert
        assertNotNull(exporter);
        assertTrue(exporter instanceof FhirExporter);
    }
    
    @Test
    @DisplayName("Should create exporter with GENERIC profile")
    void testCreateExporterWithGenericProfile() throws Exception {
        // Arrange
        ExporterFactory.Parameters params = new ExporterFactory.Parameters();
        OutputFormat format = new OutputFormat("fhir", "FHIR");
        params.setFormat(format);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("profile", "GENERIC");
        params.setParameters(paramMap);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Act
        Exporter<Substance> exporter = factory.createNewExporter(out, params);
        
        // Assert
        assertNotNull(exporter);
        assertTrue(exporter instanceof FhirExporter);
    }
    
    @Test
    @DisplayName("Should handle unknown profile gracefully")
    void testHandleUnknownProfile() throws Exception {
        // Arrange
        ExporterFactory.Parameters params = new ExporterFactory.Parameters();
        OutputFormat format = new OutputFormat("fhir", "FHIR");
        params.setFormat(format);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("profile", "UNKNOWN");
        params.setParameters(paramMap);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Act
        Exporter<Substance> exporter = factory.createNewExporter(out, params);
        
        // Assert
        assertNotNull(exporter, "Should default to GSRS profile");
        assertTrue(exporter instanceof FhirExporter);
    }
    
    @Test
    @DisplayName("Should throw exception for null output stream")
    void testNullOutputStreamThrowsException() {
        // Arrange
        ExporterFactory.Parameters params = new ExporterFactory.Parameters();
        OutputFormat format = new OutputFormat("fhir", "FHIR");
        params.setFormat(format);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            factory.createNewExporter(null, params)
        );
    }
    
    @Test
    @DisplayName("Should throw exception for null parameters")
    void testNullParametersThrowsException() {
        // Arrange
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            factory.createNewExporter(out, null)
        );
    }
    
    @Test
    @DisplayName("Should return valid JSON schema")
    void testGetSchema() {
        // Act
        var schema = factory.getSchema();
        
        // Assert
        assertNotNull(schema);
        assertTrue(schema.has("type"));
        assertTrue(schema.has("properties"));
        assertTrue(schema.get("properties").has("profile"));
    }
    
    @Test
    @DisplayName("Should support profile registration")
    void testProfileRegistration() {
        // Arrange
        ExporterProfile customProfile = new GsrsFhirExportProfile();
        
        // Act
        factory.registerProfile("CUSTOM", customProfile);
        ExporterProfile retrieved = factory.getProfile("CUSTOM");
        
        // Assert
        assertNotNull(retrieved);
        assertEquals(customProfile, retrieved);
    }
    
    @Test
    @DisplayName("Should list all profile names")
    void testGetProfileNames() {
        // Act
        Collection<String> names = factory.getProfileNames();
        
        // Assert
        assertTrue(names.contains("EMA_SMS"));
        assertTrue(names.contains("GENERIC"));
        assertTrue(names.contains("GSRS"));
        assertEquals(3, names.size());
    }
}
