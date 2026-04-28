package gsrs.module.substance.exporters.mappers.fhir.fieldmapper;

import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapper;
import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapperConfig;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.SubstanceDefinition;

import java.util.List;

/**
 * Maps Substance relationships to FHIR SubstanceDefinition.relationship elements.
 * 
 * Transforms substance-to-substance relationships into FHIR reference structures.
 */
@Slf4j
public class RelationshipMapper extends SubstanceDefinitionMapper {
    
    public RelationshipMapper(SubstanceDefinitionMapperConfig config) {
        super(config);
    }
    
    /**
     * Map relationships from Substance to SubstanceDefinition
     * @param sd the SubstanceDefinition resource to populate
     * @param substance the source Substance
     */
    public void mapRelationships(SubstanceDefinition sd, Substance substance) {
        if (!shouldIncludeField("relationships") || substance == null) {
            return;
        }
        
        List<Relationship> relationships = substance.getRelationships();
        if (isEmpty(relationships)) {
            return;
        }
        
        for (Relationship relationship : relationships) {
            mapRelationship(sd, relationship);
        }
    }
    
    /**
     * Map a single relationship to SubstanceDefinition.relationship
     * @param sd the SubstanceDefinition resource
     * @param relationship the Relationship to map
     */
    private void mapRelationship(SubstanceDefinition sd, Relationship relationship) {
        if (relationship == null) {
            return;
        }
        
        SubstanceDefinition.SubstanceDefinitionRelationshipComponent relComponent =
            new SubstanceDefinition.SubstanceDefinitionRelationshipComponent();
        
        // Map substance reference
        if (relationship.getRelatedSubstance() != null && 
            !isEmpty(relationship.getRelatedSubstance().getRefuuid())) {
            Reference substRef = new Reference();
            substRef.setReference("SubstanceDefinition/" + relationship.getRelatedSubstance().getRefuuid());
            relComponent.setSubstanceDefinitionReference(substRef);
        } else if (!isEmpty(relationship.getRelatedSubstanceUUID())) {
            Reference substRef = new Reference();
            substRef.setReference("SubstanceDefinition/" + relationship.getRelatedSubstanceUUID());
            relComponent.setSubstanceDefinitionReference(substRef);
        }
        
        // Map relationship type - delegate to profile config for code system
        if (!isEmpty(relationship.getType())) {
            CodeableConcept typeCode = new CodeableConcept();
            // Use profile-specific code system (e.g., EMA SPOR) or default
            String codeSystem = getRelationshipTypeCodeSystem();
            String relCode = mapRelationshipTypeCode(relationship.getType());
            typeCode.addCoding()
                .setSystem(codeSystem)
                .setCode(relCode != null ? relCode : relationship.getType())
                .setDisplay(relationship.getType());
            relComponent.setType(typeCode);
        }
        
        // Map amount if present
        if (relationship.getAmount() != null && !isEmpty(relationship.getAmount().toString())) {
            relComponent.setAmountRatio(null); // Could be extended for more complex amounts
        }
        
        sd.addRelationship(relComponent);
    }
    
    /**
     * Get the code system URI for relationship types from profile config
     * @return code system URI (defaults to EMA SPOR for backward compatibility)
     */
    private String getRelationshipTypeCodeSystem() {
        if (getProfile() != null && getProfile().getConfig() != null) {
            String mapped = getProfile().getConfig().mapCodeSystemToFhirUri("RELATIONSHIP_TYPE");
            if (mapped != null) {
                return mapped;
            }
        }
        // Default to EMA SPOR for backward compatibility
        return "https://spor.ema.europa.eu/v1/lists/200000004946";
    }
    
    /**
     * Map GSRS relationship type to FHIR code
     * @param gsrsType the GSRS relationship type
     * @return FHIR code
     */
    private String mapRelationshipTypeCode(String gsrsType) {
        if (gsrsType == null) {
            return "Unknown";
        }
        
        // Map common GSRS relationship types to SPOR codes
        switch (gsrsType.toLowerCase()) {
            case "salt":
                return "200000004960"; // Salt/Ester form
            case "parent":
                return "200000004956"; // Parent
            case "metabolite":
                return "200000004957"; // Metabolite
            case "active moiety":
                return "200000004965"; // Active moiety
            case "isomer":
                return "200000004961"; // Isomer
            case "polymorph":
                return "200000004964"; // Polymorph
            case "impurity":
                return "200000004963"; // Impurity
            case "authorized":
            case "authorised to development":
                return "200000004958"; // Authorised to Development
            default:
                return "200000004946"; // Generic relationship
        }
    }
    
    /**
     * Implementation of abstract map method (not used for field mappers)
     */
    @Override
    public SubstanceDefinition map(Substance substance) {
        // This mapper only handles the relationships field, not the entire substance
        throw new UnsupportedOperationException("Use mapRelationships() instead");
    }
}
