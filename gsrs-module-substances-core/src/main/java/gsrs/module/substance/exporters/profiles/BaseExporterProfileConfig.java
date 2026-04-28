package gsrs.module.substance.exporters.profiles;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hl7.fhir.r4.model.Coding;

import java.util.*;

/**
 * Abstract base implementation of ExporterProfileConfig with common configuration
 */
@Data
@NoArgsConstructor
public abstract class BaseExporterProfileConfig implements ExporterProfileConfig {
    
    protected Map<String, String> extensionUris = new HashMap<>();
    protected Set<String> includedFields = new HashSet<>();
    protected Map<String, String> codeSystemMappings = new HashMap<>();
    protected Map<String, Map<String, String>> statusMappings = new HashMap<>();
    protected Map<String, Map<String, String>> categoryMappings = new HashMap<>();
    protected Map<String, Map<String, String>> domainMappings = new HashMap<>();
    protected Map<String, Map<String, String>> dataClassificationMappings = new HashMap<>();
    protected String gsrsBaseUri = "https://gsrs.ncats.nih.gov/fhir/";
    protected String statusCodeSystem;
    protected String categoryCodeSystem;
    protected String domainCodeSystem;
    protected String languageCodeSystem;
    protected String dataClassificationCodeSystem;
    
    /**
     * Default fields to include
     */
    public BaseExporterProfileConfig() {
        initializeDefaults();
    }
    
    protected void initializeDefaults() {
        // Include common fields by default
        includedFields.addAll(Arrays.asList(
            "names", "codes", "structure", "relationships", "properties"
        ));
        
        // Initialize GSRS base URI
        this.gsrsBaseUri = "https://gsrs.ncats.nih.gov/fhir/";
    }
    
    @Override
    public boolean shouldIncludeField(String fieldName) {
        return includedFields.contains(fieldName);
    }
    
    @Override
    public String mapCodeSystemToFhirUri(String gsrsCodeSystem) {
        if (codeSystemMappings.containsKey(gsrsCodeSystem)) {
            return codeSystemMappings.get(gsrsCodeSystem);
        }
        // Return null to use default GSRS base URI + code system
        return null;
    }
    
    @Override
    public Map<String, String> getExtensionUris() {
        return Collections.unmodifiableMap(extensionUris);
    }
    
    @Override
    public String getGsrsBaseUri() {
        return gsrsBaseUri;
    }
    
    @Override
    public String getStatusCodeSystem() {
        return statusCodeSystem;
    }
    
    @Override
    public String getStatusCode(String statusValue) {
        if (statusValue == null || !statusMappings.containsKey(statusValue)) {
            return null;
        }
        return statusMappings.get(statusValue).get("code");
    }
    
    @Override
    public String getStatusDisplay(String statusValue) {
        if (statusValue == null || !statusMappings.containsKey(statusValue)) {
            return statusValue;
        }
        return statusMappings.get(statusValue).get("display");
    }
    
    @Override
    public String getCategoryCodeSystem() {
        return categoryCodeSystem;
    }
    
    @Override
    public String getCategoryCode(String categoryValue) {
        if (categoryValue == null || !categoryMappings.containsKey(categoryValue)) {
            return null;
        }
        return categoryMappings.get(categoryValue).get("code");
    }
    
    @Override
    public String getCategoryDisplay(String categoryValue) {
        if (categoryValue == null || !categoryMappings.containsKey(categoryValue)) {
            return categoryValue;
        }
        return categoryMappings.get(categoryValue).get("display");
    }
    
    @Override
    public String getDomainCodeSystem() {
        return domainCodeSystem;
    }
    
    @Override
    public String getDomainCode(String domainValue) {
        if (domainValue == null || !domainMappings.containsKey(domainValue)) {
            return null;
        }
        return domainMappings.get(domainValue).get("code");
    }
    
    @Override
    public String getDomainDisplay(String domainValue) {
        if (domainValue == null || !domainMappings.containsKey(domainValue)) {
            return domainValue;
        }
        return domainMappings.get(domainValue).get("display");
    }
    
    @Override
    public String getLanguageCodeSystem() {
        return languageCodeSystem;
    }
    
    @Override
    public String getDataClassificationCodeSystem() {
        return dataClassificationCodeSystem;
    }
    
    @Override
    public String getDataClassificationCode(String classificationValue) {
        if (classificationValue == null || !dataClassificationMappings.containsKey(classificationValue)) {
            return null;
        }
        return dataClassificationMappings.get(classificationValue).get("code");
    }
    
    @Override
    public String getDataClassificationDisplay(String classificationValue) {
        if (classificationValue == null || !dataClassificationMappings.containsKey(classificationValue)) {
            return classificationValue;
        }
        return dataClassificationMappings.get(classificationValue).get("display");
    }
    
    protected void addExtension(String name, String uri) {
        extensionUris.put(name, uri);
    }
    
    protected void addCodeSystemMapping(String gsrsSystem, String fhirUri) {
        codeSystemMappings.put(gsrsSystem, fhirUri);
    }
    
    protected void includeField(String... fields) {
        includedFields.addAll(Arrays.asList(fields));
    }
    
    protected void excludeField(String... fields) {
        for (String field : fields) {
            includedFields.remove(field);
        }
    }
    
    /**
     * Helper method to add a status mapping
     */
    protected void addStatusMapping(String statusValue, String code, String display) {
        if (!statusMappings.containsKey(statusValue)) {
            statusMappings.put(statusValue, new HashMap<>());
        }
        statusMappings.get(statusValue).put("code", code);
        statusMappings.get(statusValue).put("display", display);
    }
    
    /**
     * Helper method to add a category mapping
     */
    protected void addCategoryMapping(String categoryValue, String code, String display) {
        if (!categoryMappings.containsKey(categoryValue)) {
            categoryMappings.put(categoryValue, new HashMap<>());
        }
        categoryMappings.get(categoryValue).put("code", code);
        categoryMappings.get(categoryValue).put("display", display);
    }
    
    /**
     * Helper method to add a domain mapping
     */
    protected void addDomainMapping(String domainValue, String code, String display) {
        if (!domainMappings.containsKey(domainValue)) {
            domainMappings.put(domainValue, new HashMap<>());
        }
        domainMappings.get(domainValue).put("code", code);
        domainMappings.get(domainValue).put("display", display);
    }
    
    /**
     * Helper method to add a data classification mapping
     */
    protected void addDataClassificationMapping(String classificationValue, String code, String display) {
        if (!dataClassificationMappings.containsKey(classificationValue)) {
            dataClassificationMappings.put(classificationValue, new HashMap<>());
        }
        dataClassificationMappings.get(classificationValue).put("code", code);
        dataClassificationMappings.get(classificationValue).put("display", display);
    }
}
