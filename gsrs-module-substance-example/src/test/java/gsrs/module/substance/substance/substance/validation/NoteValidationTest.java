package gsrs.module.substance.substance.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.module.substance.substance.substance.AbstractSubstanceJpaEntityTest;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Substance;

import ix.ginas.utils.validation.validators.NotesValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class NoteValidationTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private TestGsrsValidatorFactory factory;

    @BeforeEach
    public void setup(){
        ValidatorConfig config = new DefaultValidatorConfig();
        config.setValidatorClass(NotesValidator.class);
        config.setNewObjClass(Substance.class);

        factory.addValidator("substances", config);
    }
    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void nullNotesAreRemoved() throws Exception{

            JsonNode json = new SubstanceBuilder()
                    .addName("aName", name -> name.addLanguage("en"))
                    .addNote(null)
                    .buildJson();

        ValidationResponse response = substanceEntityService.validateEntity(json);

        Stream<ValidationMessage> stream = response.getValidationMessages().stream();
        assertTrue(stream
                    .filter(m -> "Null note objects are not allowed".equals(m.getMessage()))
                    .findAny()
                    .isPresent());

        Substance submittedSubstance = substanceEntityService.createEntity(json).getCreatedEntity();

        assertFalse(submittedSubstance.notes.stream().filter(Objects::isNull).findAny().isPresent());


    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void nullNotesStringChangedToEmptyString() throws Exception{

            Note note = new Note();
            note.note= null;
            JsonNode json = new SubstanceBuilder()
                    .addName("aName", name -> name.addLanguage("en"))
                    .addNote(note)
                    .buildJson();

        ValidationResponse response = substanceEntityService.validateEntity(json);

        Stream<ValidationMessage> stream = response.getValidationMessages().stream();


        assertTrue(stream
                    .filter(m -> "Note objects must have a populated note field. Setting to empty String".equals(m.getMessage()))
                    .findAny()
                    .isPresent(),
                response.getValidationMessages().toString());

        Substance submittedSubstance = substanceEntityService.createEntity(json).getCreatedEntity();

        String noteString =submittedSubstance.notes.get(0).note;
            assertEquals("", noteString);


    }
}
