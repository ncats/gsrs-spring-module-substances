package gsrs.module.substance.exporters.profiles;

import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapperConfig;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.SubstanceDefinition;

/**
 * GSRS FHIR R4B export profile (FDA-compatible).
 * 
 * Provides comprehensive mapping of all Substance fields to FHIR SubstanceDefinition
 * resources with minimal filtering. No subsetting or regulatory-specific restrictions.
 * 
 * Suitable for FDA submissions and maximum interoperability use cases.
 */
@Slf4j
public class GsrsFhirExportProfile extends BaseExporterProfile {
    
    private static final String PROFILE_NAME = "GSRS";
    private static final String DISPLAY_NAME = "GSRS FDA-Compatible FHIR R4B Profile";
    private static final String DESCRIPTION = "Full GSRS to FHIR R4B mapping with comprehensive field inclusion and GSRS code systems, optimized for FDA compatibility and maximum interoperability";
    
    public GsrsFhirExportProfile() {
        super(new GsrsFhirExportProfileConfig());
    }
    
    public GsrsFhirExportProfile(ExporterProfileConfig config) {
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
            
            // Create mapper config for this profile with all fields included
            SubstanceDefinitionMapperConfig mapperConfig = SubstanceDefinitionMapperConfig.builder()
                .profile(this)
                .includeExtensions(true)
                .useContainedResources(true)
                .includeInternalFields(false)  // Still exclude internal-only fields
                .validateResources(false)
                .build();
            
            // Map all fields using field mappers
            mapAllFields(sd, substance, mapperConfig);
            
            return sd;
        } catch (Exception e) {
            log.error("Error mapping substance to FHIR for GSRS profile: {}", substance.getUuid(), e);
            return null;
        }
    }
}
