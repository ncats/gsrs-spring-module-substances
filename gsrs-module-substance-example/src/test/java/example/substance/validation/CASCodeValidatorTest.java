package example.substance.validation;

import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.CASUtilities;
import ix.ginas.utils.validation.validators.CASCodeValidator;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

/**
 *
 * @author mitch
 */
public class CASCodeValidatorTest extends AbstractSubstanceJpaEntityTest {

    public CASCodeValidatorTest() {
    }

    private static boolean configured = false;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @BeforeEach
    public void setup() {
        if (!configured) {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(CASCodeValidator.class);
            config.setNewObjClass(Substance.class);
            factory.addValidator("substances", config);
            configured = true;
        }
    }

    @Test
    public void CasValidationTest() throws Exception {
        //by default, as of 19 October 2021, STN ref checking is off
        ChemicalSubstance chemical = buildChemicalWithCasNoStn();
        ValidationResponse response = substanceEntityService.validateEntity(chemical.toFullJsonNode());
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        Assert.assertEquals(0, s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Must specify STN reference for CAS"))
                .count());
    }

    @Test
    public void CasValidationTestRefCheckOn() throws Exception {
        ChemicalSubstance chemical = buildChemicalWithCasNoStn();
        CASCodeValidator validator = new CASCodeValidator();
        validator.setPerformStnReferenceCheck(true);
        ValidationResponse response = validator.validate(chemical, null);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        Assert.assertEquals(1, s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Must specify STN reference for CAS"))
                .count());
    }

    @Test
    public void CasValidationTestOK() throws Exception {
        ChemicalSubstance chemical = buildChemicalWithCasAndStn();
        ValidationResponse response = substanceEntityService.validateEntity(chemical.toFullJsonNode());
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        Assert.assertEquals(0, s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("Must specify STN reference for CAS"))
                .count());
    }

    @Test
    public void CasValidationTest2() throws Exception {
        ChemicalSubstance chemical = buildChemicalWithLousyCasButStn();
        ValidationResponse response = substanceEntityService.validateEntity(chemical.toFullJsonNode());
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        long messageTotal = s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("does not have the expected format"))
                .count();
        long expectedMessageTotal = 1;
        Assert.assertEquals(expectedMessageTotal, messageTotal);
    }

    @Test
    public void CasValidationTest2FormatCheckOff() throws Exception {
        ChemicalSubstance chemical = buildChemicalWithLousyCasButStn();
        CASCodeValidator validator = new CASCodeValidator();
        validator.setPerformFormatCheck(false);
        ValidationResponse response = validator.validate(chemical, null);
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        long messageTotal = s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains("does not have the expected format"))
                .count();
        long expectedMessageTotal = 0;
        Assert.assertEquals(expectedMessageTotal, messageTotal);
    }

    @Test
    public void testisValidCas() {
        String casNum = "103-90-2";
        Assertions.assertTrue(CASUtilities.isValidCas(casNum));
    }

    @Test
    public void testisValidCasPadded() {
        String casNum = "51-66-1 ";
        Assertions.assertTrue(CASUtilities.isValidCas(casNum));
    }

    @Test
    public void testisValidCasFalse() {
        //check digit is off
        String casNum = "103-90-4";
        Assertions.assertFalse(CASUtilities.isValidCas(casNum));
    }

    @Test
    public void testisValidCasLetters() {
        //non-numeric char -> false
        String casNum = "A103-90-2";
        Assertions.assertFalse(CASUtilities.isValidCas(casNum));
    }

    @Test
    public void testisValidCasBlank() {
        //no check digit
        String casNum = "";
        Assertions.assertFalse(CASUtilities.isValidCas(casNum));
    }

    private ChemicalSubstance buildChemicalWithCasNoStn() {
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = "";
        Reference ref = new Reference();
        ref.citation = "475992-30-4";
        ref.docType = "ChemID";
        ref.url = "https://chem.nlm.nih.gov/chemidplus/rn/475992-30-4";
        ref.getOrGenerateUUID();

        Code casCode = new Code();
        casCode.setCode("475992-30-4");
        casCode.codeSystem = "CAS";
        casCode.addReference(ref.getUuid().toString());
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("chemical with CAS and issues")
                .setStructure(structure)
                .addReference(ref)
                .addCode(casCode);
        return builder.build();
    }

    private ChemicalSubstance buildChemicalWithCasAndStn() {
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = "";
        Reference ref = new Reference();
        ref.citation = "475992-30-4";
        ref.docType = "STN (SCIFINDER)";
        ref.url = "https://chem.nlm.nih.gov/chemidplus/rn/475992-30-4";
        ref.getOrGenerateUUID();

        Code casCode = new Code();
        casCode.setCode("475992-30-4");
        casCode.codeSystem = "CAS";
        casCode.addReference(ref.getUuid().toString());
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("chemical with CAS and issues")
                .setStructure(structure)
                .addReference(ref)
                .addCode(casCode);
        return builder.build();
    }

    private ChemicalSubstance buildChemicalWithLousyCasButStn() {
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = "";
        Reference ref = new Reference();
        ref.citation = "475992-30-four";
        ref.docType = "STN (SCIFINDER)";
        ref.url = "https://chem.nlm.nih.gov/chemidplus/rn/475992-30-4";
        ref.getOrGenerateUUID();

        Code casCode = new Code();
        casCode.setCode("475992-30-four");
        casCode.codeSystem = "CAS";
        casCode.addReference(ref.getUuid().toString());
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("chemical with CAS and issues")
                .setStructure(structure)
                .addReference(ref)
                .addCode(casCode);
        return builder.build();
    }

}
