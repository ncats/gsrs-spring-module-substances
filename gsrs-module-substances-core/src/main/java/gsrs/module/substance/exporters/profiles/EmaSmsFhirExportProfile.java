package gsrs.module.substance.exporters.profiles;

import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapperConfig;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;

/**
 * EMA SMS (Substance Master Submission) FHIR R4B export profile.
 * 
 * Maps Substances to FHIR SubstanceDefinition resources with EMA SPOR-specific
 * extensions and field subsetting according to EMA requirements.
 * 
 * Uses ibuprofen.json as reference for extension structures.
 */
@Slf4j
public class EmaSmsFhirExportProfile extends BaseExporterProfile {
    
    private static final String PROFILE_NAME = "EMA_SMS";
    private static final String DISPLAY_NAME = "EMA SMS FHIR Profile";
    private static final String DESCRIPTION = "EMA SPOR FHIR R4B profile with SMS (Substance Master Submission) extension mapping";
    
    public EmaSmsFhirExportProfile() {
        super(new EmaSmsFhirExportProfileConfig());
    }
    
    public EmaSmsFhirExportProfile(ExporterProfileConfig config) {
        super(config);
    }
    
    @Override
    public String getProfileName() {
        return PROFILE_NAME;
    }
    
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    public SubstanceDefinition mapSubstanceToFhir(Substance substance) {
        if (substance == null) {
            return null;
        }
        
        try {
            // Create base FHIR resource
            SubstanceDefinition sd = createBaseSubstanceDefinition(substance);
            
            // Create mapper config for this profile
            SubstanceDefinitionMapperConfig mapperConfig = SubstanceDefinitionMapperConfig.builder()
                .profile(this)
                .includeExtensions(true)
                .useContainedResources(true)
                .validateResources(false)
                .build();
            
            // Map all fields using field mappers
            mapAllFields(sd, substance, mapperConfig);
            
            // Add EMA-specific extensions
            addEmaExtensions(sd, substance);
            
            return sd;
        } catch (Exception e) {
            log.error("Error mapping substance to FHIR for EMA profile: {}", substance.getUuid(), e);
            return null;
        }
    }
    
    /**
     * Add EMA-specific extensions to SubstanceDefinition
     * @param sd the SubstanceDefinition resource
     * @param substance the source Substance
     */
    private void addEmaExtensions(SubstanceDefinition sd, Substance substance) {
        // Add currentSubstance extension
        if (config.getExtensionUris().containsKey("currentSubstance")) {
            Extension extCurrent = new Extension(config.getExtensionUris().get("currentSubstance"));
            Reference substRef = new Reference();
            substRef.setReference("SubstanceDefinition/" + substance.getUuid());
            extCurrent.setValue(substRef);
            sd.addExtension(extCurrent);
        }
        
        // Add dataClassification extension - delegate to profile config
        if (config.getExtensionUris().containsKey("dataClassification") && 
            config.getDataClassificationCodeSystem() != null) {
            Extension extClassif = new Extension(config.getExtensionUris().get("dataClassification"));
            
            String classifCode = config.getDataClassificationCode("Public");
            String classifDisplay = config.getDataClassificationDisplay("Public");
            
            CodeableConcept classif = new CodeableConcept();
            classif.addCoding()
                .setSystem(config.getDataClassificationCodeSystem())
                .setCode(classifCode != null ? classifCode : "Public")
                .setDisplay(classifDisplay != null ? classifDisplay : "Public");
            extClassif.setValue(classif);
            sd.addExtension(extClassif);
        }
    }
}
