package example.substance.validation;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Keyword;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.CodeUniquenessValidator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author mitch
 */
@Slf4j
public class CodeUniquenessValidatorTest extends AbstractSubstanceJpaEntityTest {

    @Test
    public void testCodeDuplicateCheckConfirmed() {
        Substance substance = createSimpleSubstance();
        Code code1 = new Code();
        code1.code = "DB01190";
        code1.codeSystem = "Drug Bank";
        code1.type = "PRIMARY";
        substance.addCode(code1);
        substanceRepository.saveAndFlush(substance);

        Substance substance2 = createSimpleSubstance();
        Code code2 = new Code();
        code2.code = "DB01190";
        code2.codeSystem = "Drug Bank";
        code2.type = "PRIMARY";
        substance2.addCode(code2);

        CodeUniquenessValidator validator = new CodeUniquenessValidator();
        LinkedHashMap<Integer, String> singletons = new LinkedHashMap<>();
        singletons.put(1, code1.codeSystem);
        validator.setCodeSystemsForWarning(singletons);
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse<Substance> response = validator.validate(substance2, null);
        log.trace("messages:");
        response.getValidationMessages().forEach(m -> log.trace(String.format("type: %s; text: %s", m.getMessageType(), m.getMessage())));
        long totalDuplicates = response.getValidationMessages()
                .stream()
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING && m.getMessage().contains("collides (possible duplicate) with existing code "))
                .count();
        long expectedDuplicates = 1;
        Assertions.assertEquals(expectedDuplicates, totalDuplicates);
    }

    @Test
    public void testCodeDuplicateCheckAvoided() {
        Substance substance = createSimpleSubstance();
        Code code1 = new Code();
        code1.code = "DB01190";
        code1.codeSystem = "Drug Bank";
        code1.type = "PRIMARY";
        substance.addCode(code1);
        substanceRepository.saveAndFlush(substance);

        Substance substance2 = createSimpleSubstance();
        Code code2 = new Code();
        code2.code = "DB01190";
        code2.codeSystem = "Drug Bank";
        code2.type = "PRIMARY";
        substance2.addCode(code2);

        CodeUniquenessValidator validator = new CodeUniquenessValidator();
        LinkedHashMap<Integer, String> singletons = new LinkedHashMap<>();
        singletons.put(1, "others");//bogus value; will prevent 'Drug Bank' codes from being duplicate checked
        validator.setCodeSystemsForWarning(singletons);
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse<Substance> response = validator.validate(substance2, null);
        log.trace("messages:");
        response.getValidationMessages().forEach(m -> log.trace(String.format("type: %s; text: %s", m.getMessageType(), m.getMessage())));
        long totalDuplicates = response.getValidationMessages()
                .stream()
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING && m.getMessage().contains("collides (possible duplicate) with existing code "))
                .count();
        long expectedDuplicates = 0;
        Assertions.assertEquals(expectedDuplicates, totalDuplicates);
    }

    @Test
    public void testCodeDuplicateCheckAvoidedNotPrimary() {
        Substance substance = createSimpleSubstance();
        Code code1 = new Code();
        code1.code = "DB01190";
        code1.codeSystem = "Drug Bank";
        code1.type = "PRIMARY";
        substance.addCode(code1);
        substanceRepository.saveAndFlush(substance);

        Substance substance2 = createSimpleSubstance();
        Code code2 = new Code();
        code2.code = "DB01190";
        code2.codeSystem = "Drug Bank";
        code2.type = "generic";
        substance2.addCode(code2);

        CodeUniquenessValidator validator = new CodeUniquenessValidator();
        LinkedHashMap<Integer, String> singletons = new LinkedHashMap<>();
        singletons.put(1, "others");//bogus value; will prevent 'Drug Bank' codes from being duplicate checked
        validator.setCodeSystemsForWarning(singletons);
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse<Substance> response = validator.validate(substance2, null);
        log.trace("messages:");
        response.getValidationMessages().forEach(m -> log.trace(String.format("type: %s; text: %s", m.getMessageType(), m.getMessage())));
        long totalDuplicates = response.getValidationMessages()
                .stream()
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING && m.getMessage().contains("collides (possible duplicate) with existing code "))
                .count();
        long expectedDuplicates = 0;
        Assertions.assertEquals(expectedDuplicates, totalDuplicates);
    }

    private Substance createSimpleSubstance() {
        SubstanceBuilder builder = new SubstanceBuilder();
        Reference ref1 = new Reference();
        ref1.docType = "Notes";
        ref1.citation = "Reference 1";
        ref1.publicDomain = true;
        ref1.setAccess(new HashSet<>());

        Name name1 = new Name();
        name1.name = "Name 1";
        name1.displayName = true;
        name1.type = "cn";
        name1.languages = new EmbeddedKeywordList();
        name1.languages.add(new Keyword("English"));
        name1.addReference(ref1);

        Name name2 = new Name();
        name2.name = "Name 2";
        name2.displayName = false;
        name2.type = "cn";
        name2.languages = new EmbeddedKeywordList();
        name2.languages.add(new Keyword("English"));
        name2.addReference(ref1);

        builder
                .addReference(ref1)
                .addName(name1)
                .addName(name2);

        Substance substance1 = builder.build();
        return substance1;
    }

    @Test
    public void testCodeDuplicate() {
        Substance substance = createSimpleSubstance();
        Code code1 = new Code();
        code1.code = "DB01190";
        code1.codeSystem = "Drug Bank";
        substance.addCode(code1);
        substanceRepository.saveAndFlush(substance);

        Substance substance2 = createSimpleSubstance();
        Code code2 = new Code();
        code2.code = "DB01190";
        code2.codeSystem = "Drug Bank";
        substance2.addCode(code2);

        CodeUniquenessValidator validator = new CodeUniquenessValidator();
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse<Substance> response = validator.validate(substance2, null);
        long totalDuplicates = response.getValidationMessages()
                .stream()
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING && m.getMessage().contains("collides (possible duplicate) with existing code "))
                .count();
        long expectedDuplicates = 1;
        Assertions.assertEquals(expectedDuplicates, totalDuplicates);
    }


    @Test
    public void testCodeDuplicateCheckError() {
        Substance substance = createSimpleSubstance();
        Code code1 = new Code();
        code1.code = "DB01190";
        code1.codeSystem = "Drug Bank";
        code1.type = "PRIMARY";
        substance.addCode(code1);
        substanceRepository.saveAndFlush(substance);

        Substance substance2 = createSimpleSubstance();
        Code code2 = new Code();
        code2.code = "DB01190";
        code2.codeSystem = "Drug Bank";
        code2.type = "PRIMARY";
        substance2.addCode(code2);

        CodeUniquenessValidator validator = new CodeUniquenessValidator();
        LinkedHashMap<Integer, String> singletons = new LinkedHashMap<>();
        singletons.put(1, "CAS");
        validator.setCodeSystemsForWarning(singletons);
        LinkedHashMap<Integer, String> uniqueOnes = new LinkedHashMap<>();
        uniqueOnes.put(1, code1.codeSystem);
        validator.setCodeSystemsForError(uniqueOnes);
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse<Substance> response = validator.validate(substance2, null);
        long totalDuplicates = response.getValidationMessages()
                .stream()
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR && m.getMessage().contains("is a duplicate of existing code & codeSystem for substance"))
                .count();
        long expectedDuplicates = 1;
        Assertions.assertEquals(expectedDuplicates, totalDuplicates);
    }

    @Test
    public void testCodeDuplicateCheckErrorNot() {
        Substance substance = createSimpleSubstance();
        Code code1 = new Code();
        code1.code = "DB01190";
        code1.codeSystem = "Drug Bank";
        code1.type = "PRIMARY";
        substance.addCode(code1);
        substanceRepository.saveAndFlush(substance);

        Substance substance2 = createSimpleSubstance();
        Code code2 = new Code();
        code2.code = "DB01190";
        code2.codeSystem = "Drug Bank";
        code2.type = "ALTERNATIVE";
        substance2.addCode(code2);

        CodeUniquenessValidator validator = new CodeUniquenessValidator();
        LinkedHashMap<Integer, String> singletons = new LinkedHashMap<>();
        singletons.put(1, "CAS");
        validator.setCodeSystemsForWarning(singletons);
        LinkedHashMap<Integer, String> uniqueOnes = new LinkedHashMap<>();
        uniqueOnes.put(1, code1.codeSystem);
        validator.setCodeSystemsForError(uniqueOnes);
        AutowireHelper.getInstance().autowire(validator);
        ValidationResponse<Substance> response = validator.validate(substance2, null);
        log.trace("messages:");
        response.getValidationMessages().forEach(m -> log.trace(String.format("type: %s; text: %s", m.getMessageType(), m.getMessage())));
        long totalDuplicates = response.getValidationMessages()
                .stream()
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR && m.getMessage().contains("is a duplicate of existing code & codeSystem for substance"))
                .count();
        long expectedDuplicates = 0;
        Assertions.assertEquals(expectedDuplicates, totalDuplicates);
    }

    
}
