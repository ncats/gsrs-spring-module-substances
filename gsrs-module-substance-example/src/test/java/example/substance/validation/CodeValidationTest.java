package example.substance.validation;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import ix.core.models.Keyword;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.CodesValidator;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mitch
 */
@Slf4j
public class CodeValidationTest extends AbstractSubstanceJpaEntityTest {

    //private static boolean setup = false;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Test
    public void testCodeCommentWithSpaces() {
        Substance substance = createSimpleSubstance();
        String commentsBefore = "This is a real comment ";
        Code code1 = new Code();
        code1.code = "DB01190";
        code1.codeSystem = "Drug Bank";
        code1.comments = commentsBefore;
        substance.addCode(code1);
        CodesValidator validator = new CodesValidator();
        AutowireHelper.getInstance().autowire(validator);
        validator.validate(substance, null);
        String expectedComments = commentsBefore.trim();
        String commentsAfter = substance.codes.get(0).comments;
        Assertions.assertEquals(expectedComments, commentsAfter);
    }

    @Test
    public void testCodeWithSpaces() {
        Substance substance = createSimpleSubstance();
        String commentsBefore = "This is a real comment ";
        Code code1 = new Code();
        code1.code = "DB01190 ";
        code1.codeSystem = "Drug Bank";
        code1.comments = commentsBefore;
        substance.addCode(code1);
        CodesValidator validator = new CodesValidator();
        AutowireHelper.getInstance().autowire(validator);
        validator.validate(substance, null);
        String expectedComments = commentsBefore.trim();
        String commentsAfter = substance.codes.get(0).code;
        Assertions.assertNotEquals(expectedComments, commentsAfter);
    }

    @Test
    public void testCodeTextWithSpaces() {
        Substance substance = createSimpleSubstance();
        String commentsBefore = "This is a real comment ";
        Code code1 = new Code();
        code1.code = "DB01190 ";
        code1.codeSystem = "Drug Bank";
        code1.codeText = commentsBefore;
        substance.addCode(code1);
        CodesValidator validator = new CodesValidator();
        AutowireHelper.getInstance().autowire(validator);
        validator.validate(substance, null);
        String expectedComments = commentsBefore.trim();
        String commentsAfter = substance.codes.get(0).codeText;
        Assertions.assertNotEquals(expectedComments, commentsAfter);
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
    public void testcontainsLeadingTrailingSpaces1() {
        String noSpaces = "Just text";
        boolean expected = false;
        boolean actual = CodesValidator.containsLeadingTrailingSpaces(noSpaces);
        Assertions.assertEquals(expected, actual, "Must detect no trailing spaces");
    }

    @Test
    public void testcontainsLeadingTrailingSpaces2() {
        String noSpaces = "Not just text ";
        boolean expected = true;
        boolean actual = CodesValidator.containsLeadingTrailingSpaces(noSpaces);
        Assertions.assertEquals(expected, actual, "Must detect trailing spaces");
    }

    @Test
    public void testcontainsLeadingTrailingSpaces3() {
        String noSpaces = "Not just text |More data|another line";
        boolean expected = true;
        boolean actual = CodesValidator.containsLeadingTrailingSpaces(noSpaces);
        Assertions.assertEquals(expected, actual, "Must detect trailing spaces");
    }

}
