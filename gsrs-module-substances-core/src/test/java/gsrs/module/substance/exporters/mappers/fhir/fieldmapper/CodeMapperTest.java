package gsrs.module.substance.exporters.mappers.fhir.fieldmapper;

import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapperConfig;
import gsrs.module.substance.exporters.profiles.GsrsFhirExportProfile;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hl7.fhir.r4.model.SubstanceDefinition;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CodeMapper field mapper
 */
@DisplayName("CodeMapper Tests")
public class CodeMapperTest {
    
    private CodeMapper mapper;
    private SubstanceDefinitionMapperConfig config;
    private Substance testSubstance;
    
    @BeforeEach
    void setUp() {
        // Create mapper with GSRS profile
        GsrsFhirExportProfile profile = new GsrsFhirExportProfile();
        config = SubstanceDefinitionMapperConfig.builder()
            .profile(profile)
            .includeExtensions(true)
            .build();
        mapper = new CodeMapper(config);
        
        // Create test substance
        testSubstance = new Substance();
        testSubstance.setUuid("test-uuid-123");
    }
    
    @Test
    @DisplayName("Should map single code correctly")
    void testMapSingleCode() {
        // Arrange
        Code code = new Code();
        code.setCode("15687-27-1");
        code.setCodeSystem("CAS");
        code.setDescription("CAS Registry Number");
        testSubstance.addCode(code);
        
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapCodes(sd, testSubstance);
        
        // Assert
        assertEquals(1, sd.getCode().size());
        SubstanceDefinition.SubstanceDefinitionCodeComponent codeComp = sd.getCode().get(0);
        assertNotNull(codeComp.getCode());
        assertEquals("15687-27-1", codeComp.getCode().getCoding().get(0).getCode());
    }
    
    @Test
    @DisplayName("Should map multiple codes")
    void testMapMultipleCodes() {
        // Arrange
        Code code1 = new Code();
        code1.setCode("15687-27-1");
        code1.setCodeSystem("CAS");
        testSubstance.addCode(code1);
        
        Code code2 = new Code();
        code2.setCode("WK2XYI10QM");
        code2.setCodeSystem("UNII");
        testSubstance.addCode(code2);
        
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapCodes(sd, testSubstance);
        
        // Assert
        assertEquals(2, sd.getCode().size());
    }
    
    @Test
    @DisplayName("Should skip null substance")
    void testSkipNullSubstance() {
        // Arrange
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapCodes(sd, null);
        
        // Assert
        assertEquals(0, sd.getCode().size());
    }
    
    @Test
    @DisplayName("Should handle empty codes list")
    void testHandleEmptyCodesList() {
        // Arrange
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapCodes(sd, testSubstance);
        
        // Assert
        assertEquals(0, sd.getCode().size());
    }
    
    @Test
    @DisplayName("Should skip codes with null code value")
    void testSkipCodesWithNullValue() {
        // Arrange
        Code code = new Code();
        code.setCode(null);
        code.setCodeSystem("CAS");
        testSubstance.addCode(code);
        
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapCodes(sd, testSubstance);
        
        // Assert
        assertEquals(0, sd.getCode().size());
    }
    
    @Test
    @DisplayName("Should map code system correctly")
    void testMapCodeSystem() {
        // Arrange
        Code code = new Code();
        code.setCode("WK2XYI10QM");
        code.setCodeSystem("UNII");
        testSubstance.addCode(code);
        
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapCodes(sd, testSubstance);
        
        // Assert
        assertEquals(1, sd.getCode().size());
        String codeSystemUri = sd.getCode().get(0).getCode().getCoding().get(0).getSystem();
        assertNotNull(codeSystemUri);
        // Should use GSRS base URI or mapped URI
        assertTrue(codeSystemUri.contains("gsrs") || codeSystemUri.contains("unitsofmeasure"));
    }
}
