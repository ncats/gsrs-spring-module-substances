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
 * Unit tests for NameMapper field mapper
 */
@DisplayName("NameMapper Tests")
public class NameMapperTest {
    
    private NameMapper mapper;
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
        mapper = new NameMapper(config);
        
        // Create test substance
        testSubstance = new Substance();
        testSubstance.setUuid("test-uuid-123");
    }
    
    @Test
    @DisplayName("Should map single name correctly")
    void testMapSingleName() {
        // Arrange
        Name name = new Name();
        name.setName("Ibuprofen");
        name.setPreferred(true);
        name.setLanguage("100000072147"); // English
        name.setStatus("Current");
        testSubstance.addName(name);
        
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapNames(sd, testSubstance);
        
        // Assert
        assertEquals(1, sd.getName().size());
        SubstanceDefinition.SubstanceDefinitionNameComponent nameComp = sd.getName().get(0);
        assertEquals("Ibuprofen", nameComp.getName());
        assertTrue(nameComp.getPreferred());
        assertNotNull(nameComp.getLanguage());
    }
    
    @Test
    @DisplayName("Should map multiple names")
    void testMapMultipleNames() {
        // Arrange
        Name name1 = new Name();
        name1.setName("Ibuprofen");
        name1.setPreferred(true);
        name1.setLanguage("100000072147"); // English
        testSubstance.addName(name1);
        
        Name name2 = new Name();
        name2.setName("Ibuprof\\u00e9n");
        name2.setPreferred(false);
        name2.setLanguage("100000072251"); // Portuguese
        testSubstance.addName(name2);
        
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapNames(sd, testSubstance);
        
        // Assert
        assertEquals(2, sd.getName().size());
        assertTrue(sd.getName().get(0).getPreferred());
        assertFalse(sd.getName().get(1).getPreferred());
    }
    
    @Test
    @DisplayName("Should skip null substance")
    void testSkipNullSubstance() {
        // Arrange
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapNames(sd, null);
        
        // Assert
        assertEquals(0, sd.getName().size());
    }
    
    @Test
    @DisplayName("Should handle empty names list")
    void testHandleEmptyNamesList() {
        // Arrange
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapNames(sd, testSubstance);
        
        // Assert
        assertEquals(0, sd.getName().size());
    }
    
    @Test
    @DisplayName("Should map name status correctly")
    void testMapNameStatus() {
        // Arrange
        Name name = new Name();
        name.setName("Test Name");
        name.setStatus("Current");
        testSubstance.addName(name);
        
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapNames(sd, testSubstance);
        
        // Assert
        assertEquals(1, sd.getName().size());
        SubstanceDefinition.SubstanceDefinitionNameComponent nameComp = sd.getName().get(0);
        assertNotNull(nameComp.getStatus());
        assertTrue(nameComp.getStatus().getCoding().stream()
            .anyMatch(c -> "200000005004".equals(c.getCode())));
    }
}
