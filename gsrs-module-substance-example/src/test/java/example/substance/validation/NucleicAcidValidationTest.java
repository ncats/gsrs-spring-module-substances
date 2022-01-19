package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import ix.ginas.models.v1.NucleicAcidSubstance;

import ix.ginas.models.v1.Substance;

import ix.ginas.utils.validation.validators.NucleicAcidValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import org.junit.jupiter.api.Assertions;

/**
 * Created by katzelda on 8/9/18.
 */
@WithMockUser(username = "admin", roles="Admin")
//@Ignore("don't have nucleic acid validator added yet since it uses seq search")
public class NucleicAcidValidationTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private TestGsrsValidatorFactory factory;
    @BeforeEach
    public void setup() {

        ValidatorConfig config = new DefaultValidatorConfig();
        config.setValidatorClass(NucleicAcidValidator.class);
        config.setNewObjClass(NucleicAcidSubstance.class);

        factory.addValidator("substances", config);
    }

    @Test
    public void mustHaveSubunit() throws Exception{
            JsonNode sub = new NucleicAcidSubstanceBuilder()
                    .addName("name")
                    .buildJson();

            ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub);
//            System.out.println(response.getMessages());
            assertFalse(response.isValid());

            assertTrue(response.getValidationMessages().stream()
                    .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR &&  m.getMessage().contains("Warning - Nucleic Acid substances usually have at least 1 subunit, but zero subunits were found here."))
                    .findAny().isPresent());



    }

     @Test
    public void flag0Subunit() throws Exception{
            Substance substance = new NucleicAcidSubstanceBuilder()
                    .addName("name")
                    .build();
            substance.definitionLevel = Substance.SubstanceDefinitionLevel.INCOMPLETE;
            JsonNode sub =substance.toFullJsonNode();

            ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub);
            //flipping this assertion because we changed errors to warning in case of incomplete def level
            assertTrue(response.isValid());

            Assertions.assertEquals(1, response.getValidationMessages().stream()
                    .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING &&  m.getMessage().contains("Warning - Nucleic Acid substances usually have at least 1 subunit, but zero subunits were found here.  This is discouraged and is only allowed for records labelled as incomplete"))
                    .count());
    }

    private void assertArrayNotEquals(byte[] expecteds, byte[] actuals) {
        try {
            assertArrayEquals(expecteds, actuals);
        } catch (AssertionError e) {
            return;
        }
        fail("The arrays are equal");
    }

    //TODO move these def hash tests to their own test class
/*
    @Test
    public void addingSubunitChangesDefinitionalHash(){
        String sequence = "ACGTACGTACGT";

        NucleicAcidSubstance sub = new NucleicAcidSubstanceBuilder()
                .addName("aName")
                .addDnaSubunit(sequence)
                .build();

        byte[] oldHash = sub.getDefinitionalElements().getDefinitionalHash();


        byte[] newHash = new NucleicAcidSubstanceBuilder(sub).addDnaSubunit("AAAAAAA").build().getDefinitionalElements().getDefinitionalHash();

        assertArrayNotEquals(oldHash, newHash);
    }

    @Test
    public void changingSubunitChangesDefinitionalHash(){
        String sequence = "ACGTACGTACGT";

        NucleicAcidSubstance sub = new NucleicAcidSubstanceBuilder()
                .addName("aName")
                .addDnaSubunit(sequence)
                .build();

        byte[] oldHash = sub.getDefinitionalElements().getDefinitionalHash();

        sub.nucleicAcid.getSubunits().get(0).sequence = sequence + "NN";

        byte[] newHash = sub.getDefinitionalElements().getDefinitionalHash();

        assertArrayNotEquals(oldHash, newHash);
    }
*/
    @Test
    public void dnaSeqMustReference() throws Exception{


            NucleicAcidSubstance sub = new NucleicAcidSubstanceBuilder()
                    .addName("name")
                    .addDnaSubunit("ACGTACGT")
                    .build();


            sub.nucleicAcid.setReferences(Collections.emptySet());
            sub.references.clear();

            ValidationResponse response = substanceEntityService.validateEntity(sub.toFullJsonNode());
//            assertFalse(response.isValid());

        Stream<ValidationMessage> stream = response.getValidationMessages().stream();
        assertTrue(response.getValidationMessages().toString(), stream
                    .filter(m-> /*m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR && */  m.getMessage().contains("needs at least 1 reference"))
                    .findAny().isPresent());



    }

    @Test
    public void validDna() throws Exception{


            NucleicAcidSubstance sub = new NucleicAcidSubstanceBuilder()
                    .addName("name")
                    .addDnaSubunit("ACGTACGT")
                    .build();



        ValidationResponse response = substanceEntityService.validateEntity(sub.toFullJsonNode());
        assertTrue(response.getValidationMessages().toString(), response.isValid());

        Stream<ValidationMessage> stream = response.getValidationMessages().stream();
        assertFalse(response.getValidationMessages().toString(), stream
                    .filter(m-> m.getMessage().contains("needs at least 1 reference"))
                    .findAny().isPresent());



    }
    @Test
    public void invalidDnaSequence() throws Exception{
            JsonNode sub = new NucleicAcidSubstanceBuilder()
                    .addName("name")
                    .addDnaSubunit("ACGT&&&NNT")
                    .buildJson();


        ValidationResponse response = substanceEntityService.validateEntity(sub);
        assertFalse(response.getValidationMessages().toString(), response.isValid());

        Stream<ValidationMessage> stream = response.getValidationMessages().stream();
        assertTrue(response.getValidationMessages().toString(), stream
                .filter(m-> m.getMessage().contains("invalid character"))
                .findAny().isPresent());




    }
    @Test
    public void invalidRnaSequence() throws Exception {
        JsonNode sub = new NucleicAcidSubstanceBuilder()
                .addName("name")
                .addRnaSubunit("ACGU&&&NNU")
                .buildJson();

        ValidationResponse response = substanceEntityService.validateEntity(sub);
        assertFalse(response.getValidationMessages().toString(), response.isValid());

        Stream<ValidationMessage> stream = response.getValidationMessages().stream();
        assertTrue(response.getValidationMessages().toString(), stream
                .filter(m -> m.getMessage().contains("invalid character"))
                .findAny().isPresent());

    }
}
