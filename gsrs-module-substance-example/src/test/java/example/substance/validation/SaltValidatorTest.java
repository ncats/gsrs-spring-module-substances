package example.substance.validation;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.EnableGsrsApi;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.search.text.IndexValueMaker;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ProteinValidator;
import ix.ginas.utils.validation.validators.SaltValidator;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 *
 * @author mitch miller
 */
@EnableGsrsApi(indexValueMakerDetector = EnableGsrsApi.IndexValueMakerDetector.CONF)
@RecordApplicationEvents
@WithMockUser(username = "admin", roles = "Admin")
public class SaltValidatorTest extends AbstractSubstanceJpaEntityTest {

    public SaltValidatorTest() {
        System.out.println("SaltValidatorTest()");
                
    }

    private boolean setup = false;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @TestConfiguration
    public static class Configuration {

        @Bean
        public IndexValueMaker defHashIndexer() {
            return new SubstanceDefinitionalHashIndexer();
        }
    }

    @BeforeEach
    public void runSetup(@Autowired ApplicationEvents applicationEvents) throws IOException {
        System.out.println("runSetup");
        if (!setup) {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ProteinValidator.class);
            config.setNewObjClass(ProteinSubstance.class);

            factory.addValidator("substances", config);

        }
        File dataFile = new ClassPathResource("testdumps/rep18.gsrs").getFile();
        loadGsrsFile(dataFile);
        applicationEvents.notifyAll();
        setup = true;
        System.out.println("loaded rep18 data file");
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void findDuplicates1() {
        System.out.println("findDuplicates1");
        String approvalID = "660YQ98I10";
        ChemicalSubstance chemical = getChemicalFromFile(approvalID);
        SaltValidator validator = new SaltValidator();

        List<Substance> duplicates = validator.findDefinitionaLayer1lDuplicateCandidates(chemical);
        assertFalse(duplicates.isEmpty());
    }

    private ChemicalSubstance getChemicalFromFile(String name) {
        try {
            File chemicalFile = new ClassPathResource("testJSON/" + name + ".json").getFile();
            ChemicalSubstanceBuilder builder = SubstanceBuilder.from(chemicalFile);
            System.out.println("first name of read-in chem: " + builder.build().names.get(0).name);
            return builder.build();
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
