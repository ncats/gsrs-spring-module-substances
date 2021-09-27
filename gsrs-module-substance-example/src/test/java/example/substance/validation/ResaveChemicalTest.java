package example.substance.validation;

import example.loadertests.TestableFacetIndexValueMaker;
import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import ix.ginas.utils.validation.validators.DefinitionalHashValidator;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
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
@Slf4j
@WithMockUser(username = "admin", roles = "Admin")
public class ResaveChemicalTest extends AbstractSubstanceJpaFullStackEntityTest {

    private final String fileName = "testdumps/2R5VJA8RQB-chem.txt";

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @BeforeEach
    public void clearIndexers() throws IOException {
        TestableFacetIndexValueMaker indexer = new TestableFacetIndexValueMaker();
        AutowireHelper.getInstance().autowire(indexer);
        testIndexValueMakerFactory.addIndexValueMaker(indexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
            ValidatorConfig config2 = new DefaultValidatorConfig();
            config2.setValidatorClass(DefinitionalHashValidator.class);
            config2.setNewObjClass(Substance.class);
            factory.addValidator("substances", config);
        }

        File dataFile = new ClassPathResource(fileName).getFile();
        loadGsrsFile(dataFile);
    }

    @Test
    public void defHashChangeTest() throws Exception {
        String uuid = "0d1371fc-904f-45e9-b073-ba55dacc4f30";
        String defChangeMessage = "Definitional changes have been made";
        //Substance substance = this.substanceRepository.findById(UUID.fromString(uuid)).get();
        Substance substance = substanceEntityService.get(UUID.fromString(uuid)).get();
        //make sure we have retrieved a complete substance
        Assertions.assertTrue(substance.names.size()>0);
        //add a code; something that will not trigger a def hash warning
        Code newCode = new Code();
        newCode.code = "50-00-0";
        newCode.codeSystem = "CAS";
        newCode.type = "PRIMARY";
        substance.codes.add(newCode);
        TransactionTemplate transactionSave = new TransactionTemplate(transactionManager);
        ValidationResponse response =transactionSave.execute(ts -> {
            ValidationResponse responsev =null;
            try {
                responsev = substanceEntityService.validateEntity(substance.toFullJsonNode());
            } catch (Exception ex) {
                Logger.getLogger(ResaveChemicalTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            return responsev;
        });
        
        System.out.println("response.getValidationMessages() ");
        response.getValidationMessages().forEach(m->System.out.println("type: "+ ((ValidationMessage)m).getMessageType()
            + "; message: " + ((ValidationMessage)m).getMessage()));
        Stream<ValidationMessage> messageStream = response.getValidationMessages().stream();
        long messageCount = messageStream
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains(defChangeMessage))
                .count();
        System.out.println("messageCount: " + messageCount);
        Assertions.assertEquals(0, messageCount);
    }
    

}
