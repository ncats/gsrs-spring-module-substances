package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.NucleicAcidSubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.validation.validators.HybridizationFeatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;

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
        subunit.subunitIndex = 1;
        subunit.sequence = "TAT";
        sub.nucleicAcid.subunits = new ArrayList<>(sub.nucleicAcid.subunits);
        sub.nucleicAcid.subunits.add(subunit);

        ValidationResponse<Substance> response = substanceEntityService.validateEntity(sub.toFullJsonNode());
        assertFalse(response.isValid());

        assertTrue(response.getValidationMessages().stream()
                .filter(m->m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR )
                .findAny().isEmpty());
    }

}
