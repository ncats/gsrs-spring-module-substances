package example.substance.validation;

import example.substance.AbstractSubstanceJpaEntityTest;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.BasicNameValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author mitch
 */
@Slf4j
public class BasicNameValidatorTest extends AbstractSubstanceJpaEntityTest {

    public BasicNameValidatorTest() {
    }

    @Test
    public void testValidation() {
        String basicName = "ethanol";
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical = builder.addName(basicName)
                .setStructureWithDefaultReference("CCO")
                .build();
        BasicNameValidator validator = new BasicNameValidator();
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
        ValidationResponse<Substance> response = validator.validate(chemical, null);
        Assertions.assertEquals(0, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("minimally standardized to")).count());
    }

}
