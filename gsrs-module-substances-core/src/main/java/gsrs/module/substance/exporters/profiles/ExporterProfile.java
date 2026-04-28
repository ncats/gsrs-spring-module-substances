package gsrs.module.substance.exporters.profiles;

import ix.ginas.models.v1.Substance;
import org.hl7.fhir.r4.model.SubstanceDefinition;

/**
 * Defines a contract for FHIR export profiles that specify how Substances
 * should be mapped to FHIR SubstanceDefinition resources.
 * 
 * Each profile can implement different field subsetting, filtering, and
 * custom extension logic based on regulatory or organizational requirements.
 */
public interface ExporterProfile {
    
    /**
     * Get the name of this profile (e.g., "EMA_SMS", "GENERIC", "GSRS")
     * @return profile name
     */
    String getProfileName();
    
    /**
     * Get a human-readable display name for this profile
     * @return display name
     */
    String getDisplayName();
    
    /**
     * Get the description of this profile
     * @return profile description
     */
    String getDescription();
    
    /**
     * Map a Substance to a FHIR SubstanceDefinition resource according to this profile's rules
     * @param substance the Substance to map
     * @return FHIR SubstanceDefinition resource
     */
    SubstanceDefinition mapSubstanceToFhir(Substance substance);
    
    /**
     * Get configuration for this profile (e.g., code system mappings, filtering rules)
     * @return profile configuration
     */
    ExporterProfileConfig getConfig();
}
