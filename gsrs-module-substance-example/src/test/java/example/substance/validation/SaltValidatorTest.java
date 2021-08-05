package example.substance.validation;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.module.substance.definitional.DefinitionalElement;
import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.SaltValidator;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

/**
 *
 * @author mitch miller
 */
//@RecordApplicationEvents
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class SaltValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {

    public SaltValidatorTest() {
    }

    private boolean setup = false;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired(required = true)
    private DefinitionalElementFactory definitionalElementFactory;

    @BeforeEach
    public void runSetup() throws IOException {
        System.out.println("runSetup");
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);

        //prevent validations from occurring multiple times
        if (!setup) {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(SaltValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
        }
        File dataFile = new ClassPathResource("testdumps/rep18.gsrs").getFile();
        loadGsrsFile(dataFile);
        //applicationEvents.notifyAll();
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
    public void TestFindDefHashDuplicates() {
        System.out.println("findDuplicates1");
        String approvalID = "660YQ98I10";
        ChemicalSubstance chemical = getChemicalFromFile(approvalID);
        DefinitionalElements newDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(chemical);
        DefinitionalElement elem = newDefinitionalElements.getElements().get(0);
        String msg = String.format("ID: %s; SMILES: %s; def hash layer 1: %s; value: %s", chemical.approvalID,
                chemical.getStructure().smiles, chemical.getDefinitionElement(), elem.getValue());
        System.out.println(msg);
        log.debug(msg);
        int layer = newDefinitionalElements.getDefinitionalHashLayers().size() - 1; // hashes.size()-1;
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "handling layer: " + (layer + 1));
        SaltValidator validator = new SaltValidator();

        List<Substance> duplicates = validator.findDefinitionaLayer1lDuplicateCandidates(chemical);
        System.out.println("duplicate list size: " + duplicates.size() + "; items: ");
        duplicates.forEach(s -> {
            ChemicalSubstance chem = (ChemicalSubstance) s;
            DefinitionalElements cDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(chem);
            String msg2 = String.format("ID: %s; SMILES: %s; def hash layer 1: %s; value: %s", chem.approvalID,
                    chem.getStructure().smiles, chem.getDefinitionElement(), cDefinitionalElements.getElements().get(0).getValue());
            System.out.println(msg2);
        });

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
