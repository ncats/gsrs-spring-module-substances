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
 * Unit tests for RelationshipMapper field mapper
 */
@DisplayName("RelationshipMapper Tests")
public class RelationshipMapperTest {
    
    private RelationshipMapper mapper;
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
        mapper = new RelationshipMapper(config);
        
        // Create test substance
        testSubstance = new Substance();
        testSubstance.setUuid("test-uuid-123");
    }
    
    @Test
    @DisplayName("Should map single relationship correctly")
    void testMapSingleRelationship() {
        // Arrange
        Relationship rel = new Relationship();
        Substance relSubst = new Substance();
        relSubst.setUuid("related-uuid-456");
        rel.setRelatedSubstance(relSubst);
        rel.setType("Salt");
        testSubstance.addRelationship(rel);
        
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapRelationships(sd, testSubstance);
        
        // Assert
        assertEquals(1, sd.getRelationship().size());
        SubstanceDefinition.SubstanceDefinitionRelationshipComponent relComp = sd.getRelationship().get(0);
        assertNotNull(relComp.getSubstanceDefinitionReference());
        assertTrue(relComp.getSubstanceDefinitionReference().getReference().contains("related-uuid-456"));
    }
    
    @Test
    @DisplayName("Should map multiple relationships")
    void testMapMultipleRelationships() {
        // Arrange
        Relationship rel1 = new Relationship();
        Substance relSubst1 = new Substance();
        relSubst1.setUuid("related-uuid-456");
        rel1.setRelatedSubstance(relSubst1);
        rel1.setType("Salt");
        testSubstance.addRelationship(rel1);
        
        Relationship rel2 = new Relationship();
        Substance relSubst2 = new Substance();
        relSubst2.setUuid("related-uuid-789");
        rel2.setRelatedSubstance(relSubst2);
        rel2.setType("Metabolite");
        testSubstance.addRelationship(rel2);
        
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapRelationships(sd, testSubstance);
        
        // Assert
        assertEquals(2, sd.getRelationship().size());
    }
    
    @Test
    @DisplayName("Should skip null substance")
    void testSkipNullSubstance() {
        // Arrange
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapRelationships(sd, null);
        
        // Assert
        assertEquals(0, sd.getRelationship().size());
    }
    
    @Test
    @DisplayName("Should handle empty relationships list")
    void testHandleEmptyRelationshipsList() {
        // Arrange
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapRelationships(sd, testSubstance);
        
        // Assert
        assertEquals(0, sd.getRelationship().size());
    }
    
    @Test
    @DisplayName("Should map relationship type correctly")
    void testMapRelationshipType() {
        // Arrange
        Relationship rel = new Relationship();
        Substance relSubst = new Substance();
        relSubst.setUuid("related-uuid-456");
        rel.setRelatedSubstance(relSubst);
        rel.setType("Salt");
        testSubstance.addRelationship(rel);
        
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Act
        mapper.mapRelationships(sd, testSubstance);
        
        // Assert
        assertEquals(1, sd.getRelationship().size());
        SubstanceDefinition.SubstanceDefinitionRelationshipComponent relComp = sd.getRelationship().get(0);
        assertNotNull(relComp.getType());
        assertTrue(relComp.getType().getCoding().stream()
            .anyMatch(c -> "200000004960".equals(c.getCode())));
    }
}
