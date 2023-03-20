package example.substance.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.GsrsModuleSubstanceApplication;
import gsrs.security.GsrsSecurityUtils;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Group;
import ix.core.models.Role;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidationResponseBuilder;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactoryConfiguration;
import ix.ginas.utils.validation.validators.BasicNameValidator;
import ix.ginas.utils.validation.validators.enhanced.EnhancedValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

@Slf4j

// @TestPropertySource(properties = {
//    "logging.level.example.substance.validation=info"
// })

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles="Admin")
public class EnhancedValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory;

    @Autowired
    private GsrsProcessingStrategyFactoryConfiguration gsrsProcessingStrategyFactoryConfiguration;

    @Test
    public void testEnhancedValidator1() {
        String testString = "These tags will be kept";
        Substance newSubstance = new Substance();
        newSubstance.names.add(new Name("ASPIRIN ABC"));
        newSubstance.names.add(new Name("ASPIRIN DEF"));
        EnhancedValidator validator = new EnhancedValidator();
        ValidationResponse<Substance> response = validator.validate(newSubstance, null);
        String currentUser = GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get() : "unknown";
        response.getValidationMessages().forEach(vm->{
            System.out.println( String.format("user: %s", currentUser));
            System.out.println( String.format("type: %s", vm.getMessageType()));
            System.out.println( String.format("message: %s", vm.getMessage()));
            System.out.println( String.format("messageId: %s", vm.getMessageId()));
            System.out.println( String.format("isError: %s", vm.isError()));
        });

//        Assertions.assertEquals(1, response.getValidationMessages().stream()
//                .filter(m -> m.getMessage().contains(testString)).count());
    }

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @WithMockUser(username = "bob", roles="DataEntry")
    public void testEnhancedValidator2() throws ClassNotFoundException {
        GsrsProcessingStrategyFactoryConfiguration pConf = gsrsProcessingStrategyFactoryConfiguration;
        Substance newSubstance = new Substance();
        newSubstance.names.add(new Name("ASPIRIN ABC"));
        newSubstance.names.add(new Name("ASPIRIN DEF"));
        /*
        ValidatorConfig conf = new DefaultValidatorConfig();
        conf.setValidatorClass(EnhancedValidator.class);
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        conf.setParameters(map);
        EnhancedValidator validator = (EnhancedValidator) conf.newValidatorPlugin(mapper, this.getClass().getClassLoader());
        */
        EnhancedValidator validator = new EnhancedValidator();
        validator = AutowireHelper.getInstance().autowireAndProxy(validator);
        DefaultValidatorConfig vConf = new DefaultValidatorConfig();
        vConf.setValidatorClass(EnhancedValidator.class);


        // GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        GsrsProcessingStrategyFactoryConfiguration.OverrideRule or1 = new GsrsProcessingStrategyFactoryConfiguration.OverrideRule();
        or1.setRegex(Pattern.compile("W.*"));
        or1.setUserRoles(Role.roles(Role.valueOf("Admin"), Role.valueOf("Approver")));
        or1.setNewMessageType(ValidationMessage.MESSAGE_TYPE.NOTICE);

        GsrsProcessingStrategyFactoryConfiguration.OverrideRule or2 = new GsrsProcessingStrategyFactoryConfiguration.OverrideRule();
        or2.setRegex(Pattern.compile("W.*"));
        or2.setUserRoles(Role.roles(Role.valueOf("DataEntry")));
        or2.setNewMessageType(ValidationMessage.MESSAGE_TYPE.ERROR);
        // GsrsProcessingStrategy strategy =  gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL");
        GsrsProcessingStrategy strategy = createAcceptApplyAllStrategy();
        // strategy = pConf.getDefaultStrategy();
        // pConf.setDefaultStrategy("ACCEPT_APPLY_ALL");

        pConf.setOverrideRules(Arrays.asList(or1, or2));

        ValidationResponse<Substance> response2 = new ValidationResponse<>();
        ValidationResponseBuilder validationResponsebuilder = new ValidationResponseBuilder(newSubstance, response2, strategy);
        validator.validate(newSubstance, null, validationResponsebuilder);

        // ValidationResponse<Substance> response = validator.validate(newSubstance, null);

        ValidationResponse<Substance> response = validationResponsebuilder.buildResponse();

        // Assertions.assertEquals(1, response.getValidationMessages().stream()
        // .filter(m -> m.getMessage().contains("minimally standardized to")).count());


        String currentUser = GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get() : "unknown";
        response.getValidationMessages().forEach(vm -> {
            System.out.println(String.format("user: %s", currentUser));
            System.out.println(String.format("type: %s", vm.getMessageType()));
            System.out.println(String.format("message: %s", vm.getMessage()));
            System.out.println(String.format("messageId: %s", vm.getMessageId()));
            System.out.println(String.format("isError: %s", vm.isError()));
        });

    }

    //copied from NameEntityService
    private GsrsProcessingStrategy createAcceptApplyAllStrategy() {
        return gsrsProcessingStrategyFactory.createNewDefaultStrategy();
    }




}
