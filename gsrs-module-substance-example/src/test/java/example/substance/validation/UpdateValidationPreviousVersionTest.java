package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidatorCallback;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.ValidatorPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class UpdateValidationPreviousVersionTest extends AbstractSubstanceJpaEntityTest {

    private static final String MISSING_OLD_VALUE_MESSAGE = "old substance was null during update validation";

    @Autowired
    private TestGsrsValidatorFactory factory;

    @BeforeEach
    public void setup() {
        ValidatorConfig config = new DefaultValidatorConfig();
        config.setValidatorClass(RequireOldValueOnUpdateValidator.class);
        config.setNewObjClass(Substance.class);
        factory.addValidator("substances", config);
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    void updatedSubstanceValidationReceivesPreviousVersion() throws Exception {
        Substance created = assertCreated(new ChemicalSubstanceBuilder()
                .addName("validation-update-test", name -> name.addLanguage("en"))
                .setStructureWithDefaultReference("CC")
                .buildJson());

        JsonNode updateJson = created.toFullJsonNode();
        ((com.fasterxml.jackson.databind.node.ArrayNode) updateJson.get("names")).addObject()
                .put("name", "validation-update-test-2")
                .put("type", "cn")
                .putArray("languages").add("en");

        ValidationResponse<Substance> response = substanceEntityService.validateEntity(updateJson);

        assertFalse(response.getValidationMessages().stream()
                        .anyMatch(m -> MISSING_OLD_VALUE_MESSAGE.equals(m.getMessage())),
                response.getValidationMessages().toString());

        assertUpdated(updateJson);
    }

    public static class RequireOldValueOnUpdateValidator implements ValidatorPlugin<Substance> {
        @Override
        public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
            if (objold == null) {
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(MISSING_OLD_VALUE_MESSAGE));
            }
        }

        @Override
        public boolean supports(Substance newValue, Substance oldValue, ValidatorConfig.METHOD_TYPE methodType) {
            return methodType == ValidatorConfig.METHOD_TYPE.UPDATE;
        }
    }
}
