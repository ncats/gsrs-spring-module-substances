package example.substance.validation;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Name;
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

    @Test
    public void testStandardNameValidation_defaultError() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String basicName = "1,2-CHLORO [3,5]BENZENE [INCI]";
        String fullyBogusStdName = "1,2-CHLORO [3,5]BENZENE [INCI]";

        Name nameToTest = new Name();
        nameToTest.name= basicName;
        nameToTest.stdName=fullyBogusStdName;
        ChemicalSubstance chemical = builder.addName(nameToTest)
                .setStructureWithDefaultReference("c1([Cl])c([Cl])cccc1")
                .build();
        StandardNameValidator validator = new StandardNameValidator();
        ValidationResponse<Substance> response = validator.validate(chemical, null);
        response.getValidationMessages().forEach(vm->{
            log.trace( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
        });

        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(m->m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.ERROR)));
    }

    @Test
    public void testStandardNameValidation_setWarn() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String basicName = "1,2-CHLORO [3,5]BENZENE [INCI]";
        String fullyBogusStdName = "1,2-CHLORO [3,5]BENZENE [INCI]";

        Name nameToTest = new Name();
        nameToTest.name= basicName;
        nameToTest.stdName=fullyBogusStdName;
        ChemicalSubstance chemical = builder.addName(nameToTest)
                .setStructureWithDefaultReference("c1([Cl])c([Cl])cccc1")
                .build();
        StandardNameValidator validator = new StandardNameValidator();
        validator.setBehaviorOnInvalidStdName("warn");
        ValidationResponse<Substance> response = validator.validate(chemical, null);
        response.getValidationMessages().forEach(vm->{
            log.trace( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
        });

        Assertions.assertTrue(response.getValidationMessages().stream().noneMatch(m->m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.ERROR)));
    }

    @Test
    public void testStandardNameValidation_setError() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String basicName = "1,2-CHLORO [3,5]BENZENE [INCI]";
        String fullyBogusStdName = "1,2-CHLORO [3,5]BENZENE [INCI]";

        Name nameToTest = new Name();
        nameToTest.name= basicName;
        nameToTest.stdName=fullyBogusStdName;
        ChemicalSubstance chemical = builder.addName(nameToTest)
                .setStructureWithDefaultReference("c1([Cl])c([Cl])cccc1")
                .build();
        StandardNameValidator validator = new StandardNameValidator();
        validator.setBehaviorOnInvalidStdName("error");
        ValidationResponse<Substance> response = validator.validate(chemical, null);
        response.getValidationMessages().forEach(vm->{
            log.trace( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
        });

        Assertions.assertTrue(response.getValidationMessages().stream().anyMatch(m->m.getMessageType().equals(ValidationMessage.MESSAGE_TYPE.ERROR)));
    }

    @Test
    public void testStandardNameValidation_setNonsense() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        String basicName = "1,2-CHLORO [3,5]BENZENE [INCI]";
        String fullyBogusStdName = "1,2-CHLORO [3,5]BENZENE [INCI]";

        Name nameToTest = new Name();
        nameToTest.name= basicName;
        nameToTest.stdName=fullyBogusStdName;
        ChemicalSubstance chemical = builder.addName(nameToTest)
                .setStructureWithDefaultReference("c1([Cl])c([Cl])cccc1")
                .build();
        StandardNameValidator validator = new StandardNameValidator();
        Exception ex=null;
        try {
            validator.setBehaviorOnInvalidStdName("nonsense");
        }
        catch (Exception exCaught) {
            ex=exCaught;
        }
        Assertions.assertNotNull(ex,"Must throw an exception when setting an invalid value");
    }
}
