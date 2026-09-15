package example.substance;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithMockUser(username = "admin", roles = "Admin")
public class UpdateRelationshipWithAmountTest extends AbstractSubstanceJpaEntityTest {

    private final JsonMapper mapper = JsonMapper.builderWithJackson2Defaults()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void addRelationshipWithAmountToStructurallyDiverseSubstance() {
        Substance related = assertCreated(new SubstanceBuilder()
                .addName("Relationship amount target")
                .buildJson());
        StructurallyDiverseSubstance created = (StructurallyDiverseSubstance) assertCreated(new SubstanceBuilder()
                .asStructurallyDiverse()
                .addName("Structurally diverse relationship amount source")
                .buildJson());
        UUID originalDefinitionUuid = created.structurallyDiverse.getUuid();

        ObjectNode updateJson = (ObjectNode) created.toFullJsonNode();
        updateJson.set("modifications", emptyModificationsJson());
        ((ArrayNode) updateJson.get("relationships")).add(relationshipWithAmountJson(related));

        StructurallyDiverseSubstance updated = (StructurallyDiverseSubstance) assertUpdated(updateJson);
        Relationship relationship = updated.relationships.get(0);

        assertEquals(originalDefinitionUuid, updated.structurallyDiverse.getUuid());
        assertEquals(1, updated.relationships.size());
        assertEquals(related.getUuid().toString(), relationship.relatedSubstance.refuuid);
        assertNotNull(relationship.amount);
        assertEquals(1.25, relationship.amount.average);
        assertEquals("mg", relationship.amount.units);
    }

    private JsonNode emptyModificationsJson() {
        return mapper.readTree("""
                {
                  "agentModifications": [],
                  "physicalModifications": [],
                  "structuralModifications": []
                }
                """);
    }

    private JsonNode relationshipWithAmountJson(Substance related) {
        ObjectNode relationship = mapper.createObjectNode();
        relationship.put("type", Relationship.ACTIVE_MOIETY_RELATIONSHIP_TYPE);
        relationship.set("relatedSubstance", mapper.valueToTree(related.asSubstanceReference()));
        relationship.set("references", mapper.createArrayNode());
        relationship.set("access", mapper.createArrayNode());

        ObjectNode amount = mapper.createObjectNode();
        amount.put("type", "EXACT");
        amount.put("average", 1.25);
        amount.put("units", "mg");
        amount.set("references", mapper.createArrayNode());
        amount.set("access", mapper.createArrayNode());
        relationship.set("amount", amount);
        return relationship;
    }
}
