package example.substance.validation;

import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.validation.validators.HybridizationFeatureValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@WithMockUser(username = "admin", roles="Admin")
public class HybridizationFeatureValidatorTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private TestGsrsValidatorFactory factory;
    @BeforeEach
    public void setup() {

        ValidatorConfig config = new DefaultValidatorConfig();
        config.setValidatorClass(HybridizationFeatureValidator.class);
        config.setNewObjClass(NucleicAcidSubstance.class);

        factory.addValidator("substances", config);
    }

    @Test
    public void mustHaveComplementarySequences() throws Exception{
        Property hybridizationProperty = new Property();
        hybridizationProperty.setName(HybridizationFeatureValidator.COMPLEMENTARY_REGION_PROPERTY_NAME);
        Amount hybridizationValue = new Amount();
        hybridizationValue.nonNumericValue= "1_1-1_3;2_1-2_3";
        hybridizationProperty.setValue(hybridizationValue);

        NucleicAcidSubstance sub  = new NucleicAcidSubstanceBuilder()
                .addName("name")
                .addDnaSubunit("ATA")
                .addProperty(hybridizationProperty)
                .build();
        Subunit subunit = new Subunit();
        subunit.subunitIndex = 2;
        subunit.sequence = "TAT";
        sub.nucleicAcid.subunits = new ArrayList<>(sub.nucleicAcid.subunits);
        sub.nucleicAcid.subunits.add(subunit);

        ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub.toFullJsonNode());
        assertTrue(response.isValid());

        assertTrue(response.getValidationMessages().stream()
                .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR || m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING  )
                .findAny().isEmpty());
    }

    @Test
    public void mustHaveComplementarySequences2() throws Exception{
        Property hybridizationProperty = new Property();
        hybridizationProperty.setName(HybridizationFeatureValidator.COMPLEMENTARY_REGION_PROPERTY_NAME);
        Amount hybridizationValue = new Amount();
        hybridizationValue.nonNumericValue= "1_1-1_3;2_1-2_3;1_5-1_9;2_5-2_9";
        hybridizationProperty.setValue(hybridizationValue);

        NucleicAcidSubstance sub  = new NucleicAcidSubstanceBuilder()
                .addName("name")
                .addDnaSubunit("ATAUAAAGGG")
                .addProperty(hybridizationProperty)
                .build();
        Subunit subunit = new Subunit();
        subunit.subunitIndex = 2;
        subunit.sequence = "TATUTTTCCC";
        sub.nucleicAcid.subunits = new ArrayList<>(sub.nucleicAcid.subunits);
        sub.nucleicAcid.subunits.add(subunit);

        ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub.toFullJsonNode());
        assertTrue(response.isValid());

        assertTrue(response.getValidationMessages().stream()
                .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR || m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING  )
                .findAny().isEmpty());
    }

    @Test
    public void referencingNonExistentSubunitGivesError() throws Exception{
        Property hybridizationProperty = new Property();
        hybridizationProperty.setName(HybridizationFeatureValidator.COMPLEMENTARY_REGION_PROPERTY_NAME);
        Amount hybridizationValue = new Amount();
        hybridizationValue.nonNumericValue= "1_1-1_3;2_1-2_3;1_5-1_9;4_5-4_9";
        hybridizationProperty.setValue(hybridizationValue);

        NucleicAcidSubstance sub  = new NucleicAcidSubstanceBuilder()
                .addName("name")
                .addDnaSubunit("ATAUAAAGGG")
                .addProperty(hybridizationProperty)
                .build();
        Subunit subunit = new Subunit();
        subunit.subunitIndex = 2;
        subunit.sequence = "TATUTTTCCC";
        sub.nucleicAcid.subunits = new ArrayList<>(sub.nucleicAcid.subunits);
        sub.nucleicAcid.subunits.add(subunit);

        ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub.toFullJsonNode());
        assertFalse(response.isValid());

        assertTrue(response.getValidationMessages().stream()
                .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR )
                .findAny().isPresent());
    }

    @Test
    public void mustHaveComplementarySequencesAndDoesNot() throws Exception{
        Property hybridizationProperty = new Property();
        hybridizationProperty.setName(HybridizationFeatureValidator.COMPLEMENTARY_REGION_PROPERTY_NAME);
        Amount hybridizationValue = new Amount();
        hybridizationValue.nonNumericValue= "1_1-1_3;2_1-2_3";
        hybridizationProperty.setValue(hybridizationValue);

        NucleicAcidSubstance sub  = new NucleicAcidSubstanceBuilder()
                .addName("name")
                .addDnaSubunit("ATG")
                .addProperty(hybridizationProperty)
                .build();
        Subunit subunit = new Subunit();
        subunit.subunitIndex = 2;
        subunit.sequence = "TAT";
        sub.nucleicAcid.subunits = new ArrayList<>(sub.nucleicAcid.subunits);
        sub.nucleicAcid.subunits.add(subunit);

        ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub.toFullJsonNode());
        assertTrue(response.isValid());

        assertTrue(response.getValidationMessages().stream()
                .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING )
                .findAny().isPresent());
    }

    @Test
    public void mustHaveSimilarSizedSites() throws Exception{
        Property hybridizationProperty = new Property();
        hybridizationProperty.setName(HybridizationFeatureValidator.COMPLEMENTARY_REGION_PROPERTY_NAME);
        Amount hybridizationValue = new Amount();
        hybridizationValue.nonNumericValue= "1_1-1_3;2_1-2_5";
        hybridizationProperty.setValue(hybridizationValue);

        NucleicAcidSubstance sub  = new NucleicAcidSubstanceBuilder()
                .addName("name")
                .addDnaSubunit("ATG")
                .addProperty(hybridizationProperty)
                .build();
        Subunit subunit = new Subunit();
        subunit.subunitIndex = 2;
        subunit.sequence = "TAT";
        sub.nucleicAcid.subunits = new ArrayList<>(sub.nucleicAcid.subunits);
        sub.nucleicAcid.subunits.add(subunit);

        ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub.toFullJsonNode());
        assertFalse(response.isValid());

        assertTrue(response.getValidationMessages().stream()
                .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR )
                .findAny().isPresent());
    }

    @Test
    public void nonComplementarySequencesMustGenerateWarnings() throws Exception{
        Property hybridizationProperty = new Property();
        hybridizationProperty.setName(HybridizationFeatureValidator.COMPLEMENTARY_REGION_PROPERTY_NAME);
        Amount hybridizationValue = new Amount();
        hybridizationValue.nonNumericValue= "1_1-1_3;2_1-2_3";
        hybridizationProperty.setValue(hybridizationValue);

        NucleicAcidSubstance sub  = new NucleicAcidSubstanceBuilder()
                .addName("name")
                .addDnaSubunit("ATA")
                .addProperty(hybridizationProperty)
                .build();
        Subunit subunit = new Subunit();
        subunit.subunitIndex = 2;
        subunit.sequence = "GGT";
        sub.nucleicAcid.subunits = new ArrayList<>(sub.nucleicAcid.subunits);
        sub.nucleicAcid.subunits.add(subunit);

         ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub.toFullJsonNode());

        assertTrue(response.getValidationMessages().stream()
                .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING )
                .findAny().isPresent());
    }

    @Test
    public void mismatchedSequencesMustGenerateError() throws Exception{
        Property hybridizationProperty = new Property();
        hybridizationProperty.setName(HybridizationFeatureValidator.COMPLEMENTARY_REGION_PROPERTY_NAME);
        Amount hybridizationValue = new Amount();
        hybridizationValue.nonNumericValue= "1_1-1_3;2_1-2_4";
        hybridizationProperty.setValue(hybridizationValue);

        NucleicAcidSubstance sub  = new NucleicAcidSubstanceBuilder()
                .addName("name")
                .addDnaSubunit("ATA")
                .addProperty(hybridizationProperty)
                .build();
        Subunit subunit = new Subunit();
        subunit.subunitIndex = 2;
        subunit.sequence = "GGTA";
        sub.nucleicAcid.subunits = new ArrayList<>(sub.nucleicAcid.subunits);
        sub.nucleicAcid.subunits.add(subunit);

        ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub.toFullJsonNode());

        assertTrue(response.getValidationMessages().stream()
                .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR )
                .findAny().isPresent());
    }

    @ParameterizedTest()
    @MethodSource("makeTestData")
    void testMatches(Character base1, Character base2, boolean expectMatch){
        Assertions.assertEquals(areMatches(base1, base2), expectMatch);
    } 
    
    private static Stream<Arguments> makeTestData() {
        return Stream.of(
                Arguments.of('A', 'A', false),
                Arguments.of('A', 'T', true),
                Arguments.of('A', 'U', true),
                Arguments.of('U', 'A', true),
                Arguments.of('G', 'C', true),
                Arguments.of('G', 'A', false),
                Arguments.of('G', 'G', false),
                Arguments.of('U', 'U', false),
                Arguments.of('T', 'T', false),
                Arguments.of('G', 'T', false));
    }
    private boolean areMatches(Character char1, Character char2) {
        return (char1 != 'A' || (char2 == 'T' || char2 == 'U'))
                && (char1 != 'T' || char2 == 'A')
                && (char1 != 'U' || char2 == 'A')
                && (char1 != 'G' || char2 == 'C')
                && (char1 != 'C' || char2 == 'G');
    }


}
