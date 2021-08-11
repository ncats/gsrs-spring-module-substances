package example.substance.validation;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
import ix.core.chem.StructureStandardizer;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import ix.ginas.utils.validation.validators.SaltValidator;
import java.io.File;
import java.io.IOException;
import java.util.List;
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
 * Test several cases of the SaltValidator
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

    @Autowired
    StructureProcessor structureProcessor;

    @Autowired
    StructureStandardizer standardizer;

    @BeforeEach
    public void runSetup() throws IOException {
        log.trace("runSetup");
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);

        //prevent validations from occurring multiple times
        if (!setup) {
            ValidatorConfig configChemValidator = new DefaultValidatorConfig();
            configChemValidator.setValidatorClass(ChemicalValidator.class);
            configChemValidator.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", configChemValidator);
            ValidatorConfig configSaltValidator = new DefaultValidatorConfig();
            configSaltValidator.setValidatorClass(SaltValidator.class);
            configSaltValidator.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", configSaltValidator);
        }
        File dataFile = new ClassPathResource("testdumps/rep18.gsrs").getFile();
        loadGsrsFile(dataFile);
        log.trace("loaded rep18 data file");
//        log.debug("starting fragments");
//        File dataFile2 = new ClassPathResource("testdumps/fragments.gsrs").getFile();
//        loadGsrsFile(dataFile2);
//        log.debug("finished fragments");
        //applicationEvents.notifyAll();
        setup = true;
        
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
    public void testFindLayer1DefHashDuplicates() {
        log.trace("testFindLayer1DefHashDuplicates");
        String approvalID = "660YQ98I10";
        ChemicalSubstance chemical = getChemicalFromFile(approvalID);
        SaltValidator validator = new SaltValidator();
        List<Substance> duplicates = validator.findDefinitionaLayer1lDuplicateCandidates(chemical);
        log.trace("duplicate list size: " + duplicates.size() + "; items: ");
        duplicates.forEach(s -> {
            ChemicalSubstance chem = (ChemicalSubstance) s;
            DefinitionalElements cDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(chem);
            String msg2 = String.format("ID: %s; SMILES: %s; def hash layer 1: %s; value: %s", chem.approvalID,
                    chem.getStructure().smiles, chem.getDefinitionElement(), cDefinitionalElements.getElements().get(0).getValue());
            log.trace(msg2);
        });

        assertEquals(1, duplicates.size());
    }

    @Test
    public void testFindFullDefHashDuplicates() {
        log.trace("TestFindFullDefHashDuplicates");
        String approvalID = "660YQ98I10";
        ChemicalSubstance chemical = getChemicalFromFile(approvalID);
        SaltValidator validator = new SaltValidator();
        List<Substance> duplicates = validator.findFullDefinitionalDuplicateCandidates(chemical);
        /*log.trace("duplicate list size: " + duplicates.size() + "; items: ");
        duplicates.forEach(s -> {
            ChemicalSubstance chem = (ChemicalSubstance) s;
            DefinitionalElements cDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(chem);
            String msg2 = String.format("ID: %s; SMILES: %s; def hash layer 1: %s; value: %s", chem.approvalID,
                    chem.getStructure().smiles, chem.getDefinitionElement(), cDefinitionalElements.getElements().get(0).getValue());
            log.trace(msg2);
        });*/

        assertEquals(1, duplicates.size());
    }

    /*
    Using the diastereomer of a structure in the loaded dataset, we expect to find 
    1 layer-1 duplicate
    0 full duplicates
     */
    @Test
    public void testFindLayer1NotFullDefHashDuplicates() {
        log.trace("findDuplicates1");
        String approvalID = "G6867RWN6N.diastereomer";
        ChemicalSubstance chemical = getChemicalFromFile(approvalID);
        SaltValidator validator = new SaltValidator();
        List<Substance> duplicates = validator.findDefinitionaLayer1lDuplicateCandidates(chemical);
        /*log.trace("duplicate list size: " + duplicates.size() + "; items: ");
        duplicates.forEach(s -> {
            ChemicalSubstance chem = (ChemicalSubstance) s;
            DefinitionalElements cDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(chem);
            String msg2 = String.format("ID: %s; SMILES: %s; def hash layer 1: %s; value: %s", chem.approvalID,
                    chem.getStructure().smiles, chem.getDefinitionElement(), cDefinitionalElements.getElements().get(0).getValue());
            log.trace(msg2);
        });*/
        

        assertEquals(1, duplicates.size());
        List<Substance> fullDuplicates = validator.findFullDefinitionalDuplicateCandidates(chemical);
        /*log.trace("full duplicate list size: " + fullDuplicates.size() + "; items: ");
        duplicates.forEach(s -> {
            ChemicalSubstance chem = (ChemicalSubstance) s;
            DefinitionalElements cDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(chem);
            String msg2 = String.format("ID: %s; SMILES: %s; def hash layer 1: %s; value: %s", chem.approvalID,
                    chem.getStructure().smiles, chem.getDefinitionElement(), cDefinitionalElements.getElements().get(0).getValue());
            log.trace(msg2);
        });*/
        assertTrue(fullDuplicates.isEmpty());
    }

    /*
    Using a structure not present in any form within the loaded dataset, we expect to find 
    0 layer-1 duplicates
    0 full duplicates
     */
    @Test
    public void testFin0DefHashDuplicates() {
        log.trace("testFin0DefHashDuplicates");
        String approvalID = "PJY633525U";
        ChemicalSubstance chemical = getChemicalFromFile(approvalID);

        SaltValidator validator = new SaltValidator();
        List<Substance> duplicates = validator.findDefinitionaLayer1lDuplicateCandidates(chemical);
        /*log.trace("duplicate list size: " + duplicates.size() + "; items: ");
        duplicates.forEach(s -> {
            ChemicalSubstance chem = (ChemicalSubstance) s;
            DefinitionalElements cDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(chem);
            String msg2 = String.format("ID: %s; SMILES: %s; def hash layer 1: %s; value: %s", chem.approvalID,
                    chem.getStructure().smiles, chem.getDefinitionElement(), cDefinitionalElements.getElements().get(0).getValue());
            log.trace(msg2);
        });*/
        assertTrue(duplicates.isEmpty());
        List<Substance> fullDuplicates = validator.findFullDefinitionalDuplicateCandidates(chemical);
        /*log.trace("full duplicate list size: " + fullDuplicates.size() + "; items: ");
        duplicates.forEach(s -> {
            ChemicalSubstance chem = (ChemicalSubstance) s;
            DefinitionalElements cDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(chem);
            String msg2 = String.format("ID: %s; SMILES: %s; def hash layer 1: %s; value: %s", chem.approvalID,
                    chem.getStructure().smiles, chem.getDefinitionElement(), cDefinitionalElements.getElements().get(0).getValue());
            log.trace(msg2);
        });*/
        assertTrue(fullDuplicates.isEmpty());
    }

    @Test
    public void testValidateSaltFragment() {
        log.trace("testValidateSaltFragment");
        String approvalID = "chemical other salt";
        ChemicalSubstance chemical = getChemicalFromFile(approvalID);

        SaltValidator validator = new SaltValidator();
        ValidationResponse response = validator.validate(chemical, null);

        assertTrue(response.getValidationMessages().stream().anyMatch(
                m
                -> ((ValidationMessage) m).getMessage().contains("Each fragment should be present as a separate record in the database.")));
    }

    /*
    Load a JSON file of a chemical substance whose structure consists of 2 moieties, each of which is already present
     */
    @Test
    public void testValidateSaltFragments() throws IOException {
        log.trace("testValidateSaltFragments");
         //load 2 substances that match the 2 fragments not in the 18-record set already loaded
        log.debug("starting fragments");
        File dataFile = new ClassPathResource("testdumps/fragments.txt").getFile();
        loadGsrsFile(dataFile);
        log.debug("finished fragments");

        String substanceName = "chemical salt";
        ChemicalSubstance chemical = getChemicalFromFile(substanceName);
        //some debug info
        String msg = String.format("newly read-in chemical substance %s; formula: %s; smiles: %s", chemical.names.get(0).name, 
            chemical.getStructure().formula, chemical.getStructure().smiles);
        log.debug(msg);
        SaltValidator validator = new SaltValidator();
        ValidationResponse response = validator.validate(chemical, null);

        response.getValidationMessages().forEach(vm->log.trace("message: " 
                + ((ValidationMessage)vm).getMessage() + "; type "+ ((ValidationMessage)vm).getMessageType()));
        assertTrue(response.getValidationMessages().stream().noneMatch(
                m
                -> ((ValidationMessage) m).getMessage().contains("Each fragment should be present as a separate record in the database.")));
        assertTrue(response.getValidationMessages().stream().noneMatch(
                m
                -> ((ValidationMessage) m).getMessage().contains("This fragment is present as a separate record in the database but in a different form.")));
    }

    @Test
    public void testValidateSalt() {
        log.trace("testValidateSalt");
        String approvalID = "2R5VJA8RQB";
        ChemicalSubstance chemical = getChemicalFromFile(approvalID);
        
        SaltValidator validator = new SaltValidator();
        ValidationResponse response = validator.validate(chemical, null);

        assertTrue(response.getValidationMessages().stream().anyMatch(
                m
                -> ((ValidationMessage) m).getMessage().contains("Each fragment should be present as a separate record in the database.")));
     }

    private ChemicalSubstance getChemicalFromFile(String name) {
        try {
            File chemicalFile = new ClassPathResource("testJSON/" + name + ".json").getFile();
            ChemicalSubstanceBuilder builder = SubstanceBuilder.from(chemicalFile);
            ChemicalSubstance s = builder.build();

            //'validation' here is a way of setting properties required for salt validator's duplicate check
            ChemicalValidator chemicalValidator = new ChemicalValidator();
            chemicalValidator.setStructureProcessor(structureProcessor);
            chemicalValidator.validate(s, null);
            return s;
        } catch (IOException ex) {
            log.error("Error reading chemical file", ex);
        }
        return null;
    }
}