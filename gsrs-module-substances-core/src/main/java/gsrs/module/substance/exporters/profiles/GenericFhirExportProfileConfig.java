package gsrs.module.substance.exporters.profiles;

/**
 * Configuration for Generic FHIR export profile.
 * 
 * Provides standard FHIR ValueSet URIs for interoperability and future-proofing.
 * Uses http://hl7.org/fhir/ base URIs for all FHIR-defined code systems.
 */
public class GenericFhirExportProfileConfig extends BaseExporterProfileConfig {
    
    public GenericFhirExportProfileConfig() {
        super();
        initializeGenericConfiguration();
    }
    
    private void initializeGenericConfiguration() {
        // Use standard FHIR ValueSet URIs
        // Status uses FHIR publication-status ValueSet
        this.statusCodeSystem = "http://hl7.org/fhir/ValueSet/publication-status";
        addStatusMapping("Current", "active", "Active");
        addStatusMapping("Provisional", "draft", "Draft");
        addStatusMapping("Non-Current", "retired", "Retired");
        addStatusMapping("Nullified", "unknown", "Unknown");
        
        // Category - use FHIR substance category (general categories)
        this.categoryCodeSystem = "http://hl7.org/fhir/substance-category";
        addCategoryMapping("Chemical", "allergen", "Allergen");
        addCategoryMapping("Protein", "biologicalSubstance", "Biological Substance");
        addCategoryMapping("Polymer", "chemical", "Chemical");
        addCategoryMapping("Mixture", "chemical", "Chemical");
        addCategoryMapping("Nucleic acid", "biologicalSubstance", "Biological Substance");
        
        // Domain - use FHIR ValueSet for healthcare setting
        this.domainCodeSystem = "http://hl7.org/fhir/ValueSet/use-context";
        addDomainMapping("Human use", "clinical", "Clinical Care");
        addDomainMapping("Veterinary use", "research", "Veterinary Research");
        addDomainMapping("Human and Veterinary use", "clinical", "Clinical Care");
        
        // Language - use standard IETF language tag system  
        this.languageCodeSystem = "urn:ietf:bcp:47";
        
        // Data Classification - use standard FHIR security label classification
        this.dataClassificationCodeSystem = "http://hl7.org/fhir/ValueSet/security-labels";
        addDataClassificationMapping("Confidential", "U", "Unrestricted");
        addDataClassificationMapping("Public", "U", "Unrestricted");
        addDataClassificationMapping("Restricted", "R", "Restricted");
        
        // Configure code system mappings for standard code identifiers
        addCodeSystemMapping("CAS", "http://www.cas.org");
        addCodeSystemMapping("IUPAC", "https://www.iupac.org");
        addCodeSystemMapping("INCHI", "https://www.inchi-trust.org");
        addCodeSystemMapping("SMILES", "https://www.daylight.com/smiles");
        addCodeSystemMapping("PUBCHEM", "https://pubchem.ncbi.nlm.nih.gov");
        addCodeSystemMapping("UNII", "https://fdasis.nlm.nih.gov/srs");
        addCodeSystemMapping("WHO", "https://www.who.int/iczn");
        addCodeSystemMapping("RELATIONSHIP_TYPE", "http://hl7.org/fhir/relationship-type");
        
        // Use GSRS base URI for all GSRS-specific codes
        this.gsrsBaseUri = "https://gsrs.ncats.nih.gov/fhir/";
        
        // Include all standard fields
        includeField("names", "codes", "structure", "relationships", "properties");
        
        // No profile-specific extensions for generic profile
        // extensionUris remains empty for standard FHIR
    }
}
