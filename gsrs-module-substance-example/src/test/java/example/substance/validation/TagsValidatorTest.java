package example.substance.validation;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.tags.TagUtilities;
import ix.ginas.utils.validation.validators.tags.TagsValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.HashSet;

@Slf4j

@TestPropertySource(properties = {
    "logging.level.example.substance.validation=info"
})
public class TagsValidatorTest extends AbstractSubstanceJpaEntityTest {

    @Test
    public void testTagsValidator1() {
        String testString = "These tags will be kept";
        Substance newSubstance = new Substance();
        newSubstance.names.add(new Name("D"));
        newSubstance.names.add(new Name("E [VANDF]"));
        newSubstance.names.add(new Name("F"));
        newSubstance.names.add(new Name("G"));
        newSubstance.addTagString("INN");
        newSubstance.addTagString("VANDF");
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsMissingFromNames(true);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);

        ValidationResponse<Substance> response = validator.validate(newSubstance, null);
        // response.getValidationMessages().forEach(vm->{
        //    log.info( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
        // });
        Assertions.assertEquals(TagUtilities.extractExplicitTags(newSubstance), new HashSet<>(Arrays.asList("INN", "VANDF")));

        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
    }

    @Test
    public void testTagsValidator2() {
        String testString = "Tags will be removed";
        Substance newSubstance = new Substance();
        newSubstance.names.add(new Name("D"));
        newSubstance.names.add(new Name("E [VANDF]"));
        newSubstance.names.add(new Name("F"));
        newSubstance.names.add(new Name("G"));
        newSubstance.addTagString("INN");
        newSubstance.addTagString("VANDF");
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsMissingFromNames(true);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(true);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);
        ValidationResponse<Substance> response = validator.validate(newSubstance, null);
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(newSubstance), new HashSet<>(Arrays.asList("VANDF")));
    }

    @Test
    public void testTagsValidator3() {
        String testString = "Tags WILL be added.";
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(true);
        validator.setCheckExplicitTagsMissingFromNames(false);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(true);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]" ));
        substance.names.add(new Name("Name 2 [CED]" ));
        substance.addTagString("ABC");
        ValidationResponse<Substance> response = validator.validate(substance, null);
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC", "CED")));
    }

    // begin extra name tags

    @Test
    public void testTagsValidator4() {
        String testString = "They will not be automatically added";
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(true);
        validator.setCheckExplicitTagsMissingFromNames(false);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false); // names; create; test when false
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]" ));
        substance.names.add(new Name("Name 2 [CED]" ));
        substance.addTagString("ABC");
        ValidationResponse<Substance> response = validator.validate(substance, null);
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC")));
    }

    @Test
    public void testTagsValidator5() {
        String testString = "Tags WILL be added.";
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(true);
        validator.setCheckExplicitTagsMissingFromNames(false);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(true); // names; create; test when true
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]" ));
        substance.names.add(new Name("Name 2 [CED]" ));
        substance.addTagString("ABC");
        ValidationResponse<Substance> response = validator.validate(substance, null);
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC", "CED")));

    }

    @Test
    public void testTagsValidator6() {
        String testString = "They will not be automatically added";
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(true);
        validator.setCheckExplicitTagsMissingFromNames(false);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false); // names; update; test when false
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]" ));
        substance.names.add(new Name("Name 2 [CED]" ));
        substance.addTagString("ABC");

        Substance oldSubstance = new Substance();
        oldSubstance.names.add(new Name("Name 1 [ABC]" ));
        oldSubstance.names.add(new Name("Name 2 [CED]" ));
        oldSubstance.names.add(new Name("Name 2 [KGF]" ));
        oldSubstance.addTagString("ABC");
        ValidationResponse<Substance> response = validator.validate(substance, oldSubstance);
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC")));

    }

    @Test
    public void testTagsValidator7() {
        String testString = "Tags WILL be added.";
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(true);
        validator.setCheckExplicitTagsMissingFromNames(false);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(true); // names; update; test when true
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]"));
        substance.names.add(new Name("Name 2 [CED]"));
        substance.addTagString("ABC");
        Substance oldSubstance = new Substance();
        oldSubstance.names.add(new Name("Name 1 [ABC]"));
        oldSubstance.names.add(new Name("Name 2 [CED]"));
        oldSubstance.names.add(new Name("Name 2 [KGF]"));
        oldSubstance.addTagString("ABC");
        ValidationResponse<Substance> response = validator.validate(substance, oldSubstance);
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC", "CED")));
    }

    // end extra name tags

    // begin extra tags

    @Test
    public void testTagsValidator8() {
        String testString = "These tags will be kept";
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(false);
        validator.setCheckExplicitTagsMissingFromNames(true);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false); // extra tags; create; test when false
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]"));
        substance.addTagString("ABC");
        substance.addTagString("ZEF");

        ValidationResponse<Substance> response = validator.validate(substance, null);
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC", "ZEF")));
    }

    @Test
    public void testTagsValidator9() {
        String testString = "Tags will be removed";
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(false);
        validator.setCheckExplicitTagsMissingFromNames(true);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(true); // extra tags; create; test when true
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]" ));
        substance.addTagString("ABC");
        substance.addTagString("ZEF");
        ValidationResponse<Substance> response = validator.validate(substance, null);
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC")));
    }

    @Test
    public void testTagsValidator10() {
        String testString = "These tags will be kept";
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(false);
        validator.setCheckExplicitTagsMissingFromNames(true);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false); // extra tags; update; test when false
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]" ));
        substance.addTagString("ABC");
        substance.addTagString("ZEF");

        Substance oldSubstance = new Substance();
        oldSubstance.names.add(new Name("Name 1 [ABC]" ));
        oldSubstance.names.add(new Name("Name 2 [CED]" ));
        oldSubstance.names.add(new Name("Name 2 [KGF]" ));
        oldSubstance.addTagString("ABC");
        ValidationResponse<Substance> response = validator.validate(substance, oldSubstance);
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC","ZEF")));
    }

    @Test
    public void testTagsValidator11() {
        String testString = "Tags will be removed";
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(false);
        validator.setCheckExplicitTagsMissingFromNames(true);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(true); // extra tags; update; test when true
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]" ));
        substance.addTagString("ABC");
        substance.addTagString("ZEF");

        Substance oldSubstance = new Substance();
        oldSubstance.names.add(new Name("Name 1 [ABC]"));
        oldSubstance.names.add(new Name("Name 2 [CED]"));
        oldSubstance.names.add(new Name("Name 2 [KGF]"));
        oldSubstance.addTagString("ABC");
        ValidationResponse<Substance> response = validator.validate(substance, oldSubstance);
        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(testString)).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC")));
    }

    // end extra tags

    @Test
    public void testTagsValidator12() {
        // No validation when check is off.
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(false); // names; test when check off
        validator.setCheckExplicitTagsMissingFromNames(false);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(true); // names; update; test when true
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]" ));
        substance.addTagString("ABC");
        substance.addTagString("ZEF");

        Substance oldSubstance = new Substance();
        oldSubstance.names.add(new Name("Name 1 [ABC]"));
        oldSubstance.names.add(new Name("Name 2 [CED]"));
        oldSubstance.names.add(new Name("Name 2 [KGF]"));
        oldSubstance.addTagString("ABC");
        ValidationResponse<Substance> response = validator.validate(substance, oldSubstance);
        // Should get 0 validation messages
        Assertions.assertEquals(0, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains(" ")).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC","ZEF")));
    }

    @Test
    public void testTagsValidator13() {
        // No validation when check is off.
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(false);
        validator.setCheckExplicitTagsMissingFromNames(false); // extra tags; test when check off
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(true); // extra tags; update; test when true
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]" ));
        substance.addTagString("ABC");
        substance.addTagString("ZEF");

        Substance oldSubstance = new Substance();
        oldSubstance.names.add(new Name("Name 1 [ABC]"));
        oldSubstance.names.add(new Name("Name 2 [CED]"));
        oldSubstance.names.add(new Name("Name 2 [KGF]"));
        oldSubstance.addTagString("ABC");
        ValidationResponse<Substance> response = validator.validate(substance, oldSubstance);
        // Should get 0 validation messages
        Assertions.assertEquals(0, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("")).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC","ZEF")));
    }

    @Test
    public void testTagsValidator14() {
        // update, testing more the one bracket term in name.
        TagsValidator validator = new TagsValidator();
        validator.setCheckExplicitTagsExtractedFromNames(true);
        validator.setCheckExplicitTagsMissingFromNames(true);
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(true);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(true);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(true);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(true);
        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC][TANGO][B-611][EDU:ALL]" ));
        substance.addTagString("ABC");
        substance.addTagString("ZEF");

        Substance oldSubstance = new Substance();
        oldSubstance.names.add(new Name("Name 1"));
        ValidationResponse<Substance> response = validator.validate(substance, oldSubstance);

        Assertions.assertEquals(2, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("")).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("ABC","TANGO","B-611","EDU","ALL")));
    }


}
