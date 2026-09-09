package example.substance;

import tools.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.modelBuilders.SpecifiedSubstanceGroup1SubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.PhysicalModification;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.SpecifiedSubstanceComponent;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import ix.ginas.models.v1.StructuralModification;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void addStructuralModificationWithAmountToProtein() throws Exception {
        Substance fragment = assertCreated(new SubstanceBuilder()
                .addName("Protein structural fragment")
                .buildJson());

        ObjectNode createJson = (ObjectNode) new SubstanceBuilder()
                .asProtein()
                .addName("Protein with structural modification")
                .addSubunitWithDefaultReference("ACDEFGHIK")
                .addRelationshipTo(fragment, Relationship.ACTIVE_MOIETY_RELATIONSHIP_TYPE)
                .buildJson();
        createJson.set("modifications", emptyModificationsJson());

        ProteinSubstance created = (ProteinSubstance) assertCreated(createJson);

        ObjectNode updateJson = (ObjectNode) created.toFullJsonNode();
        String copiedReferenceUuid = updateJson.at("/relationships/0/relatedSubstance/uuid").asText();
        ObjectNode structuralModification = (ObjectNode) structuralModificationWithAmountJson();
        structuralModification.set("molecularFragment",
                updateJson.at("/relationships/0/relatedSubstance").deepCopy());
        ((ArrayNode) updateJson.at("/modifications/structuralModifications"))
                .add(structuralModification);

        ProteinSubstance updated = (ProteinSubstance) assertUpdated(updateJson);
        StructuralModification modification = updated.modifications.structuralModifications.get(0);

        assertEquals(1, updated.modifications.structuralModifications.size());
        assertEquals("AMINO_ACID_SUBSTITUTION", modification.structuralModificationType);
        assertEquals("COMPLETE", modification.extent);
        assertEquals(1, modification.getSites().size());
        assertNotNull(modification.extentAmount);
        assertEquals(1.0, modification.extentAmount.average);
        assertNotNull(modification.molecularFragment);
        assertEquals(fragment.getUuid().toString(), modification.molecularFragment.refuuid);
        assertNotEquals(copiedReferenceUuid, modification.molecularFragment.getUuid().toString());
    }

    @Test
    void createCopiedProteinWithStructuralModificationAmountResetsOwnedChildIds() throws Exception {
        Substance fragment = assertCreated(new SubstanceBuilder()
                .addName("Protein copy structural fragment")
                .buildJson());

        ObjectNode createJson = (ObjectNode) new SubstanceBuilder()
                .asProtein()
                .addName("Original protein copied for create")
                .addSubunitWithDefaultReference("ACDEFGHIK")
                .addRelationshipTo(fragment, Relationship.ACTIVE_MOIETY_RELATIONSHIP_TYPE)
                .buildJson();
        createJson.set("modifications", emptyModificationsJson());

        ProteinSubstance original = (ProteinSubstance) assertCreated(createJson);
        String originalNameReference = original.names.get(0).getReferences().iterator().next().term;
        ObjectNode copiedCreateJson = (ObjectNode) original.toFullJsonNode();
        copiedCreateJson.remove("uuid");
        ((ObjectNode) copiedCreateJson.at("/names/0")).put("name",
                "Copied protein with structural modification");
        ObjectNode structuralModification = (ObjectNode) structuralModificationWithAmountJson();
        structuralModification.set("molecularFragment",
                copiedCreateJson.at("/relationships/0/relatedSubstance").deepCopy());
        ((ArrayNode) copiedCreateJson.at("/modifications/structuralModifications"))
                .add(structuralModification);

        ProteinSubstance copied = (ProteinSubstance) assertCreated(copiedCreateJson);
        StructuralModification modification = copied.modifications.structuralModifications.get(0);

        assertEquals("Copied protein with structural modification", copied.names.get(0).name);
        assertNotEquals(original.names.get(0).getUuid(), copied.names.get(0).getUuid());
        String copiedNameReference = copied.names.get(0).getReferences().iterator().next().term;
        Set<String> copiedReferenceUuids = copied.references.stream()
                .map(reference -> reference.getUuid().toString())
                .collect(Collectors.toSet());
        assertNotEquals(originalNameReference, copiedNameReference);
        assertTrue(copiedReferenceUuids.contains(copiedNameReference));
        assertNotNull(modification.extentAmount);
        assertEquals(1.0, modification.extentAmount.average);
        assertNotNull(modification.molecularFragment);
        assertEquals(fragment.getUuid().toString(), modification.molecularFragment.refuuid);
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

    private JsonNode structuralModificationWithAmountJson() throws Exception {
        return mapper.readTree("""
                {
                  "structuralModificationType": "AMINO_ACID_SUBSTITUTION",
                  "locationType": "SITE-SPECIFIC",
                  "residueModified": "GLYCINE",
                  "sites": [
                    {
                      "subunitIndex": 1,
                      "residueIndex": 1
                    }
                  ],
                  "extent": "COMPLETE",
                  "extentAmount": {
                    "type": "EXACT",
                    "average": 1.0,
                    "units": "mol"
                  },
                  "modificationGroup": "1",
                  "references": []
                }
                """);
    }
}
