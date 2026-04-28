package gsrs.module.substance.exporters.profiles;

import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapperConfig;
import gsrs.module.substance.exporters.mappers.fhir.fieldmapper.*;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;

import java.util.Date;
import java.util.UUID;

/**
 * Abstract base implementation of ExporterProfile with common mapping logic.
 * 
 * Provides standard FHIR resource initialization, metadata setup, and field mapper integration.
 */
@Slf4j
public abstract class BaseExporterProfile implements ExporterProfile {
    
    protected ExporterProfileConfig config;
    
    public BaseExporterProfile(ExporterProfileConfig config) {
        this.config = config;
    }
    
    @Override
    public ExporterProfileConfig getConfig() {
        return config;
    }
    
    /**
     * Create a base SubstanceDefinition resource with common initialization
     * @param substance the source Substance
     * @return initialized SubstanceDefinition
     */
    protected SubstanceDefinition createBaseSubstanceDefinition(Substance substance) {
        SubstanceDefinition sd = new SubstanceDefinition();
        
        // Set basic metadata
        sd.setId(substance.getUuid() != null ? substance.getUuid() : UUID.randomUUID().toString());
        
        // Set status - delegate to profile config
        String statusCodeSystem = config.getStatusCodeSystem();
        if (statusCodeSystem != null) {
            CodeableConcept status = new CodeableConcept();
            String statusCode = config.getStatusCode("Current");
            String statusDisplay = config.getStatusDisplay("Current");
            status.addCoding()
                .setSystem(statusCodeSystem)
                .setCode(statusCode != null ? statusCode : "active")
                .setDisplay(statusDisplay != null ? statusDisplay : "Current");
            sd.setStatus(status);
        }
        
        // Set category - delegate to profile config
        String categoryCodeSystem = config.getCategoryCodeSystem();
        if (categoryCodeSystem != null) {
            CodeableConcept category = new CodeableConcept();
            String categoryCode = config.getCategoryCode("Chemical");
            String categoryDisplay = config.getCategoryDisplay("Chemical");
            category.addCoding()
                .setSystem(categoryCodeSystem)
                .setCode(categoryCode != null ? categoryCode : "Chemical")
                .setDisplay(categoryDisplay != null ? categoryDisplay : "Chemical");
            sd.setCategory(category);
        }
        
        // Set domain - delegate to profile config
        String domainCodeSystem = config.getDomainCodeSystem();
        if (domainCodeSystem != null) {
            CodeableConcept domain = new CodeableConcept();
            String domainCode = config.getDomainCode("Human use");
            String domainDisplay = config.getDomainDisplay("Human use");
            domain.addCoding()
                .setSystem(domainCodeSystem)
                .setCode(domainCode != null ? domainCode : "Human use")
                .setDisplay(domainDisplay != null ? domainDisplay : "Human use");
            sd.setDomain(domain);
        }
        
        // Set identifier
        Identifier identifier = new Identifier();
        identifier.setSystem("https://gsrs.ncats.nih.gov/substance");
        identifier.setValue(substance.getUuid() != null ? substance.getUuid() : UUID.randomUUID().toString());
        sd.addIdentifier(identifier);
        
        // Set metadata
        Meta meta = new Meta();
        meta.setLastUpdated(substance.getLastModified() != null ? 
            new Date(substance.getLastModified().getTime()) : new Date());
        sd.setMeta(meta);
        
        return sd;
    }
    
    /**
     * Map all substance fields using field mappers
     * @param sd the SubstanceDefinition to populate
     * @param substance the source Substance
     * @param mapperConfig the mapper configuration
     */
    protected void mapAllFields(SubstanceDefinition sd, Substance substance, 
                               SubstanceDefinitionMapperConfig mapperConfig) {
        // Map structure
        StructureMapper structureMapper = new StructureMapper(mapperConfig);
        structureMapper.mapStructure(sd, substance);
        
        // Map names
        NameMapper nameMapper = new NameMapper(mapperConfig);
        nameMapper.mapNames(sd, substance);
        
        // Map codes
        CodeMapper codeMapper = new CodeMapper(mapperConfig);
        codeMapper.mapCodes(sd, substance);
        
        // Map relationships
        RelationshipMapper relationshipMapper = new RelationshipMapper(mapperConfig);
        relationshipMapper.mapRelationships(sd, substance);
    }
    
    /**
     * Add extension to SubstanceDefinition
     * @param sd the SubstanceDefinition
     * @param url the extension URL
     * @param value the extension value (can be various types)
     */
    protected void addExtension(SubstanceDefinition sd, String url, Object value) {
        Extension ext = new Extension(url);
        
        if (value instanceof String) {
            ext.setValue(new StringType((String) value));
        } else if (value instanceof Boolean) {
            ext.setValue(new BooleanType((Boolean) value));
        } else if (value instanceof CodeableConcept) {
            ext.setValue((CodeableConcept) value);
        } else if (value instanceof Reference) {
            ext.setValue((Reference) value);
        } else {
            ext.setValue(new StringType(value.toString()));
        }
        
        sd.addExtension(ext);
    }
    
    /**
     * Add extension to a name component
     * @param nameComp the name component
     * @param url the extension URL
     * @param value the extension value
     */
    protected void addExtensionToName(SubstanceDefinition.SubstanceDefinitionNameComponent nameComp,
                                     String url, Object value) {
        Extension ext = new Extension(url);
        
        if (value instanceof CodeableConcept) {
            ext.setValue((CodeableConcept) value);
        } else {
            ext.setValue(new StringType(value != null ? value.toString() : ""));
        }
        
        nameComp.addExtension(ext);
    }
}
