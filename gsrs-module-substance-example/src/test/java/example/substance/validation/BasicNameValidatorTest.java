package example.substance.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import gsrs.module.substance.standardizer.FDAFullNameStandardizer;
import gsrs.module.substance.standardizer.FDAMinimumNameStandardizer;
import gsrs.module.substance.standardizer.NameStandardizer;
import gsrs.module.substance.standardizer.NameStandardizerConfiguration;
import gsrs.services.GroupService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidationResponseBuilder;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.strategy.GinasProcessingStrategy;
import ix.ginas.utils.validation.strategy.AcceptApplyAllProcessingStrategy;
import ix.ginas.utils.validation.validators.BasicNameValidator;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author mitch
 */
@Slf4j
public class BasicNameValidatorTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private GroupService groupService;

    

    @TestConfiguration
    static class Testconfig{
        @Bean
        public NameStandardizerConfiguration nameStandardizerConfig() {
        	NameStandardizerConfiguration cfg= new NameStandardizerConfiguration() {

				@Override
				public NameStandardizer nameStandardizer() throws InstantiationException, IllegalAccessException,
						NoSuchMethodException, ClassNotFoundException {
					return new FDAMinimumNameStandardizer();
				}

				@Override
				public NameStandardizer stdNameStandardizer() throws InstantiationException, IllegalAccessException,
						NoSuchMethodException, ClassNotFoundException {
					return new FDAFullNameStandardizer();
				}
        		
        	};
        	
            return cfg;
        }
    }
    
    @Test
    public void testValidation() {
        String basicName = "ethanol";
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical = builder.addName(basicName)
                .setStructureWithDefaultReference("CCO")
                .build();
        BasicNameValidator validator = new BasicNameValidator();
        validator=AutowireHelper.getInstance().autowireAndProxy(validator);
        ValidationResponse<Substance> response = validator.validate(chemical, null);
        Assertions.assertEquals(0, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("minimally standardized to")).count());
    }

    @Test
    public void testValidation2() {
        String basicName = "γ-aminobutyric acid";
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical = builder.addName(basicName)
                .setStructureWithDefaultReference("CCO")
                .build();
        BasicNameValidator validator = new BasicNameValidator();
        validator=AutowireHelper.getInstance().autowireAndProxy(validator);
        ValidationResponse<Substance> response = validator.validate(chemical, null);
        Assertions.assertEquals(0, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("minimally standardized to")).count());
    }

    @Test
    public void testValidation3() {
        String basicName = "any\u200Bthing";
        String expectedName = "anything";
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical = builder.addName(basicName)
                .setStructureWithDefaultReference("CCO")
                .build();
        BasicNameValidator validator = new BasicNameValidator();
        validator=AutowireHelper.getInstance().autowireAndProxy(validator);
        GinasProcessingStrategy strategy = createAcceptApplyAllStrategy();
        ValidationResponse<Substance> response2 = new ValidationResponse<>();
        ValidationResponseBuilder validationResponsebuilder = new ValidationResponseBuilder(chemical,
                response2, strategy);
        validator.validate(chemical, null, validationResponsebuilder);
        ValidationResponse<Substance> response = validationResponsebuilder.buildResponse();
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("minimally standardized to")).count());

        Assertions.assertEquals(expectedName, chemical.names.get(0).name);
    }

    @Test
    public void testValidation4() {
        String basicName = "butanoic     acid";
        String expectedName = "butanoic acid";
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical = builder.addName(basicName)
                .setStructureWithDefaultReference("CCCC(=O)O")
                .build();
        BasicNameValidator validator = new BasicNameValidator();
        validator=AutowireHelper.getInstance().autowireAndProxy(validator);
        GinasProcessingStrategy strategy = createAcceptApplyAllStrategy();
        ValidationResponse<Substance> response2 = new ValidationResponse<>();
        ValidationResponseBuilder validationResponsebuilder = new ValidationResponseBuilder(chemical,
                response2, strategy);
        validator.validate(chemical, null, validationResponsebuilder);
        ValidationResponse<Substance> response = validationResponsebuilder.buildResponse();
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("minimally standardized to")).count());

        Assertions.assertEquals(expectedName, chemical.names.get(0).name);
    }

    @Test
    public void testValidation5() {
        String basicName = "butanoic\u0000acid";
        String expectedName = "butanoicacid";
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical = builder.addName(basicName)
                .setStructureWithDefaultReference("CCCC(=O)O")
                .build();
        BasicNameValidator validator = new BasicNameValidator();
        validator=AutowireHelper.getInstance().autowireAndProxy(validator);
        GinasProcessingStrategy strategy = createAcceptApplyAllStrategy();
        ValidationResponse<Substance> response2 = new ValidationResponse<>();
        ValidationResponseBuilder validationResponsebuilder = new ValidationResponseBuilder(chemical,
                response2, strategy);
        validator.validate(chemical, null, validationResponsebuilder);
        ValidationResponse<Substance> response = validationResponsebuilder.buildResponse();
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("minimally standardized to")).count());

        Assertions.assertEquals(expectedName, chemical.names.get(0).name);
    }

    //copied from NameEntityService
    private GinasProcessingStrategy createAcceptApplyAllStrategy() {
        return new AcceptApplyAllProcessingStrategy(groupService);
    }

}
