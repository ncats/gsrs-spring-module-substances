package example.substance.processor;

import gsrs.module.substance.processors.ConfigurableMolweightProcessor;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Property;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author mitch
 */
public class ConfigurableMolweightProcessorTest {

    @Test
    public void testSimpleMwCalc() {
        ConfigurableMolweightProcessor processor = processor("intrinsic", 2, null);
        ChemicalSubstance chemical = chemicalWithStructure("c1ccccc1");

        processor.prePersist(chemical);

        double expectedMw = 6 * 12.511 + 6 * 1.008;
        assertEquals(expectedMw, chemical.getStructure().mwt, 0.001,
                "Molecular weight calculation must be accurate");
    }

    @Test
    public void testSimpleMwCalc2() {
        ConfigurableMolweightProcessor processor = processor("intrinsic", 2, null);
        ChemicalSubstance chemical = chemicalWithStructure("c1c([81Br])cccc1");

        processor.prePersist(chemical);

        double expectedMw = 6 * 12.511 + 5 * 1.008 + 280.916290563;
        assertEquals(expectedMw, chemical.getStructure().mwt, 0.001,
                "Molecular weight calculation must be accurate");
    }

    @Test
    public void testSimpleMwCalcProperty() {
        String propertyName = "customMw";

        ConfigurableMolweightProcessor processor = processor("property", 3, propertyName);
        ChemicalSubstance chemical = chemicalWithStructure("c1ccccc1");

        processor.prePersist(chemical);
        Property mwProp = chemical.properties.stream()
                .filter(p -> p.getName().equals(propertyName))
                .findFirst().get();

        double expectedMw = 6 * 12.511 + 6 * 1.008;
        assertEquals(expectedMw, mwProp.getValue().average, 0.001,
                "Molecular weight calculation must be accurate");
    }

    @Test
    public void testComplexMwCalcProperty() {
        String propertyName = "OrganizationalMolecularWeight";

        ConfigurableMolweightProcessor processor = processor("property", 3, propertyName);
        ChemicalSubstance chemical = chemicalWithStructure("C[N+](C)(C)C.[O-]C(=O)c1ccc2ncccc2c1");

        processor.prePersist(chemical);
        Property mwProp = chemical.properties.stream()
                .filter(p -> p.getName().equals(propertyName))
                .findFirst().get();

        //C10H6NO2.C4H12N
        double expectedMw = (10+4) * 12.511 + (6+12) * 1.008 + 2*14.007 + 2*15.999;
        assertEquals(expectedMw, mwProp.getValue().average, 0.001,
                "Molecular weight calculation must be accurate");
    }

    private ConfigurableMolweightProcessor processor(String persistenceMode, int decimalDigits, String propertyName) {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("atomicWeights", atomicWeightEntries());
        configValues.put("persistenceMode", persistenceMode);
        configValues.put("decimalDigits", decimalDigits);
        if (propertyName != null) {
            configValues.put("propertyName", propertyName);
        }
        return new ConfigurableMolweightProcessor(configValues);
    }

    /**
     * The atomic weights that used to be supplied by the "atomicweightdataodd.csv" file, now
     * supplied inline through configuration.  Note that these deliberately differ from the real
     * values for carbon and bromine-81, matching the test data that the assertions expect.
     */
    private List<Map<String, Object>> atomicWeightEntries() {
        List<Map<String, Object>> entries = new ArrayList<>();
        entries.add(atomicWeightEntry("C", null, 12.511));
        entries.add(atomicWeightEntry("Br", 81, 280.916290563));
        return entries;
    }

    private Map<String, Object> atomicWeightEntry(String symbol, Integer massNumber, Double value) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("symbol", symbol);
        if (massNumber != null) {
            entry.put("massNumber", massNumber);
        }
        entry.put("value", value);
        return entry;
    }

    private ChemicalSubstance chemicalWithStructure(String structure) {
        return new ChemicalSubstanceBuilder()
                .addName("benzene")
                .setStructureWithDefaultReference(structure)
                .build();
    }
}
