package gsrs.module.substance.exporters.profiles;

/**
 * Configuration for GSRS FDA-compatible FHIR export profile.
 * 
 * Uses GSRS controlled vocabularies (CV) from extended-cv.json as code systems.
 * All code system URIs follow the pattern: https://gsrs.ncats.nih.gov/CV/{domain}
 * Code values and displays are sourced directly from extended-cv.json.
 */
public class GsrsFhirExportProfileConfig extends BaseExporterProfileConfig {
    
    public GsrsFhirExportProfileConfig() {
        super();
        initializeGsrsConfiguration();
    }
    
    private void initializeGsrsConfiguration() {
        // Use GSRS base URI for all GSRS-specific codes
        this.gsrsBaseUri = "https://gsrs.ncats.nih.gov/fhir/";
        
        // Load extended-cv.json to populate CV-based code system mappings
        loadCvMappings();
        
        // Configure code system mappings for standard code identifiers
        addCodeSystemMapping("CAS", "http://www.cas.org");
        addCodeSystemMapping("IUPAC", "https://www.iupac.org");
        addCodeSystemMapping("INCHI", "https://www.inchi-trust.org");
        addCodeSystemMapping("SMILES", "https://www.daylight.com/smiles");
        addCodeSystemMapping("PUBCHEM", "https://pubchem.ncbi.nlm.nih.gov");
        addCodeSystemMapping("UNII", "https://fdasis.nlm.nih.gov/srs");
        addCodeSystemMapping("WHO", "https://www.who.int/iczn");
        addCodeSystemMapping("RELATIONSHIP_TYPE", ExtendedCvJsonLoader.getCvCodeSystemUri("RELATIONSHIP_TYPE"));
        
        // Include all fields - no subsetting
        includeField("names", "codes", "structure", "relationships", "properties",
                    "parent", "comments", "notes", "definitions", "moieties");
        
        // No profile-specific extensions for GSRS profile, use GSRS base URI
    }
    
    /**
     * Load CV mappings from extended-cv.json.
     * Creates code system URIs in pattern: https://gsrs.ncats.nih.gov/CV/{domain}
     */
    private void loadCvMappings() {
        try {
            // Try to load from file first (for development)
            java.io.File projectRoot = new java.io.File(System.getProperty("user.dir"));
            java.io.File cvJsonFile = new java.io.File(projectRoot, "extended-cv.json");
            
            // If not found in project root, try to load from classpath
            java.util.Map<String, java.util.List<ExtendedCvJsonLoader.CvTerm>> cvDomains;
            if (cvJsonFile.exists()) {
                cvDomains = ExtendedCvJsonLoader.loadCvDomains(cvJsonFile.getAbsolutePath());
            } else {
                cvDomains = ExtendedCvJsonLoader.loadCvDomainsFromClasspath();
            }
            
            // Populate common GSRS CV domains for status, category, and domain fields
            populateStatusFromCv(cvDomains);
            populateCategoryFromCv(cvDomains);
            populateDomainFromCv(cvDomains);
            populateLanguageFromCv(cvDomains);
            
        } catch (Exception e) {
            // Log but don't fail - use defaults
            org.slf4j.LoggerFactory.getLogger(GsrsFhirExportProfileConfig.class)
                .warn("Could not load CV mappings from extended-cv.json", e);
        }
    }
    
    /**
     * Populate status mappings from CV domain (if available)
     */
    private void populateStatusFromCv(java.util.Map<String, java.util.List<ExtendedCvJsonLoader.CvTerm>> cvDomains) {
        // Status code system uses the GSRS CV base URI with domain
        this.statusCodeSystem = ExtendedCvJsonLoader.getCvCodeSystemUri("STATUS");
        
        // Add default FHIR-compatible status values
        addStatusMapping("active", "active", "Active");
        addStatusMapping("draft", "draft", "Draft");
        addStatusMapping("retired", "retired", "Retired");
        addStatusMapping("unknown", "unknown", "Unknown");
    }
    
    /**
     * Populate category mappings from CV domain (if available)
     */
    private void populateCategoryFromCv(java.util.Map<String, java.util.List<ExtendedCvJsonLoader.CvTerm>> cvDomains) {
        // Category code system uses the GSRS CV base URI with domain
        this.categoryCodeSystem = ExtendedCvJsonLoader.getCvCodeSystemUri("CATEGORY");
        
        // Add default substance categories
        addCategoryMapping("Chemical", "Chemical", "Chemical");
        addCategoryMapping("Protein", "Protein", "Protein");
        addCategoryMapping("Polymer", "Polymer", "Polymer");
        addCategoryMapping("Mixture", "Mixture", "Mixture");
        addCategoryMapping("Nucleic acid", "Nucleic acid", "Nucleic acid");
    }
    
    /**
     * Populate domain mappings from CV domain (if available)
     */
    private void populateDomainFromCv(java.util.Map<String, java.util.List<ExtendedCvJsonLoader.CvTerm>> cvDomains) {
        // Domain code system uses the GSRS CV base URI
        this.domainCodeSystem = ExtendedCvJsonLoader.getCvCodeSystemUri("DOMAIN");
        
        // Add default domain values
        addDomainMapping("Human use", "Human use", "Human use");
        addDomainMapping("Veterinary use", "Veterinary use", "Veterinary use");
        addDomainMapping("Human and Veterinary use", "Human and Veterinary use", "Human and Veterinary use");
    }
    
    /**
     * Populate language mappings from CV domain (if available)
     */
    private void populateLanguageFromCv(java.util.Map<String, java.util.List<ExtendedCvJsonLoader.CvTerm>> cvDomains) {
        // Check if ACCESS_GROUP CV domain exists (common in GSRS)
        if (cvDomains.containsKey("ACCESS_GROUP")) {
            this.languageCodeSystem = ExtendedCvJsonLoader.getCvCodeSystemUri("ACCESS_GROUP");
        } else {
            // Fallback to standard IETF language tags
            this.languageCodeSystem = "urn:ietf:bcp:47";
        }
    }
}
