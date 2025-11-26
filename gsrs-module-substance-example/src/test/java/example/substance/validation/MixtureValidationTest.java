package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.service.GsrsEntityService;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.MixtureSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.MixtureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Created by katzelda on 7/20/18.
 */
@WithMockUser(username = "admin", roles="Admin")

public class MixtureValidationTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private TestGsrsValidatorFactory factory;

    Substance s1, s2, s3;

    @BeforeEach
    public void setup(){
        ValidatorConfig config = new DefaultValidatorConfig();
        config.setValidatorClass(MixtureValidator.class);
        config.setNewObjClass(MixtureSubstance.class);

        factory.addValidator("substances", config);

        s1 = new SubstanceBuilder()
                .addName("sub1")
                .generateNewUUID()
                .build();
        assertCreated(s1.toFullJsonNode());


        s2 = new SubstanceBuilder()
                .addName("sub2")
                .generateNewUUID()
                .build();
        assertCreated(s2.toFullJsonNode());

        s3 = new SubstanceBuilder()
                .addName("sub3")
                .generateNewUUID()
                .build();
        assertCreated(s3.toFullJsonNode());
    }



    @Test
    public void mixtureWith2OneOfsIsOK() throws Exception{

         new MixtureSubstanceBuilder()
                    .addName("foo")
                    .addComponents("MAY_BE_PRESENT_ONE_OF", s1, s2)
                    .buildJsonAnd(this::assertCreated);

    }

    @Test
    public void refUnregisteredSubstanceIsWarning() throws Exception{


            Substance unregistered = new SubstanceBuilder()
                    .addName("unreg")
                    .generateNewUUID()
                    .build();

            //didn't submit
            JsonNode toSubmit = new MixtureSubstanceBuilder()
                    .addName("foo")
                    .addComponents("MAY_BE_PRESENT_ONE_OF", s1, unregistered)
                    .buildJson();


            //can't actually submit this because currently the submission of
            //a passing substance doesn't have a way to easily expose the validation
            //messages.  looks like they are added as notes but the format is different
            //and some information is lost.  could possibly be able to write a lossy converter...

//            System.out.println("submit result = " + result);
            ValidationResponse response = substanceEntityService.validateEntity(toSubmit);

            //this is split up and stored as a variable for java 8 type inference to work...
            Stream<ValidationMessage>s1 = response.getValidationMessages().stream();

            assertTrue(s1
                    .filter(m ->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                    .map(ValidationMessage::getMessage)
                    .filter(m-> m.contains("not yet registered"))
                    .findAny().isPresent());


    }

    @Test
    public void mixtureMustHave2Components() throws Exception{

            JsonNode toSubmit = new MixtureSubstanceBuilder()
                    .addName("foo")
                    .addComponents("MUST_BE_PRESENT", s1)
                    .buildJson();

            GsrsEntityService.CreationResult<Substance> result = substanceEntityService.createEntity(toSubmit);
            assertFalse(result.isCreated());

            ValidationResponse response = result.getValidationResponse();



            assertFalse(response.isValid());
            //this is split up and stored as a variable for java 8 type inference to work...
            Stream<ValidationMessage>s1 = response.getValidationMessages().stream();

            assertTrue(s1
                    .filter(ValidationMessage::isError)
                    .map(ValidationMessage::getMessage)
                    .filter(m-> m.contains("at least 2"))
                    .findAny().isPresent());

    }

    @Test
    public void cantRefSameSubstanceTwiceSameType() throws Exception{


            JsonNode toSubmit = new MixtureSubstanceBuilder()
                    .addName("foo")

                    .addComponents("MAY_BE_PRESENT_ONE_OF", s1, s1)
                    .buildJson();

        GsrsEntityService.CreationResult<Substance> result = substanceEntityService.createEntity(toSubmit);
        assertFalse(result.isCreated());

        ValidationResponse response = result.getValidationResponse();


            assertFalse(response.isValid());
            //this is split up and stored as a variable for java 8 type inference to work...
            Stream<ValidationMessage>s1 = response.getValidationMessages().stream();

            assertTrue(s1
                    .filter(ValidationMessage::isError)
                    .map(ValidationMessage::getMessage)
                    .filter(m-> m.contains("Cannot reference the same mixture substance twice"))
                    .findAny().isPresent());



    }

    @Test
    public void mixtureMustAtLeast2OneOfComponents() throws Exception{


            JsonNode toSubmit = new MixtureSubstanceBuilder()
                    .addName("foo")
                    .addComponents("MUST_BE_PRESENT", s1)
                    .addComponents("MAY_BE_PRESENT_ONE_OF", s2)
                    .buildJson();

            GsrsEntityService.CreationResult<Substance> result = substanceEntityService.createEntity(toSubmit);
            assertFalse(result.isCreated());

            ValidationResponse response = result.getValidationResponse();




            assertFalse(response.isValid());
            //this is split up and stored as a variable for java 8 type inference to work...
            Stream<ValidationMessage>s1 = response.getValidationMessages().stream();

            assertTrue(s1
                    .filter(ValidationMessage::isError)
                    .map(ValidationMessage::getMessage)
                    .filter(m-> m.contains("Should have at least two \"One of\" components"))
                    .findAny().isPresent());



    }

    @Test
    public void mixtureComponentsTypeMustHaveNoTrailingBlanks() throws Exception{

        String inputComponentType = "MAY_BE_PRESENT_ONE_OF ";
        String componentTypeTrimmed =inputComponentType.trim();
        String inputComponentType2 = " MUST_BE_PRESENT";
        String componentType2Trimmed = inputComponentType2.trim();
        JsonNode toSubmit = new MixtureSubstanceBuilder()
                .addName("foo " + UUID.randomUUID())
                .addComponents(inputComponentType, s1)
                .addComponents(inputComponentType2, s2)
                .addComponents(componentTypeTrimmed, s3)
                .buildJson();

        GsrsEntityService.CreationResult<Substance> result = substanceEntityService.createEntity(toSubmit);

        ValidationResponse response = result.getValidationResponse();
        assertTrue(response.isValid());
        //this is split up and stored as a variable for java 8 type inference to work...
        Stream<ValidationMessage>s1 = response.getValidationMessages().stream();

        assertEquals(2, s1
                .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m-> m.contains(MixtureValidator.NO_LEAD_TRAIL_SPACES_IN_TYPE))
                .count());
        MixtureSubstance completed = (MixtureSubstance) result.getCreatedEntity();
        assertEquals(2, completed.mixture.components.stream().map(component -> component.type).filter(t->t.equals(componentTypeTrimmed)).count());
        assertEquals(1, completed.mixture.components.stream().map(component -> component.type).filter(t->t.equals(componentType2Trimmed)).count());
    }

}
