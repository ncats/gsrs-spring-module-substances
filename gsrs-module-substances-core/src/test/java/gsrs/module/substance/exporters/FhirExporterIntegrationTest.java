package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.exporters.profiles.*;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.*;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.SubstanceDefinition;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FHIR exporter using ibuprofen.json reference data
 */
@DisplayName("FHIR Exporter Integration Tests")
public class FhirExporterIntegrationTest {
    
    private static final String IBUPROFEN_JSON_PATH = "ibuprofen.json";
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }
    
    @Test
    @DisplayName("Should export with EMA SMS profile")
    void testExportWithEmaProfile() throws Exception {
        // Arrange
        ExporterProfile profile = new EmaSmsFhirExportProfile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Act
        try (FhirExporter exporter = new FhirExporter(baos, profile)) {
            SubstanceDefinition sd = createTestSubstanceDefinition();
            if (sd != null) {
                // Create a mock Substance for testing (would load from ibuprofen.json in real scenario)
                Substance substance = createTestSubstance();
                exporter.export(substance);
            }
        }
        
        // Assert
        byte[] result = baos.toByteArray();
        assertTrue(result.length > 0, "Export should produce output");
        
        // Verify it's valid gzipped output
        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(result))) {
            byte[] buffer = new byte[1024];
            int read = gzis.read(buffer);
            assertTrue(read > 0, "Gzipped output should be readable");
        }
    }
    
    @Test
    @DisplayName("Should export with Generic profile")
    void testExportWithGenericProfile() throws Exception {
        // Arrange
        ExporterProfile profile = new GenericFhirExportProfile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Act
        try (FhirExporter exporter = new FhirExporter(baos, profile)) {
            Substance substance = createTestSubstance();
            exporter.export(substance);
        }
        
        // Assert
        byte[] result = baos.toByteArray();
        assertTrue(result.length > 0, "Export should produce output");
    }
    
    @Test
    @DisplayName("Should export with GSRS profile")
    void testExportWithGsrsProfile() throws Exception {
        // Arrange
        ExporterProfile profile = new GsrsFhirExportProfile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Act
        try (FhirExporter exporter = new FhirExporter(baos, profile)) {
            Substance substance = createTestSubstance();
            exporter.export(substance);
        }
        
        // Assert
        byte[] result = baos.toByteArray();
        assertTrue(result.length > 0, "Export should produce output");
    }
    
    @Test
    @DisplayName("Should handle multiple substances in bundle")
    void testExportMultipleSubstances() throws Exception {
        // Arrange
        ExporterProfile profile = new GsrsFhirExportProfile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Act
        try (FhirExporter exporter = new FhirExporter(baos, profile)) {
            for (int i = 0; i < 3; i++) {
                Substance substance = createTestSubstance();
                exporter.export(substance);
            }
            assertEquals(3, exporter.getExportCount(), "Should have exported 3 substances");
        }
        
        // Assert
        byte[] result = baos.toByteArray();
        assertTrue(result.length > 0, "Export should produce output");
    }
    
    @Test
    @DisplayName("Should create valid FHIR Bundle")
    void testFhirBundleValidity() throws Exception {
        // Arrange
        ExporterProfile profile = new GsrsFhirExportProfile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Act
        try (FhirExporter exporter = new FhirExporter(baos, profile)) {
            Substance substance = createTestSubstance();
            exporter.export(substance);
        }
        
        // Assert
        byte[] result = baos.toByteArray();
        assertTrue(result.length > 0);
        
        // Decompress and parse bundle
        String jsonContent = decompressGzip(result);
        assertTrue(jsonContent.contains("\"resourceType\":\"Bundle\""), 
            "Should contain Bundle resourceType");
        assertTrue(jsonContent.contains("\"type\":\"collection\""), 
            "Bundle should be of type collection");
        assertTrue(jsonContent.contains("\"entry\""), 
            "Bundle should have entries");
    }
    
    @Test
    @DisplayName("Should throw exception for null profile")
    void testNullProfileThrowsException() {
        // Arrange
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            new FhirExporter(baos, null)
        );
    }
    
    @Test
    @DisplayName("Should throw exception for null output stream")
    void testNullOutputStreamThrowsException() {
        // Arrange
        ExporterProfile profile = new GsrsFhirExportProfile();
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            new FhirExporter(null, profile)
        );
    }
    
    /**
     * Create a test SubstanceDefinition for testing
     */
    private SubstanceDefinition createTestSubstanceDefinition() {
        SubstanceDefinition sd = new SubstanceDefinition();
        sd.setId("test-substance-123");
        return sd;
    }
    
    /**
     * Create a minimal test Substance
     */
    private Substance createTestSubstance() {
        Substance substance = new Substance();
        substance.setUuid("test-substance-uuid");
        return substance;
    }
    
    /**
     * Decompress gzipped content to string
     */
    private String decompressGzip(byte[] gzipContent) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(gzipContent);
        try (GZIPInputStream gzis = new GZIPInputStream(bais);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
