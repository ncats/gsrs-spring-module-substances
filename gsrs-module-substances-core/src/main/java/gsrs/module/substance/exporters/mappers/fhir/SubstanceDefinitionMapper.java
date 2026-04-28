package gsrs.module.substance.exporters.mappers.fhir;

import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;

import java.util.*;

/**
 * Abstract base class for mapping Substance objects to FHIR SubstanceDefinition resources.
 * 
 * This class provides common mapping utilities, code system URI translations,
 * and field inclusion checking based on the export profile configuration.
 */
@Slf4j
public abstract class SubstanceDefinitionMapper {
    
    protected SubstanceDefinitionMapperConfig config;
    
    public SubstanceDefinitionMapper(SubstanceDefinitionMapperConfig config) {
        this.config = config;
    }
    
    /**
     * Map a Substance to a FHIR SubstanceDefinition resource
     * @param substance the Substance to map
     * @return FHIR SubstanceDefinition resource
     */
    public abstract SubstanceDefinition map(Substance substance);
    
    /**
     * Get the profile from the mapper configuration
     * @return the ExporterProfile
     */
    protected gsrs.module.substance.exporters.profiles.ExporterProfile getProfile() {
        return config.getProfile();
    }
    
    /**
     * Check if a field should be included in the export based on profile configuration
     * @param fieldName the field name
     * @return true if the field should be included
     */
    protected boolean shouldIncludeField(String fieldName) {
        if (config.getProfile() == null) {
            return true;
        }
        return config.getProfile().getConfig().shouldIncludeField(fieldName);
    }
    
    /**
     * Map a GSRS code system URI to a FHIR code system URI
     * @param gsrsCodeSystem the GSRS code system
     * @return FHIR code system URI
     */
    protected String getCodeSystemUri(String gsrsCodeSystem) {
        if (gsrsCodeSystem == null) {
            return null;
        }
        
        if (config.getProfile() != null && config.getProfile().getConfig() != null) {
            String mapped = config.getProfile().getConfig().mapCodeSystemToFhirUri(gsrsCodeSystem);
            if (mapped != null) {
                return mapped;
            }
        }
        
        // Default: use GSRS base URI for unmapped systems
        String baseUri = getGsrsBaseUri();
        return baseUri + gsrsCodeSystem;
    }
    
    /**
     * Get the base URI for GSRS-specific codes
     * @return base URI
     */
    protected String getGsrsBaseUri() {
        if (config.getProfile() != null && config.getProfile().getConfig() != null) {
            return config.getProfile().getConfig().getGsrsBaseUri();
        }
        return "https://gsrs.ncats.nih.gov/fhir/";
    }
    
    /**
     * Get extension URIs from profile configuration
     * @return map of extension names to URIs
     */
    protected Map<String, String> getExtensionUris() {
        if (config.getProfile() != null && config.getProfile().getConfig() != null) {
            return config.getProfile().getConfig().getExtensionUris();
        }
        return new HashMap<>();
    }
    
    /**
     * Add a code to SubstanceDefinition.code[]
     * @param sd the SubstanceDefinition resource
     * @param codeSystem the code system URI
     * @param code the code value
     * @param display the display text
     */
    protected void addCode(SubstanceDefinition sd, String codeSystem, String code, String display) {
        if (sd == null || codeSystem == null || code == null) {
            return;
        }
        
        SubstanceDefinition.SubstanceDefinitionCodeComponent codeComponent = 
            new SubstanceDefinition.SubstanceDefinitionCodeComponent();
        
        CodeableConcept codeable = new CodeableConcept();
        codeable.addCoding()
            .setSystem(codeSystem)
            .setCode(code)
            .setDisplay(display);
        
        codeComponent.setCode(codeable);
        sd.addCode(codeComponent);
    }
    
    /**
     * Add a name to SubstanceDefinition.name[]
     * @param sd the SubstanceDefinition resource
     * @param name the name value
     * @param language the language code (e.g., "en", "fr")
     * @param preferred whether this is the preferred name
     */
    protected void addName(SubstanceDefinition sd, String name, String language, boolean preferred) {
        if (sd == null || name == null) {
            return;
        }
        
        SubstanceDefinition.SubstanceDefinitionNameComponent nameComponent =
            new SubstanceDefinition.SubstanceDefinitionNameComponent();
        
        nameComponent.setName(name);
        nameComponent.setPreferred(preferred);
        
        if (language != null && getProfile() != null && 
            getProfile().getConfig().getLanguageCodeSystem() != null) {
            String languageCodeSystem = getProfile().getConfig().getLanguageCodeSystem();
            nameComponent.setLanguage(
                new CodeableConcept().addCoding()
                    .setSystem(languageCodeSystem)
                    .setCode(language)
            );
        }
        
        sd.addName(nameComponent);
    }
    
    /**
     * Create a Coding within a CodeableConcept
     * @param system the code system URI
     * @param code the code value
     * @param display the display text
     * @return CodeableConcept with the coding
     */
    protected CodeableConcept createCodeableConcept(String system, String code, String display) {
        CodeableConcept concept = new CodeableConcept();
        concept.addCoding()
            .setSystem(system)
            .setCode(code)
            .setDisplay(display);
        return concept;
    }
    
    /**
     * Create an Identifier
     * @param system the identifier system URI
     * @param value the identifier value
     * @return Identifier
     */
    protected Identifier createIdentifier(String system, String value) {
        Identifier identifier = new Identifier();
        identifier.setSystem(system);
        identifier.setValue(value);
        return identifier;
    }
    
    /**
     * Check if a value is null or empty string
     * @param value the value to check
     * @return true if null or empty
     */
    protected boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    /**
     * Check if a collection is null or empty
     * @param collection the collection to check
     * @return true if null or empty
     */
    protected boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
