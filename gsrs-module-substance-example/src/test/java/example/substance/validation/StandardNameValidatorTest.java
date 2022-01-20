package example.substance.validation;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.StandardNameValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author mitch
 */
@Slf4j
public class StandardNameValidatorTest extends AbstractSubstanceJpaEntityTest {

    @Test
    public void testStandardNameValidation1() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String basicName = "γ-aminobutyric acid";
        String fullyStdName = ".GAMMA.-AMINOBUTYRIC ACID";

        ChemicalSubstance chemical = builder.addName(basicName)
                .setStructureWithDefaultReference("CCO")
                .build();
        StandardNameValidator validator = new StandardNameValidator();
        ValidationResponse<Substance> response = validator.validate(chemical, null);
        response.getValidationMessages().forEach(vm->{
            log.trace( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
        });
        
        Assertions.assertEquals(fullyStdName, chemical.names.get(0).stdName);
    }
}
