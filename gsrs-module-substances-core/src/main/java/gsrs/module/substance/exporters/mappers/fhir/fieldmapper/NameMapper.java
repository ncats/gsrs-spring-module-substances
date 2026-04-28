package gsrs.module.substance.exporters.mappers.fhir.fieldmapper;

import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapper;
import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapperConfig;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.SubstanceDefinition;

import java.util.List;

/**
 * Maps Substance names to FHIR SubstanceDefinition.name elements.
 * 
 * Handles multi-language names with status (current/inactive), preferred flags,
 * and data classification extensions.
 */
@Slf4j
public class NameMapper extends SubstanceDefinitionMapper {
    
    public NameMapper(SubstanceDefinitionMapperConfig config) {
        super(config);
    }
    
    /**
     * Map names from Substance to SubstanceDefinition
     * @param sd the SubstanceDefinition resource to populate
     * @param substance the source Substance
     */
    public void mapNames(SubstanceDefinition sd, Substance substance) {
        if (!shouldIncludeField("names") || substance == null) {
            return;
        }
        
        List<Name> names = substance.getNames();
        if (isEmpty(names)) {
            return;
        }
        
        for (Name name : names) {
            mapName(sd, name);
        }
    }
    
    /**
     * Map a single name to SubstanceDefinition.name
     * @param sd the SubstanceDefinition resource
     * @param name the Name to map
     */
    private void mapName(SubstanceDefinition sd, Name name) {
        if (isEmpty(name.getName())) {
            return;
        }
        
        SubstanceDefinition.SubstanceDefinitionNameComponent nameComponent =
            new SubstanceDefinition.SubstanceDefinitionNameComponent();
        
        // Set name value
        nameComponent.setName(name.getName());
        
        // Set preferred status
        if (name.getPreferred() != null) {
            nameComponent.setPreferred(name.getPreferred());
        }
        
        // Map status (Current/Inactive) - delegate to profile config
        if (name.getStatus() != null && getProfile().getConfig().getStatusCodeSystem() != null) {
            CodeableConcept status = new CodeableConcept();
            Coding statusCoding = status.addCoding();
            String statusCodeSystem = getProfile().getConfig().getStatusCodeSystem();
            statusCoding.setSystem(statusCodeSystem);
            
            String statusCode = getProfile().getConfig().getStatusCode(name.getStatus());
            String statusDisplay = getProfile().getConfig().getStatusDisplay(name.getStatus());
            
            statusCoding.setCode(statusCode != null ? statusCode : name.getStatus());
            statusCoding.setDisplay(statusDisplay != null ? statusDisplay : name.getStatus());
            
            nameComponent.setStatus(status);
        }
        
        // Map language
        if (!isEmpty(name.getLanguage())) {
            mapLanguage(nameComponent, name.getLanguage());
        }
        
        // Add extension for data classification if available
        if (name.getLabels() != null && !name.getLabels().isEmpty()) {
            // Map data classification as an extension - delegate to profile config
            if (getExtensionUris().containsKey("dataClassification") && 
                getProfile().getConfig().getDataClassificationCodeSystem() != null) {
                String extUri = getExtensionUris().get("dataClassification");
                CodeableConcept classifConcept = new CodeableConcept();
                
                String classifCode = getProfile().getConfig().getDataClassificationCode("Public");
                String classifDisplay = getProfile().getConfig().getDataClassificationDisplay("Public");
                
                classifConcept.addCoding()
                    .setSystem(getProfile().getConfig().getDataClassificationCodeSystem())
                    .setCode(classifCode != null ? classifCode : "Public")
                    .setDisplay(classifDisplay != null ? classifDisplay : "Public");
                
                nameComponent.addExtension(extUri, classifConcept);
            }
        }
        
        sd.addName(nameComponent);
    }
    
    /**
     * Map language code to FHIR CodeableConcept
     * @param nameComponent the name component to update
     * @param languageCode the language code
     */
    private void mapLanguage(SubstanceDefinition.SubstanceDefinitionNameComponent nameComponent, String languageCode) {
        if (isEmpty(languageCode)) {
            return;
        }
        
        String languageCodeSystem = getProfile().getConfig().getLanguageCodeSystem();
        if (languageCodeSystem == null) {
            return;
        }
        
        CodeableConcept language = new CodeableConcept();
        Coding langCoding = language.addCoding();
        langCoding.setSystem(languageCodeSystem);
        langCoding.setCode(languageCode);
        
        // Map common language codes to display names
        langCoding.setDisplay(getLanguageDisplayName(languageCode));
        
        nameComponent.setLanguage(language);
    }
    
    /**
     * Get display name for language code
     * @param code the language code
     * @return display name
     */
    private String getLanguageDisplayName(String code) {
        // Map of common SPOR language codes to display names
        switch (code) {
            case "100000072147": return "English";
            case "100000072175": return "French";
            case "100000072178": return "German";
            case "100000072169": return "Dutch";
            case "100000072194": return "Italian";
            case "100000072264": return "Spanish";
            case "100000072251": return "Portuguese";
            case "100000072288": return "Swedish";
            case "100000072243": return "Norwegian";
            case "100000072167": return "Czech";
            case "100000072172": return "Estonian";
            case "100000072205": return "Latvian";
            case "100000072206": return "Lithuanian";
            case "100000072217": return "Polish";
            case "100000072254": return "Romanian";
            case "100000072258": return "Croatian";
            case "100000072259": return "Slovak";
            case "100000072260": return "Slovenian";
            case "100000072187": return "Hungarian";
            case "100000072181": return "Greek";
            case "100000072142": return "Bulgarian";
            case "100000072149": return "Finnish";
            case "100000072156": return "Icelandic";
            case "100000072226": return "Latin";
            default: return code;
        }
    }
    
    /**
     * Implementation of abstract map method (not used for field mappers)
     */
    @Override
    public SubstanceDefinition map(Substance substance) {
        // This mapper only handles the names field, not the entire substance
        throw new UnsupportedOperationException("Use mapNames() instead");
    }
}
