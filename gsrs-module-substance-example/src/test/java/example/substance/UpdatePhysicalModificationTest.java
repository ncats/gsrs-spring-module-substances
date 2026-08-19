package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.modelBuilders.SpecifiedSubstanceGroup1SubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.PhysicalModification;
import ix.ginas.models.v1.SpecifiedSubstanceComponent;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithMockUser(username = "admin", roles = "Admin")
public class UpdatePhysicalModificationTest extends AbstractSubstanceJpaEntityTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void addPhysicalModificationToSpecifiedSubstanceGroup1() throws Exception {
        Substance basis = assertCreated(new SubstanceBuilder()
                .addName("SSG1 Basis")
                .buildJson());

        ObjectNode createJson = (ObjectNode) new SpecifiedSubstanceGroup1SubstanceBuilder()
                        .addName("SSG1 with physical modification")
                        .setSpecifiedSubstance(specifiedSubstanceFor(basis))
                        .buildJson();
        createJson.set("modifications", emptyModificationsJson());

        SpecifiedSubstanceGroup1Substance created = (SpecifiedSubstanceGroup1Substance) assertCreated(createJson);

        ObjectNode updateJson = (ObjectNode) created.toFullJsonNode();
        ((ArrayNode) updateJson.at("/modifications/physicalModifications")).add(physicalModificationJson());

        SpecifiedSubstanceGroup1Substance updated = (SpecifiedSubstanceGroup1Substance) assertUpdated(updateJson);
        PhysicalModification modification = updated.modifications.physicalModifications.get(0);

        assertEquals(1, updated.modifications.physicalModifications.size());
        assertEquals("PHYSICAL PROCESS", modification.physicalModificationRole);
        assertEquals(1, modification.parameters.size());
        assertEquals("TEMPERATURE", modification.parameters.get(0).parameterName);
    }

    private SpecifiedSubstanceGroup1 specifiedSubstanceFor(Substance basis) {
        SpecifiedSubstanceComponent component = new SpecifiedSubstanceComponent();
        component.substance = basis.asSubstanceReference();

        SpecifiedSubstanceGroup1 specifiedSubstance = new SpecifiedSubstanceGroup1();
        specifiedSubstance.constituents = List.of(component);
        return specifiedSubstance;
    }

    private JsonNode emptyModificationsJson() throws Exception {
        return mapper.readTree("""
                {
                  "agentModifications": [],
                  "physicalModifications": [],
                  "structuralModifications": []
                }
                """);
    }

    private JsonNode physicalModificationJson() throws Exception {
        return mapper.readTree("""
                {
                  "physicalModificationRole": "PHYSICAL PROCESS",
                  "modificationGroup": "1",
                  "parameters": [
                    {
                      "parameterName": "TEMPERATURE",
                      "amount": {
                        "type": "ESTIMATED",
                        "average": 25.0,
                        "units": "C"
                      },
                      "references": []
                    }
                  ],
                  "references": []
                }
                """);
    }
}
