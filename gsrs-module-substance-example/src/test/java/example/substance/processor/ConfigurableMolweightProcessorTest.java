package example.substance.processor;

import gsrs.module.substance.processors.ConfigurableMolweightProcessor;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Property;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author mitch
 */
public class ConfigurableMolweightProcessorTest {

    private static final String ATOMIC_WEIGHT_DATA_FILE = "atomicweightdataodd.csv";

    @Test
    public void testSimpleMwCalc() throws IOException {
        ConfigurableMolweightProcessor processor = processor("intrinsic", 2, null);
        ChemicalSubstance chemical = chemicalWithStructure("c1ccccc1");

        processor.prePersist(chemical);

        double expectedMw = 6 * 12.511 + 6 * 1.008;
        assertEquals(expectedMw, chemical.getStructure().mwt, 0.001,
                "Molecular weight calculation must be accurate");
    }

    @Test
    public void testSimpleMwCalc2() throws IOException {
        ConfigurableMolweightProcessor processor = processor("intrinsic", 2, null);
        ChemicalSubstance chemical = chemicalWithStructure("c1c([81Br])cccc1");

        processor.prePersist(chemical);

        double expectedMw = 6 * 12.511 + 5 * 1.008 + 280.916290563;
        assertEquals(expectedMw, chemical.getStructure().mwt, 0.001,
                "Molecular weight calculation must be accurate");
    }

    @Test
    public void testSimpleMwCalcProperty() throws IOException {
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
    public void testComplexMwCalcProperty() throws IOException {
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

    private ConfigurableMolweightProcessor processor(String persistenceMode, int decimalDigits, String propertyName)
            throws IOException {
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("atomWeightFilePath", atomicWeightFilePath());
        configValues.put("persistenceMode", persistenceMode);
        configValues.put("decimalDigits", decimalDigits);
        if (propertyName != null) {
            configValues.put("propertyName", propertyName);
        }
        return new ConfigurableMolweightProcessor(configValues);
    }

    private String atomicWeightFilePath() throws IOException {
        return new ClassPathResource(ATOMIC_WEIGHT_DATA_FILE).getFile().getAbsolutePath();
    }

    private ChemicalSubstance chemicalWithStructure(String structure) {
        return new ChemicalSubstanceBuilder()
                .addName("benzene")
                .setStructureWithDefaultReference(structure)
                .build();
    }
}
