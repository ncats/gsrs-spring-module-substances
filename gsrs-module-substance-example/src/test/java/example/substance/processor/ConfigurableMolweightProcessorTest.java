package example.substance.processor;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.module.substance.processors.ConfigurableMolweightProcessor;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Property;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author mitch
 */
public class ConfigurableMolweightProcessorTest extends AbstractSubstanceJpaEntityTest {

    @Test
    public void testSimpleMwCalc() throws IOException {
        //use an atomic weight data file with idiosyncratic values so we can confirm that the file values were used
        // rather than the defauult atomic weights
        String atomicWeightFilePath = new ClassPathResource("atomicweightdataodd.csv").getFile().getAbsolutePath();

        Map configValues = new HashMap();
        configValues.put("atomWeightFilePath", atomicWeightFilePath);
        configValues.put("persistanceMode", "intrinsic");
        configValues.put("decimalDigits", 2);
        ConfigurableMolweightProcessor processor = new ConfigurableMolweightProcessor(configValues);
        //construct a simple chemical
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("benzene")
                .setStructureWithDefaultReference("c1ccccc1");
        ChemicalSubstance chemical = builder.build();
        processor.prePersist(chemical);

        double expectedMw = 6 * 12.511 + 6 * 1.008;
        Assertions.assertEquals(expectedMw, chemical.getStructure().mwt, 0.001, "Molecular weight calculation must be accurate");
    }

    @Test
    public void testSimpleMwCalc2() throws IOException {
        //use an atomic weight data file with idiosyncratic values so we can confirm that the file values were used
        // rather than the defauult atomic weights
        String atomicWeightFilePath = new ClassPathResource("atomicweightdataodd.csv").getFile().getAbsolutePath();

        Map configValues = new HashMap();
        configValues.put("atomWeightFilePath", atomicWeightFilePath);
        configValues.put("persistanceMode", "intrinsic");
        configValues.put("decimalDigits", 2);
        ConfigurableMolweightProcessor processor = new ConfigurableMolweightProcessor(configValues);
        //construct a simple chemical
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("benzene")
                .setStructureWithDefaultReference("c1c([81Br])cccc1");
        ChemicalSubstance chemical = builder.build();
        processor.prePersist(chemical);

        double expectedMw = 6 * 12.511 + 5 * 1.008 + 280.916290563;
        Assertions.assertEquals(expectedMw, chemical.getStructure().mwt, 0.001, "Molecular weight calculation must be accurate");
    }

    @Test
    public void testSimpleMwCalcProperty() throws IOException {
        //use an atomic weight data file with idiosyncratic values so we can confirm that the file values were used
        // rather than the defauult atomic weights
        String atomicWeightFilePath = new ClassPathResource("atomicweightdataodd.csv").getFile().getAbsolutePath();
        String propertyName = "customMw";

        Map configValues = new HashMap();
        configValues.put("atomWeightFilePath", atomicWeightFilePath);
        configValues.put("persistanceMode", "property");
        configValues.put("decimalDigits", 3);
        configValues.put("propertyName", propertyName);
        ConfigurableMolweightProcessor processor = new ConfigurableMolweightProcessor(configValues);
        //construct a simple chemical
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("benzene")
                .setStructureWithDefaultReference("c1ccccc1");
        ChemicalSubstance chemical = builder.build();
        processor.prePersist(chemical);
        Property mwProp = chemical.properties.stream()
                .filter(p -> p.getName().equals(propertyName))
                .findFirst().get();

        double expectedMw = 6 * 12.511 + 6 * 1.008;
        Assertions.assertEquals(expectedMw, mwProp.getValue().average, 0.001, "Molecular weight calculation must be accurate");
    }

    @Test
    public void testComplexMwCalcProperty() throws IOException {
        //use an atomic weight data file with idiosyncratic values so we can confirm that the file values were used
        // rather than the defauult atomic weights
        String atomicWeightFilePath = new ClassPathResource("atomicweightdataodd.csv").getFile().getAbsolutePath();
        String propertyName = "OrganizationalMolecularWeight";

        Map configValues = new HashMap();
        configValues.put("atomWeightFilePath", atomicWeightFilePath);
        configValues.put("persistanceMode", "property");
        configValues.put("decimalDigits", 3);
        configValues.put("propertyName", propertyName);
        ConfigurableMolweightProcessor processor = new ConfigurableMolweightProcessor(configValues);
        //construct a simple chemical
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("benzene")
                .setStructureWithDefaultReference("C[N+](C)(C)C.[O-]C(=O)c1ccc2ncccc2c1");
        ChemicalSubstance chemical = builder.build();
        processor.prePersist(chemical);
        Property mwProp = chemical.properties.stream()
                .filter(p -> p.getName().equals(propertyName))
                .findFirst().get();

        //C10H6NO2.C4H12N
        double expectedMw = (10+4) * 12.511 + (6+12) * 1.008 + 2*14.007 + 2*15.999;
        Assertions.assertEquals(expectedMw, mwProp.getValue().average, 0.001, "Molecular weight calculation must be accurate");
    }
}
