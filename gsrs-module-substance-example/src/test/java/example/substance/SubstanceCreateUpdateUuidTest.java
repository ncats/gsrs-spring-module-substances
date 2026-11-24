package example.substance;

import gsrs.service.GsrsEntityService;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.validator.ValidationMessage;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies how POST (create) and PUT (update) handle missing, duplicate and unknown substance UUIDs.
 */
class SubstanceCreateUpdateUuidTest extends AbstractSubstanceJpaEntityTest {

    private Substance createBase(String name) {
        return assertCreated(new SubstanceBuilder()
                .addName(name)
                .generateNewUUID()
                .buildJson());
    }

    @Test
    void postWithoutUuidGeneratesUuidAndStoresSubstance() {
        JsonNode json = new SubstanceBuilder().addName("uuid test no uuid").buildJson();
        ((ObjectNode) json).remove("uuid");

        Substance created = assertCreated(json);

        assertNotNull(created.getUuid());
        assertNotNull(created.names.get(0).getUuid());
        assertTrue(substanceRepository.existsById(created.getUuid()));
    }

    @Test
    void postWithExistingUuidIsRejectedWithValidationErrorAndKeepsOriginal() throws Exception {
        Substance existing = createBase("uuid test original");
        JsonNode json = new SubstanceBuilder().addName("uuid test duplicate").buildJson();
        ((ObjectNode) json).put("uuid", existing.getUuid().toString());

        GsrsEntityService.CreationResult<Substance> result = substanceEntityService.createEntity(json);

        assertFalse(result.isCreated());
        assertNull(result.getCreatedEntity());
        assertFalse(result.getValidationResponse().isValid());
        assertTrue(result.getValidationResponse().getValidationMessages().stream()
                .anyMatch(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR
                        && m.getMessage().contains(existing.getUuid().toString())));
        assertEquals("uuid test original",
                substanceRepository.findById(existing.getUuid()).get().names.get(0).name);
    }

    @Test
    void bulkCreateWithExistingUuidIsRejectedWithValidationError() throws Exception {
        Substance existing = createBase("uuid test bulk original");
        JsonNode json = new SubstanceBuilder().addName("uuid test bulk duplicate").buildJson();
        ((ObjectNode) json).put("uuid", existing.getUuid().toString());

        GsrsEntityService.CreationResult<Substance> result = substanceEntityService.createEntity(json, true);

        assertFalse(result.isCreated());
        assertTrue(result.getValidationResponse().hasError());
    }

    @Test
    void putWithoutUuidReturnsNotFound() throws Exception {
        Substance existing = createBase("uuid test put no uuid");
        ObjectNode json = (ObjectNode) existing.toFullJsonNode();
        json.remove("uuid");

        GsrsEntityService.UpdateResult<Substance> result = substanceEntityService.updateEntity(json);

        assertEquals(GsrsEntityService.UpdateResult.STATUS.NOT_FOUND, result.getStatus());
        assertNull(result.getUpdatedEntity());
    }

    @Test
    void putWithUnknownUuidReturnsNotFoundAndCreatesNothing() throws Exception {
        Substance existing = createBase("uuid test put unknown uuid");
        ObjectNode json = (ObjectNode) existing.toFullJsonNode();
        UUID unknown = UUID.randomUUID();
        json.put("uuid", unknown.toString());

        GsrsEntityService.UpdateResult<Substance> result = substanceEntityService.updateEntity(json);

        assertEquals(GsrsEntityService.UpdateResult.STATUS.NOT_FOUND, result.getStatus());
        assertNull(result.getUpdatedEntity());
        assertFalse(substanceRepository.existsById(unknown));
    }

    @Test
    void putWithUnknownUuidWithoutValidationReturnsNotFound() {
        Substance existing = createBase("uuid test put unknown no validation");
        ObjectNode json = (ObjectNode) existing.toFullJsonNode();
        json.put("uuid", UUID.randomUUID().toString());

        GsrsEntityService.UpdateResult<Substance> result = substanceEntityService.updateEntityWithoutValidation(json);

        assertEquals(GsrsEntityService.UpdateResult.STATUS.NOT_FOUND, result.getStatus());
    }

    @Test
    void putWithExistingUuidStillUpdates() throws Exception {
        Substance existing = createBase("uuid test put existing");
        JsonNode json = new SubstanceBuilder(existing)
                .addName("uuid test put existing second name")
                .buildJson();

        Substance updated = assertUpdated(json);

        assertEquals(existing.getUuid(), updated.getUuid());
        assertEquals(2, updated.names.size());
    }
}
