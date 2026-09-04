package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateSubstanceWithAssignedUuidTest extends AbstractSubstanceJpaEntityTest {

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void ordinaryCreatePreservesClientAssignedSubstanceUuid() throws Exception {
        UUID assignedUuid = UUID.randomUUID();
        JsonNode json = new SubstanceBuilder()
                .addName("assigned uuid substance " + assignedUuid)
                .setUUID(assignedUuid)
                .buildJson();

        Substance created = ensurePass(substanceEntityService.createEntity(json));

        assertEquals(assignedUuid, created.getUuid());
    }
}