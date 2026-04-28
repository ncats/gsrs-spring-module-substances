package gsrs.module.substance.exporters.profiles;

import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapperConfig;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.SubstanceDefinition;

/**
 * Generic FHIR R4B export profile.
 * 
 * Provides a comprehensive, standards-compliant mapping with minimal regulatory-specific
 * extensions. Suitable for general FHIR interoperability and future-proofing.
 * 
 * Uses GSRS-specific code system URIs to avoid conflicts with standard FHIR systems.
 */
@Slf4j
public class GenericFhirExportProfile extends BaseExporterProfile {
    
    private static final String PROFILE_NAME = "GENERIC";
    private static final String DISPLAY_NAME = "Generic FHIR R4B Profile";
    private static final String DESCRIPTION = "IDMP-inspired generic FHIR R4B mapping with comprehensive field inclusion and GSRS-specific code systems";
    
    public GenericFhirExportProfile() {
        super(new GenericFhirExportProfileConfig());
    }
    
    public GenericFhirExportProfile(ExporterProfileConfig config) {
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
                .includeExtensions(false)  // Keep generic, minimal extensions
                .useContainedResources(true)
                .validateResources(false)
                .build();
            
            // Map all fields using field mappers
            mapAllFields(sd, substance, mapperConfig);
            
            return sd;
        } catch (Exception e) {
            log.error("Error mapping substance to FHIR for Generic profile: {}", substance.getUuid(), e);
            return null;
        }
    }
}
