package example.substance.validation;

import example.GsrsModuleSubstanceApplication;
import gsrs.security.GsrsSecurityUtils;
import gsrs.services.GroupService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Role;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidationResponseBuilder;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.VocabularyTerm;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactoryConfiguration;
import ix.ginas.utils.validation.validators.CVValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class CVValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private GroupService groupService;

    @Test
    void test1Duplicate() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("holidays");
        VocabularyTerm holiday1 = new VocabularyTerm();
        holiday1.setValue("Presidents Day");
        holiday1.setDisplay("Presidents Day");
        VocabularyTerm holiday2 = new VocabularyTerm();
        holiday2.setValue("Valentines Day");
        holiday2.setDisplay("Valentines Day");
        VocabularyTerm holiday3 = new VocabularyTerm();
        holiday3.setValue("Pi Day");
        holiday3.setDisplay("Pi Day");
        vocabulary.terms = Arrays.asList(holiday1, holiday2, holiday3);
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL_WARNINGS");
        ValidationResponse<ControlledVocabulary> response = new ValidationResponse<>(vocabulary);
        ValidatorCallback callback = createCallbackFor(vocabulary, response, ValidatorConfig.METHOD_TYPE.UPDATE, strategy);
        CVValidator validator = new CVValidator();
        validator.validate(vocabulary, null, callback);
        Assertions.assertTrue(response.getValidationMessages().isEmpty());
    }

    @Test
    void testDuplicate2() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("holidays");
        VocabularyTerm holiday1 = new VocabularyTerm();
        holiday1.setValue("Presidents Day");
        holiday1.setDisplay("Presidents Day");
        VocabularyTerm holiday2 = new VocabularyTerm();
        holiday2.setValue("Valentines Day");
        holiday2.setDisplay("Valentines Day");
        VocabularyTerm holiday2a = new VocabularyTerm();
        holiday2a.setValue("Valentines Day");
        holiday2a.setDisplay("New Valentines Day");
        //holiday2 and holiday2a have the same values but different display
        VocabularyTerm holiday3 = new VocabularyTerm();
        holiday3.setValue("Pi Day");
        holiday3.setDisplay("Pi Day");
        vocabulary.terms = Arrays.asList(holiday1, holiday2, holiday3, holiday2a);
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL_WARNINGS");
        ValidationResponse<ControlledVocabulary> response = new ValidationResponse<>(vocabulary);
        ValidatorCallback callback = createCallbackFor(vocabulary, response, ValidatorConfig.METHOD_TYPE.UPDATE, strategy);
        CVValidator validator = new CVValidator();
        validator.validate(vocabulary, null, callback);
        Assertions.assertEquals( 0, response.getValidationMessages().size());
    }

    @Test
    void testDuplicateNoDuplicateValues() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("holidays");
        VocabularyTerm holiday1 = new VocabularyTerm();
        holiday1.setValue("Presidents Day");
        holiday1.setDisplay("Presidents Day");
        VocabularyTerm holiday2 = new VocabularyTerm();
        holiday2.setValue("Valentines Day");
        holiday2.setDisplay("Valentines Day");
        VocabularyTerm holiday2a = new VocabularyTerm();
        holiday2a.setValue("Valentines Day");
        holiday2a.setDisplay("New Valentines Day");
        //holiday2 and holiday2a have the same values but different display
        VocabularyTerm holiday3 = new VocabularyTerm();
        holiday3.setValue("Pi Day");
        holiday3.setDisplay("Pi Day");
        vocabulary.terms = Arrays.asList(holiday1, holiday2, holiday3, holiday2a);
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL_WARNINGS");
        ValidationResponse<ControlledVocabulary> response = new ValidationResponse<>(vocabulary);
        ValidatorCallback callback = createCallbackFor(vocabulary, response, ValidatorConfig.METHOD_TYPE.UPDATE, strategy);
        CVValidator validator = new CVValidator();
        validator.setAllowDuplicateValues(false);
        validator.validate(vocabulary, null, callback);
        Assertions.assertEquals( 1, response.getValidationMessages().size());
    }

    @Test
    void testDuplicate2a() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("holidays");
        VocabularyTerm holiday1 = new VocabularyTerm();
        holiday1.setValue("Presidents Day");
        holiday1.setDisplay("Presidents Day");
        VocabularyTerm holiday2 = new VocabularyTerm();
        holiday2.setValue("Valentines Day");
        holiday2.setDisplay("Valentines Day");
        VocabularyTerm holiday2a = new VocabularyTerm();
        holiday2a.setValue("New  Valentines Day");
        holiday2a.setDisplay("Valentines Day");
        //holiday2 and holiday2a have the same display but different values
        VocabularyTerm holiday3 = new VocabularyTerm();
        holiday3.setValue("Pi Day");
        holiday3.setDisplay("Pi Day");
        vocabulary.terms = Arrays.asList(holiday1, holiday2, holiday3, holiday2a);
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL_WARNINGS");
        ValidationResponse<ControlledVocabulary> response = new ValidationResponse<>(vocabulary);
        ValidatorCallback callback = createCallbackFor(vocabulary, response, ValidatorConfig.METHOD_TYPE.UPDATE, strategy);
        CVValidator validator = new CVValidator();
        validator.validate(vocabulary, null, callback);
        Assertions.assertEquals( 1, response.getValidationMessages().size());
    }

    @Test
    void testDuplicate3() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("holidays");
        VocabularyTerm holiday1 = new VocabularyTerm();
        holiday1.setValue("Presidents Day");
        holiday1.setDisplay("Presidents Day");
        VocabularyTerm holiday2 = new VocabularyTerm();
        holiday2.setValue("Valentines Day");
        holiday2.setDisplay("Valentines Day");
        VocabularyTerm holiday3 = new VocabularyTerm();
        holiday3.setValue("Pi Day");
        holiday3.setDisplay("Pi Day");
        VocabularyTerm holiday4 = new VocabularyTerm();
        holiday4.setValue("Easter");
        holiday4.setDisplay("Easter");

        vocabulary.terms = Arrays.asList(holiday1, holiday2, holiday3, holiday4);
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL_WARNINGS");
        ValidationResponse<ControlledVocabulary> response = new ValidationResponse<>(vocabulary);
        ValidatorCallback callback = createCallbackFor(vocabulary, response, ValidatorConfig.METHOD_TYPE.UPDATE, strategy);
        CVValidator validator = new CVValidator();
        validator.validate(vocabulary, null, callback);
        Assertions.assertEquals( 0, response.getValidationMessages().size());
    }

    @Test
    void testDuplicate4() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("holidays");
        VocabularyTerm holiday1 = new VocabularyTerm();
        holiday1.setValue("Presidents Day");
        holiday1.setDisplay("Presidents Day");
        VocabularyTerm holiday2 = new VocabularyTerm();
        holiday2.setValue("Valentines Day");
        holiday2.setDisplay("Valentines Day");
        VocabularyTerm holiday3 = new VocabularyTerm();
        holiday3.setValue("Pi Day");
        holiday3.setDisplay("Pi Day");
        VocabularyTerm holiday4 = new VocabularyTerm();
        holiday4.setValue("Easter");
        holiday4.setDisplay("Easter");
        holiday4.setDescription("same old");
        VocabularyTerm holiday4a = new VocabularyTerm();
        holiday4a.setValue("Easter");
        holiday4a.setDisplay("Easter");
        holiday4a.setDescription("Something different");
        VocabularyTerm holiday4b = new VocabularyTerm();
        holiday4b.setValue("Valentines Day");
        holiday4b.setDisplay("Easter");
        holiday4b.setDescription("Fusion");

        vocabulary.terms = Arrays.asList(holiday1, holiday2, holiday3, holiday4, holiday4a, holiday4b);
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL_WARNINGS");
        ValidationResponse<ControlledVocabulary> response = new ValidationResponse<>(vocabulary);
        ValidatorCallback callback = createCallbackFor(vocabulary, response, ValidatorConfig.METHOD_TYPE.UPDATE, strategy);
        CVValidator validator = new CVValidator();
        validator.validate(vocabulary, null, callback);
        Assertions.assertEquals( 1, response.getValidationMessages().size());
        Assertions.assertFalse(response.isValid());
    }


    @Test
    void testDuplicate4Case() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("holidays");
        VocabularyTerm holiday1 = new VocabularyTerm();
        holiday1.setValue("Presidents Day");
        holiday1.setDisplay("Presidents Day");
        VocabularyTerm holiday2 = new VocabularyTerm();
        holiday2.setValue("Valentines Day");
        holiday2.setDisplay("Valentines Day");
        VocabularyTerm holiday3 = new VocabularyTerm();
        holiday3.setValue("Pi Day");
        holiday3.setDisplay("Pi Day");
        VocabularyTerm holiday4 = new VocabularyTerm();
        holiday4.setValue("Easter");
        holiday4.setDisplay("Easter");
        holiday4.setDescription("same old");
        VocabularyTerm holiday4a = new VocabularyTerm();
        holiday4a.setValue("Easter");
        holiday4a.setDisplay("EASTER");
        holiday4a.setDescription("Something different");

        vocabulary.terms = Arrays.asList(holiday1, holiday2, holiday3, holiday4, holiday4a);
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL_WARNINGS");
        ValidationResponse<ControlledVocabulary> response = new ValidationResponse<>(vocabulary);
        ValidatorCallback callback = createCallbackFor(vocabulary, response, ValidatorConfig.METHOD_TYPE.UPDATE, strategy);
        CVValidator validator = new CVValidator();
        validator.setAllowDuplicateValues(true);
        validator.validate(vocabulary, null, callback);
        Assertions.assertEquals( 1, response.getValidationMessages().size());
        Assertions.assertFalse(response.isValid());
    }

    @Test
    void testDuplicate4NoDuplicateValues() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("holidays");
        VocabularyTerm holiday1 = new VocabularyTerm();
        holiday1.setValue("Presidents Day");
        holiday1.setDisplay("Presidents Day");
        VocabularyTerm holiday2 = new VocabularyTerm();
        holiday2.setValue("Valentines Day");
        holiday2.setDisplay("Valentines Day");
        VocabularyTerm holiday3 = new VocabularyTerm();
        holiday3.setValue("Pi Day");
        holiday3.setDisplay("Pi Day");
        VocabularyTerm holiday4 = new VocabularyTerm();
        holiday4.setValue("Easter");
        holiday4.setDisplay("Easter");
        holiday4.setDescription("same old");
        VocabularyTerm holiday4a = new VocabularyTerm();
        holiday4a.setValue("Easter");
        holiday4a.setDisplay("Easter");
        holiday4a.setDescription("Something different");
        VocabularyTerm holiday4b = new VocabularyTerm();
        holiday4b.setValue("Valentines Day");
        holiday4b.setDisplay("Easter");
        holiday4b.setDescription("Fusion");

        vocabulary.terms = Arrays.asList(holiday1, holiday2, holiday3, holiday4, holiday4a, holiday4b);
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL_WARNINGS");
        ValidationResponse<ControlledVocabulary> response = new ValidationResponse<>(vocabulary);
        ValidatorCallback callback = createCallbackFor(vocabulary, response, ValidatorConfig.METHOD_TYPE.UPDATE, strategy);
        CVValidator validator = new CVValidator();
        validator.setAllowDuplicateValues(false);
        validator.validate(vocabulary, null, callback);
        Assertions.assertEquals(2, response.getValidationMessages().size());
        Assertions.assertFalse(response.isValid());
    }

    // We do not call the 'complete' method so it does not matter that it references substances
    protected <T> ValidatorCallback createCallbackFor(T object, ValidationResponse<T> response, ValidatorConfig.METHOD_TYPE type,
                                                      GsrsProcessingStrategy strategy) {
        ValidationResponseBuilder<T> builder = new ValidationResponseBuilder<T>(object, response, strategy){
            @Override
            public void complete() {
                if(object instanceof ControlledVocabulary) {
                    ValidationResponse<T> resp = buildResponse();

                    List<GinasProcessingMessage> messages = resp.getValidationMessages()
                            .stream()
                            .filter(m -> m instanceof GinasProcessingMessage)
                            .map(m -> (GinasProcessingMessage) m)
                            .collect(Collectors.toList());
                    //processMessage, handleMessages, addProblems?
                    //Why all 3? because right now each of these methods might set or change fields in validation response.
                    messages.stream().forEach(strategy::processMessage);
                    resp.setValid(false);
                    if (strategy.handleMessages((Substance) object, messages)) {
                        resp.setValid(true);
                    }
                    strategy.addProblems((Substance) object, messages);

                    strategy.setIfValid(resp, messages);
                }
            }
        };
        if(type == ValidatorConfig.METHOD_TYPE.BATCH){
            builder.allowPossibleDuplicates(true);
        }
        if(GsrsSecurityUtils.hasAnyRoles(Role.SuperUpdate,Role.SuperDataEntry,Role.Admin)) {
            builder.allowPossibleDuplicates(true);
        }
        return builder;
    }

}
