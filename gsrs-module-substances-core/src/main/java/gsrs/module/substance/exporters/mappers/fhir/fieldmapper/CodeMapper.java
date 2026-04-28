package gsrs.module.substance.exporters.mappers.fhir.fieldmapper;

import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapper;
import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapperConfig;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.SubstanceDefinition;

import java.util.List;

/**
 * Maps Substance codes to FHIR SubstanceDefinition.code elements.
 * 
 * Handles code system URI translation and profile-specific code filtering.
 */
@Slf4j
public class CodeMapper extends SubstanceDefinitionMapper {
    
    public CodeMapper(SubstanceDefinitionMapperConfig config) {
        super(config);
    }
    
    /**
     * Map codes from Substance to SubstanceDefinition
     * @param sd the SubstanceDefinition resource to populate
     * @param substance the source Substance
     */
    public void mapCodes(SubstanceDefinition sd, Substance substance) {
        if (!shouldIncludeField("codes") || substance == null) {
            return;
        }
        
        List<Code> codes = substance.getCodes();
        if (isEmpty(codes)) {
            return;
        }
        
        for (Code code : codes) {
            mapCode(sd, code);
        }
    }
    
    /**
     * Map a single code to SubstanceDefinition.code
     * @param sd the SubstanceDefinition resource
     * @param code the Code to map
     */
    private void mapCode(SubstanceDefinition sd, Code code) {
        if (code == null || isEmpty(code.getCode())) {
            return;
        }
        
        SubstanceDefinition.SubstanceDefinitionCodeComponent codeComponent =
            new SubstanceDefinition.SubstanceDefinitionCodeComponent();
        
        // Map code system and code value
        String codeSystem = getCodeSystemUri(code.getCodeSystem());
        
        CodeableConcept codeable = new CodeableConcept();
        codeable.addCoding()
            .setSystem(codeSystem)
            .setCode(code.getCode())
            .setDisplay(getCodeDisplay(code));
        
        codeComponent.setCode(codeable);
        
        // Add status if available
        if (!isEmpty(code.getStatus())) {
            codeable.setText(code.getStatus());
        }
        
        sd.addCode(codeComponent);
    }
    
    /**
     * Get display text for a code
     * @param code the Code object
     * @return display text
     */
    private String getCodeDisplay(Code code) {
        // Try to use description or code system as fallback
        if (!isEmpty(code.getDescription())) {
            return code.getDescription();
        }
        if (!isEmpty(code.getCodeSystem())) {
            return code.getCodeSystem();
        }
        return code.getCode();
    }
    
    /**
     * Implementation of abstract map method (not used for field mappers)
     */
    @Override
    public SubstanceDefinition map(Substance substance) {
        // This mapper only handles the codes field, not the entire substance
        throw new UnsupportedOperationException("Use mapCodes() instead");
    }
}
