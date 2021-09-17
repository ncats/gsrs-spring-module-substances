package example.substance.validation;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.CASCodeValidator;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
            System.out.println("configured!");
        }
    }

    @Test
    public void CasValidationTest() throws Exception {
        ChemicalSubstance chemical = buildChemicalWithCasNoStn();
        ValidationResponse response = substanceEntityService.validateEntity(chemical.toFullJsonNode());
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
        long expectedMessageTotal =1;
        Assert.assertEquals(expectedMessageTotal, messageTotal);
    }

    @Test
    public void testisValidCas() {
        String casNum = "103-90-2";
        Assertions.assertTrue(CASCodeValidator.isValidCas(casNum));
    }

    @Test
    public void testisValidCasPadded() {
        String casNum = "51-66-1 ";
        Assertions.assertTrue(CASCodeValidator.isValidCas(casNum));
    }

    @Test
    public void testisValidCasFalse() {
        //check digit is off
        String casNum = "103-90-4";
        Assertions.assertFalse(CASCodeValidator.isValidCas(casNum));
    }

    @Test
    public void testisValidCasLetters() {
        //check digit is off
        String casNum = "A103-90-2";
        Assertions.assertFalse(CASCodeValidator.isValidCas(casNum));
    }

    @Test
    public void testisValidCasBlank() {
        //check digit is off
        String casNum = "";
        Assertions.assertFalse(CASCodeValidator.isValidCas(casNum));
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
