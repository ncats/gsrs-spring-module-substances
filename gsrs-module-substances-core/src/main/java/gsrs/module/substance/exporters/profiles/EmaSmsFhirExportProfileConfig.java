package gsrs.module.substance.exporters.profiles;

/**
 * Configuration for EMA SMS FHIR export profile.
 * 
 * Implements EMA SPOR FHIR extensions and field filtering specific to EMA requirements.
 * All code system URIs and code mappings are sourced from rms.csv.
 */
public class EmaSmsFhirExportProfileConfig extends BaseExporterProfileConfig {
    
    public EmaSmsFhirExportProfileConfig() {
        super();
        initializeEmaConfiguration();
    }
    
    private void initializeEmaConfiguration() {
        // Add EMA-specific extensions
        addExtension("currentSubstance", "https://ema.europa.eu/fhir/currentSubstance");
        addExtension("dataClassification", "https://ema.europa.eu/fhir/dataClassification");
        addExtension("substanceRole", "https://ema.europa.eu/fhir/substanceRole");
        
        // Configure EMA code system URIs (from rms.csv)
        // Status (from rms.csv LIST 200000005003)
        this.statusCodeSystem = "https://spor.ema.europa.eu/v1/lists/200000005003";
        addStatusMapping("Current", "200000005004", "Current");
        addStatusMapping("Provisional", "200000005005", "Provisional");
        addStatusMapping("Non-Current", "200000005006", "Non-Current");
        addStatusMapping("Nullified", "200000005007", "Nullified");
        
        // Category (from rms.csv LIST 100000075826)
        this.categoryCodeSystem = "https://spor.ema.europa.eu/v1/lists/100000075826";
        addCategoryMapping("Chemical", "100000075670", "Chemical");
        addCategoryMapping("Protein", "200000005020", "Protein");
        addCategoryMapping("Polymer", "200000005022", "Polymer");
        addCategoryMapping("Mixture", "200000005023", "Mixture");
        addCategoryMapping("Structurally Diverse - Plasma derived", "200000005024", "Structurally Diverse - Plasma derived");
        addCategoryMapping("Structurally Diverse - Herbal", "200000005025", "Structurally Diverse - Herbal");
        addCategoryMapping("Structurally Diverse - Allergen", "200000005026", "Structurally Diverse - Allergen");
        addCategoryMapping("Structurally Diverse - Vaccine", "200000005027", "Structurally Diverse - Vaccine");
        addCategoryMapping("Structurally Diverse - Cell therapy", "200000005029", "Structurally Diverse - Cell therapy");
        addCategoryMapping("Structurally Diverse - Other", "200000005030", "Structurally Diverse - Other");
        addCategoryMapping("Specified Substance Group 1", "200000005031", "Specified Substance Group 1");
        addCategoryMapping("Specified Substance Group 2", "200000005032", "Specified Substance Group 2");
        addCategoryMapping("Specified Substance Group 3", "200000005033", "Specified Substance Group 3");
        addCategoryMapping("Specified Substance Group 4", "200000005034", "Specified Substance Group 4");
        addCategoryMapping("Nucleic acid", "200000005035", "Nucleic acid");
        
        // Domain (from rms.csv LIST 100000000004)
        this.domainCodeSystem = "https://spor.ema.europa.eu/v1/lists/100000000004";
        addDomainMapping("Human use", "100000000012", "Human use");
        addDomainMapping("Veterinary use", "100000000013", "Veterinary use");
        addDomainMapping("Human and Veterinary use", "100000000014", "Human and Veterinary use");
        
        // Language (from rms.csv LIST 100000072057)
        this.languageCodeSystem = "https://spor.ema.europa.eu/v1/lists/100000072057";
        
        // Data Classification (from rms.csv LIST 200000004983)
        this.dataClassificationCodeSystem = "https://spor.ema.europa.eu/v1/lists/200000004983";
        addDataClassificationMapping("Confidential", "200000004984", "Confidential");
        addDataClassificationMapping("Public", "200000004985", "Public");
        addDataClassificationMapping("Restricted", "200000004986", "Restricted");
        
        // Configure code system mappings for other EMA identifiers
        // InChI Key (SMILES CACTUS)
        addCodeSystemMapping("INCHIKEY", "https://spor.ema.europa.eu/v1/lists/100000000009/terms/200000018817");
        
        // CAS Registry Number
        addCodeSystemMapping("CAS", "https://spor.ema.europa.eu/v1/lists/100000000009/terms/100000075787");
        
        // EMA code (EMA/CPMS)
        addCodeSystemMapping("EMA", "https://spor.ema.europa.eu/v1/lists/100000000009/terms/100000146035");
        
        // EFSA code (EFSA)
        addCodeSystemMapping("EFSA", "https://spor.ema.europa.eu/v1/lists/100000000009/terms/200000032418");
        
        // WHO code
        addCodeSystemMapping("WHO", "https://spor.ema.europa.eu/v1/lists/100000000009/terms/100000075715");
        
        // EC number (EINECS)
        addCodeSystemMapping("EC_NUMBER", "https://spor.ema.europa.eu/v1/lists/100000000009/terms/200000032418");
        
        // Relationship type (EMA SPOR)
        addCodeSystemMapping("RELATIONSHIP_TYPE", "https://spor.ema.europa.eu/v1/lists/200000004946");
        
        // Include all fields by default, EMA can filter as needed
        includeField("names", "codes", "structure", "relationships", "properties");
    }
}
