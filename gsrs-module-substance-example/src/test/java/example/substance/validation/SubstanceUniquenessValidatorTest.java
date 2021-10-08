package example.substance.validation;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.StructurallyDiverseSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import ix.ginas.utils.validation.validators.SubstanceUniquenessValidator;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author mitch
 */
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class SubstanceUniquenessValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {

    public SubstanceUniquenessValidatorTest() {
    }

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    StructureProcessor structureProcessor;

    private final String fileName = "rep18.gsrs";

    @BeforeEach
    public void setupIndexers() throws IOException {
        log.trace("setupIndexers");
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            //ensure calculation of def hash
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
            log.trace("completed set-up of validator config");
        }

        File dataFile = new ClassPathResource(fileName).getFile();
        loadGsrsFile(dataFile);
        log.trace("setupIndexers complete");
    }

    @Test
    public void testValidation() {
        log.trace("Starting in testValidation");
        String name = "G6867RWN6N";
        String completeDuplicateMessage = "appears to be a full duplicate";
        ChemicalSubstance protein = getChemicalSubstanceFromFile(name);
        protein.uuid = UUID.randomUUID();
        SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse response = validator.validate(protein, null);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(m -> ((GinasProcessingMessage) m).message.contains(completeDuplicateMessage)));
    }

    @Test
    public void testExactDuplicateStrDiv() {
        log.trace("Starting in testValidation");
        String name = "N5WWR36MDJ";
        String completeDuplicateMessage = "appears to be a full duplicate";
        Substance substance = getSubstanceFromFile(name);
        substance.uuid = UUID.randomUUID();
        SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse response = validator.validate(substance, null);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(m -> ((GinasProcessingMessage) m).message.contains(completeDuplicateMessage)));
    }

    @Test
    public void testValidationDiastereomer() {
        log.trace("Starting in testValidation");
        String name = "G6867RWN6N-diast";
        String possibleDuplicateMessage = "is a possible duplicate";
        ChemicalSubstance chem = getChemicalSubstanceFromFile(name);
        chem.uuid = UUID.randomUUID();

        SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse response = validator.validate(chem, null);
        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(m -> ((GinasProcessingMessage) m).message.contains(possibleDuplicateMessage)));
    }
    
    @Test
    public void testDuplicateDoesNotFindItself() {
        log.trace("Starting in testValidation");
        String name ="G6867RWN6N-diast";
        String possibleDuplicateMessage= "is a possible duplicate";
        ChemicalSubstance chem = getChemicalSubstanceFromFile(name);
        SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse response = validator.validate(chem, null);
        //should not contain a duplicate message
        Assertions.assertFalse(response.getValidationMessages().stream().anyMatch(m-> ((GinasProcessingMessage) m).message.contains(possibleDuplicateMessage)));
    }

    
    @Test
    public void testValidationNoDuplicates() {
        log.trace("Starting in testValidationNoDuplicates");
        String idToLookup = "90e9191d-1a81-4a53-b7ee-560bf9e68109";
        String fullDuplicateWarning ="appears to be a full duplicate";
        TransactionTemplate transactionMod = new TransactionTemplate(transactionManager);
        transactionMod.executeWithoutResult(a -> {
            Substance toModify = this.substanceRepository.findById(UUID.fromString(idToLookup)).get();
            Assertions.assertTrue(toModify.names.size() > 0);
            SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
            AutowireHelper.getInstance().autowire(validator);
            ValidationResponse response = validator.validate(toModify, null);
            long countOfMessages = response.getValidationMessages().stream()
                    .filter(m-> ((ValidationMessage)m).getMessageType()==ValidationMessage.MESSAGE_TYPE.WARNING)
                    .filter(m-> ((ValidationMessage)m).getMessage().contains(fullDuplicateWarning))
                    .count();
            Assertions.assertEquals(0, countOfMessages);
            
        });
    }

    @Test
    public void testValidationRetrievedItem() {
        /*
        retrieve a substance, make a small change, validate to confirm that def hash search does NOT include the substance itself
        */
        log.trace("Starting in testValidationRetrievedItem");
        String idToLookup = "ac776f92-b90f-48a0-a54e-7461a60c84b3";
        String fullDuplicateWarning ="appears to be a full duplicate";
        TransactionTemplate transactionMod = new TransactionTemplate(transactionManager);
        transactionMod.executeWithoutResult(a -> {
            ChemicalSubstance toModify = (ChemicalSubstance) this.substanceRepository.findById(UUID.fromString(idToLookup)).get();
            Assertions.assertTrue(toModify.names.size() > 0);
            SubstanceUniquenessValidator validator = new SubstanceUniquenessValidator();
            AutowireHelper.getInstance().autowire(validator);
            ValidationResponse response =null;
            try {
                response = substanceEntityService.validateEntity( toModify.toFullJsonNode());
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
            //ValidationResponse response = validator.validate(toModify, null);
            log.debug("total validation messages: " +response.getValidationMessages().size());
            response.getValidationMessages().forEach(m->{
                ValidationMessage msg = (ValidationMessage)m;
                log.debug(String.format("message type: %s; message: %s", msg.getMessageType(), msg.getMessage()));
            });
            long countOfMessages = response.getValidationMessages().stream()
                    .filter(m-> ((ValidationMessage)m).getMessageType()==ValidationMessage.MESSAGE_TYPE.WARNING)
                    .filter(m-> ((ValidationMessage)m).getMessage().contains(fullDuplicateWarning))
                    .count();
            Assertions.assertEquals(0, countOfMessages);
            
        });

    }
    private ChemicalSubstance getChemicalSubstanceFromFile(String name) {
        try {
            File proteinFile = new ClassPathResource("testJSON/" + name + ".json").getFile();
            ChemicalSubstanceBuilder builder = SubstanceBuilder.from(proteinFile);
            ChemicalValidator chemicalValidator = new ChemicalValidator();
            chemicalValidator.setStructureProcessor(structureProcessor);
            ChemicalSubstance chem = builder.build();
            chemicalValidator.validate(chem, null);

            return builder.build();
        } catch (IOException ex) {
            log.error("Error retrieving substance from file", ex);
        }
        return null;
    }

    private Substance getSubstanceFromFile(String name) {
        try {
            File substanceFile = new ClassPathResource("testJSON/" + name + ".json").getFile();
            StructurallyDiverseSubstanceBuilder builder = SubstanceBuilder.from(substanceFile);

            return builder.build();
        } catch (IOException ex) {
            log.error("Error retrieving substance from file", ex);
        }
        return null;
    }

}
