package gsrs.module.substance.exporters.profiles;

import java.util.Map;

/**
 * Holds configuration for an ExporterProfile such as code system mappings,
 * field filtering rules, and extension URIs.
 */
public interface ExporterProfileConfig {
    
    /**
     * Check if a field should be included in the export
     * @param fieldName the name of the field (e.g., "codes", "names", "relationships")
     * @return true if the field should be included
     */
    boolean shouldIncludeField(String fieldName);
    
    /**
     * Get the FHIR code system URI for a given GSRS code system
     * @param gsrsCodeSystem the GSRS code system identifier
     * @return FHIR code system URI, or null if not mapped
     */
    String mapCodeSystemToFhirUri(String gsrsCodeSystem);
    
    /**
     * Get FHIR extension URIs for profile-specific extensions
     * @return map of extension names to URIs (e.g., "dataClassification" -> "https://ema.europa.eu/fhir/dataClassification")
     */
    Map<String, String> getExtensionUris();
    
    /**
     * Get the base URI for GSRS-specific codes
     * @return base URI (e.g., "https://gsrs.ncats.nih.gov/fhir/")
     */
    String getGsrsBaseUri();
    
    /**
     * Get the code system URI for status values
     * @return code system URI
     */
    String getStatusCodeSystem();
    
    /**
     * Get the status code for a given status value
     * @param statusValue the status value (e.g., "Current", "Active", "Draft")
     * @return FHIR code
     */
    String getStatusCode(String statusValue);
    
    /**
     * Get the display value for a status code
     * @param statusValue the status value
     * @return display text
     */
    String getStatusDisplay(String statusValue);
    
    /**
     * Get the code system URI for category values
     * @return code system URI
     */
    String getCategoryCodeSystem();
    
    /**
     * Get the category code for a given category value
     * @param categoryValue the category value (e.g., "Chemical", "Protein")
     * @return FHIR code
     */
    String getCategoryCode(String categoryValue);
    
    /**
     * Get the display value for a category code
     * @param categoryValue the category value
     * @return display text
     */
    String getCategoryDisplay(String categoryValue);
    
    /**
     * Get the code system URI for domain values
     * @return code system URI
     */
    String getDomainCodeSystem();
    
    /**
     * Get the domain code for a given domain value
     * @param domainValue the domain value (e.g., "Human use", "Veterinary use")
     * @return FHIR code
     */
    String getDomainCode(String domainValue);
    
    /**
     * Get the display value for a domain code
     * @param domainValue the domain value
     * @return display text
     */
    String getDomainDisplay(String domainValue);
    
    /**
     * Get the code system URI for language codes
     * @return code system URI
     */
    String getLanguageCodeSystem();
    
    /**
     * Get the data classification code system URI
     * @return code system URI
     */
    String getDataClassificationCodeSystem();
    
    /**
     * Get the data classification code
     * @param classificationValue the classification value (e.g., "Public", "Confidential")
     * @return FHIR code
     */
    String getDataClassificationCode(String classificationValue);
    
    /**
     * Get the display value for a data classification code
     * @param classificationValue the classification value
     * @return display text
     */
    String getDataClassificationDisplay(String classificationValue);
}
