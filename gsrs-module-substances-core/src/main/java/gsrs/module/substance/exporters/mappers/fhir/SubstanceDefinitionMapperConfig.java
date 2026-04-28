package gsrs.module.substance.exporters.mappers.fhir;

import gsrs.module.substance.exporters.profiles.ExporterProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for SubstanceDefinitionMapper that controls how Substances
 * are mapped to FHIR SubstanceDefinition resources.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubstanceDefinitionMapperConfig {
    
    /**
     * The export profile to use for mapping decisions
     */
    private ExporterProfile profile;
    
    /**
     * Whether to include extension metadata
     */
    @Builder.Default
    private boolean includeExtensions = true;
    
    /**
     * Whether to use contained resources for complex nested structures
     */
    @Builder.Default
    private boolean useContainedResources = true;
    
    /**
     * Maximum depth for nested resource expansion (0 = no expansion)
     */
    @Builder.Default
    private int maxNestingDepth = 1;
    
    /**
     * Whether to validate FHIR resources before returning them
     */
    @Builder.Default
    private boolean validateResources = false;
    
    /**
     * Whether to include internal-only fields (system fields)
     */
    @Builder.Default
    private boolean includeInternalFields = false;
}
