package example.substance.validation;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.GsrsModuleSubstanceApplication;
import gsrs.security.GsrsSecurityUtils;
import gsrs.services.GroupService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Role;
import ix.core.validator.*;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactoryConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j


@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class ValidationMessageFilterTest extends AbstractSubstanceJpaFullStackEntityTest {  // AbstractSubstanceJpaEntityTest {
    // data load: turn off entity processors to see how fast things go?

    // SubstanceEntityServiceImpl extends AbstractGsrsEntityService.
    // See AbstractGsrsEntityService to four steps created  (validator=, response=, callback=, then validator.validate)
    // The message override/filtering is tied to callback complete, and therein processMessage.

    // TP Notes:
    // validation flow needs to be refactored.
    // "Strategy" should be part of starter not hidden in substances.
    // Mechanism for creating a callback needs to be exposed through a more obvious interface, maybe public methods in SubstanceEntityServiceImpl
    // Interaction between validation response, callback, response builder and validator seems overly complex.
    // We could refactor these tests to more directly use the rest api flow or call the methods that the api calls.

    @Autowired
    private GroupService groupService;

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void testEnhancedValidator1() throws ClassNotFoundException {
        // Reset the configuration values associated with gsrs.processing-strategy property value; set override values.
        // Check if we can change WARNING to ERROR.
        // Check if we can create message id based on template/format.

        String testUsername = "admin";
        String testRole = "Admin";
        String testMessageType = "ERROR";
        String testTemplate = "FYI, there are %s names.*";
        String testContains = "FYI,";


        Substance newSubstance = new Substance();
        newSubstance.names.add(new Name("ASPIRIN ABC"));
        // see also ValidatorConfigTest in starter
        NameCountValidator validator = new NameCountValidator();
        validator = AutowireHelper.getInstance().autowireAndProxy(validator);

        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactoryConfiguration.OverrideRule or1 = new GsrsProcessingStrategyFactoryConfiguration.OverrideRule();
        or1.setRegex(Pattern.compile(testTemplate));
        or1.setUserRoles(Role.roles(Role.valueOf("Admin"), Role.valueOf("Approver")));
        or1.setNewMessageType(ValidationMessage.MESSAGE_TYPE.ERROR);
        pConf.setOverrideRules(Arrays.asList(or1));
        pConf.setDefaultStrategy("ACCEPT_APPLY_ALL_WARNINGS");

        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        gsrsProcessingStrategyFactory = AutowireHelper.getInstance().autowireAndProxy(gsrsProcessingStrategyFactory);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy(pConf.getDefaultStrategy());
        ValidationResponse<Substance> response2 = new ValidationResponse<>(newSubstance);
        ValidatorCallback callback = createCallbackFor(newSubstance, response2, DefaultValidatorConfig.METHOD_TYPE.CREATE, strategy);
        validator.validate(newSubstance, null, callback);
        callback.complete();

        String currentUser = GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get() : "unknown";
        boolean hasRole = GsrsSecurityUtils.hasAnyRoles(testRole);
        assertEquals(testUsername, currentUser);
        assertEquals(true, hasRole);

        response2.getValidationMessages().forEach(vm -> {
            // System.out.println(String.format("user: %s", currentUser));
            // System.out.println(String.format("type: %s", vm.getMessageType()));
            // System.out.println(String.format("message: %s", vm.getMessage()));
            // System.out.println(String.format("messageId: %s", vm.getMessageId()));
            // System.out.println(String.format("isError: %s", vm.isError()));
            assertTrue(vm.getMessageId().contains(testContains));
            assertEquals(testMessageType, vm.getMessageType().toString());
        });
    }


    @Test
    @WithMockUser(username = "bob", roles="DataEntry")
    public void testEnhancedValidator2() throws ClassNotFoundException {
        // Reset the configuration values associated with gsrs.processing-strategy property value; set override values.
        // Check if we can change WARNING to NOTICE.

        String testUsername = "bob";
        String testRole = "DataEntry";
        String testMessageType = "NOTICE";
        Substance newSubstance = new Substance();
        newSubstance.names.add(new Name("ASPIRIN ABC"));
        newSubstance.names.add(new Name("ASPIRIN DEF"));
        // see also ValidatorConfigTest in starter
        OnlyOneNameValidator validator = new OnlyOneNameValidator();
        validator = AutowireHelper.getInstance().autowireAndProxy(validator);

        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactoryConfiguration.OverrideRule or1 = new GsrsProcessingStrategyFactoryConfiguration.OverrideRule();
        or1.setRegex(Pattern.compile("W.*"));
        or1.setUserRoles(Role.roles(Role.valueOf("Admin"), Role.valueOf("Approver")));
        or1.setNewMessageType(ValidationMessage.MESSAGE_TYPE.NOTICE);
        GsrsProcessingStrategyFactoryConfiguration.OverrideRule or2 = new GsrsProcessingStrategyFactoryConfiguration.OverrideRule();
        or2.setRegex(Pattern.compile("W.*"));
        or2.setUserRoles(Role.roles(Role.valueOf("DataEntry")));
        or2.setNewMessageType(ValidationMessage.MESSAGE_TYPE.NOTICE);
        pConf.setOverrideRules(Arrays.asList(or1, or2));
        pConf.setDefaultStrategy("ACCEPT_APPLY_ALL_WARNINGS");

        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        gsrsProcessingStrategyFactory = AutowireHelper.getInstance().autowireAndProxy(gsrsProcessingStrategyFactory);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy(pConf.getDefaultStrategy());
        ValidationResponse<Substance> response2 = new ValidationResponse<>(newSubstance);
        ValidatorCallback callback = createCallbackFor(newSubstance, response2, DefaultValidatorConfig.METHOD_TYPE.CREATE, strategy);
        validator.validate(newSubstance, null, callback);
        callback.complete();

        String currentUser = GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get() : "unknown";
        boolean hasRole = GsrsSecurityUtils.hasAnyRoles(testRole);
        assertEquals(testUsername, currentUser);
        assertEquals(true, hasRole);

        response2.getValidationMessages().forEach(vm -> {
            // System.out.println(String.format("user: %s", currentUser));
            // System.out.println(String.format("type: %s", vm.getMessageType()));
            // System.out.println(String.format("message: %s", vm.getMessage()));
            // System.out.println(String.format("messageId: %s", vm.getMessageId()));
            // System.out.println(String.format("isError: %s", vm.isError()));
            assertEquals(testMessageType, vm.getMessageType().toString());
        });
    }







    // This is modified from SubstanceServiceImpl because we could not pass strategy
    protected <T> ValidatorCallback createCallbackFor(T object, ValidationResponse<T> response, ValidatorConfig.METHOD_TYPE type, GsrsProcessingStrategy strategy) {

        ValidationResponseBuilder<T> builder = new ValidationResponseBuilder<T>(object, response, strategy){
            @Override
            public void complete() {
                if(object instanceof Substance) {
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
//
        if(GsrsSecurityUtils.hasAnyRoles(Role.SuperUpdate,Role.SuperDataEntry,Role.Admin)) {
            builder.allowPossibleDuplicates(true);
        }

        return builder;
    }


    public class OnlyOneNameValidator extends AbstractValidatorPlugin<Substance> {
        @Override
        public void validate(Substance s, Substance objold, ValidatorCallback callback) {
            // This is a TEMPORARY, simple validator for testing.
            if (s.getAllNames().size() > 0) {
                GinasProcessingMessage mes = GinasProcessingMessage
                .WARNING_MESSAGE(String.format("Only one name, please."));
                mes.appliableChange(true);
                callback.addMessage(mes);
            }
        }
    }
    public class NameCountValidator extends AbstractValidatorPlugin<Substance> {
        @Override
        public void validate(Substance s, Substance objold, ValidatorCallback callback) {
            // This is a TEMPORARY, simple validator for testing.
            int size =s.getAllNames().size();
            if (size > 0) {
                String template = "FYI, there are %s names in your substance";
                GinasProcessingMessage mes = GinasProcessingMessage
                .WARNING_MESSAGE(template, size);
                mes.appliableChange(true);
                mes.setMessageId(template);
                callback.addMessage(mes);
            }
        }
    }


}