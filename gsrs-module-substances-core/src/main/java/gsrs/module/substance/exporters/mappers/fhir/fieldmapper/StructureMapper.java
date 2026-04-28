package gsrs.module.substance.exporters.mappers.fhir.fieldmapper;

import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapper;
import gsrs.module.substance.exporters.mappers.fhir.SubstanceDefinitionMapperConfig;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;

/**
 * Maps Substance structure information to FHIR SubstanceDefinition.structure element.
 * 
 * Handles molecular formula, molecular weight, and chemical representations (SMILES, InChI, etc.).
 */
@Slf4j
public class StructureMapper extends SubstanceDefinitionMapper {
    
    public StructureMapper(SubstanceDefinitionMapperConfig config) {
        super(config);
    }
    
    /**
     * Map structure information from Substance to SubstanceDefinition
     * @param sd the SubstanceDefinition resource to populate
     * @param substance the source Substance
     */
    public void mapStructure(SubstanceDefinition sd, Substance substance) {
        if (!shouldIncludeField("structure") || substance == null) {
            return;
        }
        
        // Get the substance's structure
        Object substanceStructure = substance.getStructure();
        if (substanceStructure == null) {
            return;
        }
        
        SubstanceDefinition.SubstanceDefinitionStructureComponent structComp =
            new SubstanceDefinition.SubstanceDefinitionStructureComponent();
        
        // Map molecular formula if available
        try {
            // Try to extract molecular formula using reflection
            String molFormula = extractMolecularFormula(substance);
            if (!isEmpty(molFormula)) {
                structComp.setMolecularFormula(molFormula);
            }
        } catch (Exception e) {
            log.debug("Could not extract molecular formula: {}", e.getMessage());
        }
        
        // Map molecular weight if available
        try {
            String molWeight = extractMolecularWeight(substance);
            if (!isEmpty(molWeight)) {
                Quantity quantity = new Quantity();
                try {
                    quantity.setValue(Double.parseDouble(molWeight));
                    quantity.setUnit("g/mol");
                    quantity.setSystem("http://unitsofmeasure.org");
                    quantity.setCode("g/mol");
                    structComp.setMolecularWeight(quantity);
                } catch (NumberFormatException e) {
                    log.debug("Could not parse molecular weight as number: {}", molWeight);
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract molecular weight: {}", e.getMessage());
        }
        
        // Map chemical representations (SMILES, InChI, etc.)
        try {
            mapRepresentations(structComp, substance);
        } catch (Exception e) {
            log.debug("Could not map representations: {}", e.getMessage());
        }
        
        // Only add structure if it has content
        if (structComp.getMolecularFormula() != null ||
            structComp.getMolecularWeight() != null ||
            !structComp.getRepresentation().isEmpty()) {
            sd.setStructure(structComp);
        }
    }
    
    /**
     * Extract molecular formula from substance using reflection
     * @param substance the Substance
     * @return molecular formula or null
     */
    private String extractMolecularFormula(Substance substance) {
        try {
            Object structure = substance.getStructure();
            if (structure != null) {
                java.lang.reflect.Method method = structure.getClass().getMethod("getMolecularFormula");
                Object result = method.invoke(structure);
                return result != null ? result.toString() : null;
            }
        } catch (Exception e) {
            log.debug("Could not get molecular formula via reflection");
        }
        return null;
    }
    
    /**
     * Extract molecular weight from substance using reflection
     * @param substance the Substance
     * @return molecular weight or null
     */
    private String extractMolecularWeight(Substance substance) {
        try {
            Object structure = substance.getStructure();
            if (structure != null) {
                // Try different method names that might be used
                String[] methodNames = {"getMolecularWeight", "getMolWeight", "getWeight"};
                for (String methodName : methodNames) {
                    try {
                        java.lang.reflect.Method method = structure.getClass().getMethod(methodName);
                        Object result = method.invoke(structure);
                        if (result != null) {
                            return result.toString();
                        }
                    } catch (NoSuchMethodException ignored) {
                        // Try next method name
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not get molecular weight via reflection");
        }
        return null;
    }
    
    /**
     * Map chemical representations (SMILES, InChI, etc.)
     * @param structComp the structure component to populate
     * @param substance the source Substance
     */
    private void mapRepresentations(SubstanceDefinition.SubstanceDefinitionStructureComponent structComp,
                                    Substance substance) {
        try {
            Object structure = substance.getStructure();
            if (structure != null) {
                // Try to get chemical identifiers
                java.lang.reflect.Method method = structure.getClass().getMethod("getChemicalIdentifiers");
                Object result = method.invoke(structure);
                
                if (result instanceof java.util.List) {
                    java.util.List<?> identifiers = (java.util.List<?>) result;
                    for (Object identifier : identifiers) {
                        mapChemicalIdentifier(structComp, identifier);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not map representations: {}", e.getMessage());
        }
    }
    
    /**
     * Map a single chemical identifier to a representation
     * @param structComp the structure component
     * @param identifier the chemical identifier object
     */
    private void mapChemicalIdentifier(SubstanceDefinition.SubstanceDefinitionStructureComponent structComp,
                                      Object identifier) {
        try {
            // Try to extract representation type and value
            java.lang.reflect.Method typeMethod = identifier.getClass().getMethod("getType");
            java.lang.reflect.Method valueMethod = identifier.getClass().getMethod("getValue");
            
            Object type = typeMethod.invoke(identifier);
            Object value = valueMethod.invoke(identifier);
            
            if (type != null && value != null) {
                SubstanceDefinition.SubstanceDefinitionStructureRepresentationComponent rep =
                    new SubstanceDefinition.SubstanceDefinitionStructureRepresentationComponent();
                
                rep.setType(new CodeableConcept().setText(type.toString()));
                rep.setRepresentation(value.toString());
                
                structComp.addRepresentation(rep);
            }
        } catch (Exception e) {
            log.debug("Could not map chemical identifier");
        }
    }
    
    /**
     * Implementation of abstract map method (not used for field mappers)
     */
    @Override
    public SubstanceDefinition map(Substance substance) {
        // This mapper only handles the structure field, not the entire substance
        throw new UnsupportedOperationException("Use mapStructure() instead");
    }
}
