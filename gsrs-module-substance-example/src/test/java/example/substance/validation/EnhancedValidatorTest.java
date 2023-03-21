package example.substance.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.standardizer.FDAFullNameStandardizer;
import gsrs.module.substance.standardizer.FDAMinimumNameStandardizer;
import gsrs.module.substance.standardizer.NameStandardizer;
import gsrs.module.substance.standardizer.NameStandardizerConfiguration;
import gsrs.security.GsrsSecurityUtils;
import gsrs.services.GroupService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Group;
import ix.core.models.Role;
import ix.core.validator.*;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.strategy.AcceptApplyAllProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactoryConfiguration;
import ix.ginas.utils.validation.validators.enhanced.EnhancedValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Slf4j


@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
// Doesn't work
// @TestPropertySource(properties = {
// "gsrs.processing-strategy = {\n" +
// "    \"defaultStrategy\": \"ACCEPT_APPLY_ALL\",\n" +
// "    \"overrideRules\": [\n" +
// "        {\"regex\": \"W.*\", \"userRoles\": [\"DataEntry\"], \"newMessageType\": \"NOTICE\"}\n" +
//"    ]\n" +
// "}\n"
// })

public class EnhancedValidatorTest extends AbstractSubstanceJpaFullStackEntityTest {
// AbstractSubstanceJpaEntityTest {
/*
    @TestConfiguration
static class Testconfig{
    @Bean
    public GsrsProcessingStrategyFactoryConfiguration gsrsProcessingStrategyFactoryConfig() {
        GsrsProcessingStrategyFactoryConfiguration cfg= new GsrsProcessingStrategyFactoryConfiguration() {
        };
        return cfg;
    }
}

 */
    @Autowired
    private GroupService groupService;

    // @Autowired
    // private GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory;

    // @Autowired
    // private GsrsProcessingStrategyFactoryConfiguration gsrsProcessingStrategyFactoryConfiguration;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void testEnhancedValidator1() {
        String testString = "These tags will be kept";
        Substance newSubstance = new Substance();
        newSubstance.names.add(new Name("ASPIRIN ABC"));
        newSubstance.names.add(new Name("ASPIRIN DEF"));
        EnhancedValidator validator = new EnhancedValidator();
        ValidationResponse<Substance> response = validator.validate(newSubstance, null);
        String currentUser = GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get() : "unknown";
        Boolean hasRole = GsrsSecurityUtils.hasAnyRoles("Admin");

        assertEquals("admin", currentUser);
        assertEquals(true, hasRole);
        response.getValidationMessages().forEach(vm->{
            System.out.println( String.format("user: %s", currentUser));
            System.out.println( String.format("type: %s", vm.getMessageType()));
            System.out.println( String.format("message: %s", vm.getMessage()));
            System.out.println( String.format("messageId: %s", vm.getMessageId()));
            System.out.println( String.format("isError: %s", vm.isError()));
        });
    }


    @Test
    @WithMockUser(username = "bob", roles="DataEntry")
    public void testEnhancedValidator2() throws ClassNotFoundException {
        // Here, my goal is to override the gsrs.processing-strategy property value, but I am not sure how to
        // override the value from substance-core.conf in a test
        // or, alternatively, setup a test that doesn't read substance-core.conf

        Substance newSubstance = new Substance();
        newSubstance.names.add(new Name("ASPIRIN ABC"));
        newSubstance.names.add(new Name("ASPIRIN DEF"));
        // see also ValidatorConfigTest in starter
        EnhancedValidator validator = new EnhancedValidator();
        validator = AutowireHelper.getInstance().autowireAndProxy(validator);
        DefaultValidatorConfig vConf = new DefaultValidatorConfig();
        vConf.setValidatorClass(EnhancedValidator.class);


        // Is it possible to set a processing message configuration for a specific validator?
        // vConf.setParameters( ); ??

        // GsrsProcessingStrategyFactoryConfiguration pConf = gsrsProcessingStrategyFactoryConfiguration;
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        GsrsProcessingStrategyFactoryConfiguration.OverrideRule or1 = new GsrsProcessingStrategyFactoryConfiguration.OverrideRule();
        or1.setRegex(Pattern.compile("W.*"));
        or1.setUserRoles(Role.roles(Role.valueOf("Admin"), Role.valueOf("Approver")));
        or1.setNewMessageType(ValidationMessage.MESSAGE_TYPE.NOTICE);
        GsrsProcessingStrategyFactoryConfiguration.OverrideRule or2 = new GsrsProcessingStrategyFactoryConfiguration.OverrideRule();
        or2.setRegex(Pattern.compile("W.*"));
        or2.setUserRoles(Role.roles(Role.valueOf("DataEntry")));
        or2.setNewMessageType(ValidationMessage.MESSAGE_TYPE.ERROR);
        // GsrsProcessingStrategy strategy =  gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL");
        pConf.setOverrideRules(Arrays.asList(or1, or2));
        pConf.setDefaultStrategy("ACCEPT_APPLY_ALL");

        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        gsrsProcessingStrategyFactory = AutowireHelper.getInstance().autowireAndProxy(gsrsProcessingStrategyFactory);

        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy(pConf.getDefaultStrategy());
        // How can the override rules in pConf affect the validation response below? Seems like they don't?

        ValidationResponse<Substance> response2 = new ValidationResponse<>();
        ValidationResponseBuilder validationResponsebuilder = new ValidationResponseBuilder(newSubstance, response2, strategy);
        validator.validate(newSubstance, null, validationResponsebuilder);

        ValidationResponse<Substance> response = validationResponsebuilder.buildResponse();
        String currentUser = GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get() : "unknown";
        Boolean hasRole = GsrsSecurityUtils.hasAnyRoles("DataEntry");
        assertEquals("bob", currentUser);
        assertEquals(true, hasRole);

        response.getValidationMessages().forEach(vm -> {
            System.out.println(String.format("user: %s", currentUser));
            System.out.println(String.format("type: %s", vm.getMessageType()));
            System.out.println(String.format("message: %s", vm.getMessage()));
            System.out.println(String.format("messageId: %s", vm.getMessageId()));
            System.out.println(String.format("isError: %s", vm.isError()));
            // I want this to succeed, but it doesn't
            assertEquals("E", vm.getMessageId().substring(0,1));

        });

    }
}
