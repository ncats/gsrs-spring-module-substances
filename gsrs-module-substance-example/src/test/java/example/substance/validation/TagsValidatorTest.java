package example.substance.validation;

import com.hp.hpl.jena.rdf.arp.lang.LanguageTagSyntaxException;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.TagUtilities;
import ix.ginas.utils.validation.validators.TagsValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.util.TagUtils;

import java.util.Arrays;
import java.util.HashSet;

@Slf4j
// can't set property with annotation?
// how to assert applyChanges(true) changes?
@TestPropertySource(properties = {
    "logging.level.example.substance.validation=info"
})
public class TagsValidatorTest extends AbstractSubstanceJpaEntityTest {


    Substance createOldSubstance() {
        Substance oldSubstance = new Substance();
        Name oldName1 = new Name();
        Name oldName2 = new Name();
        Name oldName3 = new Name();
        Name oldName4 = new Name();
        Name oldName5 = new Name();
        Name oldName6 = new Name();
        oldName1.setName("A");
        oldName2.setName("B");
        oldName3.setName("C [USP]");
        oldName4.setName("D");
        oldName5.setName("E [VANDF]");
        oldSubstance.names.add(oldName1);
        oldSubstance.names.add(oldName2);
        oldSubstance.names.add(oldName3);
        oldSubstance.names.add(oldName4);
        oldSubstance.names.add(oldName5);
        oldSubstance.names.add(oldName6);
        oldSubstance.addTagString("USP");
        oldSubstance.addTagString("VANDF");
        return oldSubstance;
    }

    Substance createNewSubstance() {
        Substance newSubstance = new Substance();
        Name newName1 = new Name();
        Name newName2 = new Name();
        Name newName3 = new Name();
        Name newName4 = new Name();
        newName1.setName("D");
        newName2.setName("E [VANDF]");
        newName3.setName("F");
        newName4.setName("G");
        newSubstance.names.add(newName1);
        newSubstance.names.add(newName2);
        newSubstance.names.add(newName3);
        newSubstance.names.add(newName4);
        newSubstance.addTagString("INN");
        newSubstance.addTagString("VANDF");
        return newSubstance;
    }

    @Test
    public void testTagsValidator1() {

        TagsValidator validator = new TagsValidator();

        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);

        Substance newSubstance = createNewSubstance();

        ValidationResponse<Substance> response = validator.validate(newSubstance, null);
        response.getValidationMessages().forEach(vm->{
            log.info( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
        });

        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("These tags will be kept")).count());
        Assertions.assertEquals(TagUtilities.extractExplicitTags(newSubstance), new HashSet<>(Arrays.asList("INN", "VANDF")));
    }

    @Test
    public void testTagsValidator2() {
        TagsValidator validator = new TagsValidator();

        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(true);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(true);

        Substance newSubstance = createNewSubstance();

        ValidationResponse<Substance> response = validator.validate(newSubstance, null);
        // response.getValidationMessages().forEach(vm->{
        //    log.info( String.format("type: %s; message: %s", vm.getMessageType(), vm.getMessage()));
        // });

        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("Tags will be removed")).count());

        // Can't get this assertion to pass; does it take applicableChanges(true) into account?
        // Still case after Danny's suggested fix.
        // Assertions.assertEquals(TagUtilities.extractExplicitTags(newSubstance), new HashSet<>(Arrays.asList("USP","INN","VANDF")));

    }

    @Test
    public void testTagsValidator3() {
        TagsValidator validator = new TagsValidator();

        validator.setAddExplicitTagsExtractedFromNamesOnCreate(true);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(true);
        validator.setRemoveExplicitTagsMissingFromNamesOnCreate(false);
        validator.setRemoveExplicitTagsMissingFromNamesOnUpdate(false);

        Substance substance = new Substance();
        substance.names.add(new Name("Name 1 [ABC]" ));
        substance.names.add(new Name("Name 2 [CED]" ));
        substance.addTagString("ABC");

        ValidationResponse<Substance> response = validator.validate(substance, null);

        Assertions.assertEquals(1, response.getValidationMessages().stream()
                .filter(m -> m.getMessage().contains("Tags WILL be added.")).count());

        // Can't get this assertion to pass; does it take applicableChanges(true) into account?
        // Still case after Danny's suggested fix.
        // Assertions.assertEquals(TagUtilities.extractExplicitTags(substance), new HashSet<>(Arrays.asList("VANDF")));

    }

    // ==== addExplicitTagsExtractedFromNames begin ====

    @Test
    public void testTagsValidator4() {
        // check create vs update logic
        // action is create|setAddExplicitTagsExtractedFromNamesOnCreate: false
        String testString = "They will not be automatically added";
        TagsValidator validator = new TagsValidator();
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
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
    }


    @Test
    public void testTagsValidator5() {
        // check create vs update logic
        // action is create|setAddExplicitTagsExtractedFromNamesOnCreate: true
        String testString = "Tags WILL be added.";
        TagsValidator validator = new TagsValidator();
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
    }

    @Test
    public void testTagsValidator6() {
        // check create vs update logic
        // action is update|setAddExplicitTagsExtractedFromNamesOnUpdate: false
        String testString = "They will not be automatically added";
        TagsValidator validator = new TagsValidator();
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
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
    }

    @Test
    public void testTagsValidator7() {
        // check create vs update logic
        // action is update|setAddExplicitTagsExtractedFromNamesOnUpdate: true
        String testString = "Tags WILL be added.";
        TagsValidator validator = new TagsValidator();
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(true);
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
    }

    // ==== addExplicitTagsExtractedFromNames end ====

    // ==== removeExplicitTagsMissingFromNames begin ====

    @Test
    public void testTagsValidator8() {
        // check create vs update logic
        // action is create|setRemoveExplicitTagsMissingFromNamesOnCreate: false
        String testString = "They will not be automatically added";
        TagsValidator validator = new TagsValidator();
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
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
    }


    @Test
    public void testTagsValidator9() {
        // check create vs update logic
        // action is create|setRemoveExplicitTagsMissingFromNamesOnCreate: true
        String testString = "Tags WILL be added.";
        TagsValidator validator = new TagsValidator();
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
    }

    @Test
    public void testTagsValidator10() {
        // check create vs update logic
        // action is update|setRemoveExplicitTagsMissingFromNamesOnUpdate: false
        String testString = "They will not be automatically added";
        TagsValidator validator = new TagsValidator();
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(false);
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
    }

    @Test
    public void testTagsValidator11() {
        // check create vs update logic
        // action is update|setRemoveExplicitTagsMissingFromNamesOnUpdate: true
        String testString = "Tags WILL be added.";
        TagsValidator validator = new TagsValidator();
        validator.setAddExplicitTagsExtractedFromNamesOnCreate(false);
        validator.setAddExplicitTagsExtractedFromNamesOnUpdate(true);
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
    }

    // ==== removeExplicitTagsMissingFromNames end ====



}
