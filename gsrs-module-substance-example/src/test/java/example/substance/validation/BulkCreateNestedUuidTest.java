package example.substance.validation;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.AopTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BulkCreateNestedUuidTest extends AbstractSubstanceJpaEntityTest {
    private final JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();

    private ObjectNode payload() {
        String reference = UUID.randomUUID().toString();
        return (ObjectNode) mapper.readTree("""
                {"substanceClass":"concept","uuid":"%s","version":"12",
                 "names":[{"uuid":"%s","name":"bulk nested %s","references":["%s"],
                           "nameOrgs":[{"uuid":"%s","nameOrg":"TEST"}]}],
                 "references":[{"uuid":"%s","docType":"TEST","citation":"source","publicDomain":true}],
                 "codes":[{"uuid":"%s","codeSystem":"TEST","code":"123"}],
                 "notes":[{"uuid":"%s","note":"bulk note"}],
                 "properties":[{"uuid":"%s","name":"test","value":{"uuid":"%s","average":2}}],
                 "modifications":{"uuid":"%s","agentModifications":[
                    {"uuid":"%s","agentSubstance":{"uuid":"%s","refuuid":"%s","refPname":"agent"},
                     "amount":{"uuid":"%s","average":1}}]}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), reference,
                UUID.randomUUID(), reference, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void bulkCreatePreservesOwnedGraphAndReferenceLinksAfterReload() throws Exception {
        assertEquals(SubstanceEntityServiceImpl.class,
                AopTestUtils.getUltimateTargetObject(substanceEntityService).getClass());
        ObjectNode json = payload();
        Substance created = ensurePass(substanceEntityService.createEntity(json, true));
        UUID root = created.uuid;
        entityManager.flush();
        entityManager.clear();
        Substance saved = substanceRepository.findById(root).orElseThrow();
        assertEquals(json.get("uuid").asText(), saved.uuid.toString());
        assertEquals("1", saved.version);
        assertId(json.at("/names/0/uuid"), saved.names.get(0).uuid);
        assertId(json.at("/names/0/nameOrgs/0/uuid"), saved.names.get(0).nameOrgs.get(0).uuid);
        assertId(json.at("/references/0/uuid"), saved.references.get(0).uuid);
        assertId(json.at("/codes/0/uuid"), saved.codes.get(0).uuid);
        assertId(json.at("/notes/0/uuid"), saved.notes.get(0).uuid);
        assertId(json.at("/properties/0/uuid"), saved.properties.get(0).uuid);
        assertId(json.at("/properties/0/value/uuid"), saved.properties.get(0).getValue().uuid);
        assertId(json.at("/modifications/uuid"), saved.modifications.uuid);
        AgentModification agent = saved.modifications.agentModifications.get(0);
        assertId(json.at("/modifications/agentModifications/0/uuid"), agent.uuid);
        assertId(json.at("/modifications/agentModifications/0/agentSubstance/uuid"), agent.agentSubstance.uuid);
        assertId(json.at("/modifications/agentModifications/0/amount/uuid"), agent.amount.uuid);
        assertTrue(saved.names.get(0).getReferences().stream()
                .anyMatch(k -> saved.references.get(0).uuid.toString().equals(k.term)));
        assertEquals(root, saved.names.get(0).getOwner().uuid);
        assertEquals(root, saved.codes.get(0).getOwner().uuid);
    }

    @ParameterizedTest
    @CsvSource({
            "protein,protein,ix.ginas.models.v1.Protein",
            "nucleicAcid,nucleicAcid,ix.ginas.models.v1.NucleicAcid",
            "polymer,polymer,ix.ginas.models.v1.Polymer",
            "mixture,mixture,ix.ginas.models.v1.Mixture",
            "structurallyDiverse,structurallyDiverse,ix.ginas.models.v1.StructurallyDiverse",
            "specifiedSubstanceG1,specifiedSubstance,ix.ginas.models.v1.SpecifiedSubstanceGroup1"
    })
    void bulkCreatePreservesSubtypeIdentity(String substanceClass, String field, String entityClass) throws Exception {
        ObjectNode json = payload();
        UUID subtypeId = UUID.randomUUID();
        json.put("substanceClass", substanceClass);
        ObjectNode graph = json.putObject(field).put("uuid", subtypeId.toString());
        Map<Class<?>, UUID> children = new LinkedHashMap<>();
        switch (field) {
            case "protein" -> {
                child(graph.putArray("subunits").addObject().put("sequence", "ACD")
                        .put("subunitIndex", 1), Subunit.class, children);
                child(graph.putObject("glycosylation"), Glycosylation.class, children);
                child(graph.putArray("otherLinks").addObject().put("linkageType", "test"),
                        OtherLinks.class, children);
            }
            case "nucleicAcid" -> {
                child(graph.putArray("subunits").addObject().put("sequence", "ACGT")
                        .put("subunitIndex", 1), Subunit.class, children);
                child(graph.putArray("sugars").addObject().put("sugar", "dR"), Sugar.class, children);
                child(graph.putArray("linkages").addObject().put("linkage", "P"), Linkage.class, children);
            }
            case "polymer" -> {
                child(graph.putObject("classification"), PolymerClassification.class, children);
                child(graph.putArray("monomers").addObject(), Material.class, children);
                child(graph.putArray("structuralUnits").addObject().put("type", "SRU"), Unit.class, children);
                child(graph.putObject("displayStructure").put("smiles", "C"),
                        GinasChemicalStructure.class, children, "id");
            }
            case "mixture" ->
                    child(graph.putArray("components").addObject(), Component.class, children);
            case "structurallyDiverse" ->
                    child(graph.putObject("parentSubstance").put("refuuid", UUID.randomUUID().toString())
                            .put("refPname", "parent"), SubstanceReference.class, children);
            case "specifiedSubstance" ->
                    child(graph.putArray("constituents").addObject(), SpecifiedSubstanceComponent.class, children);
        }
        Substance saved = ensurePass(substanceEntityService.createEntity(json, true));
        entityManager.flush();
        entityManager.clear();
        assertNotNull(entityManager.getEntityManager().find(Class.forName(entityClass), subtypeId));
        for (Map.Entry<Class<?>, UUID> child : children.entrySet()) {
            assertNotNull(entityManager.getEntityManager().find(child.getKey(), child.getValue()),
                    "Missing imported " + child.getKey().getSimpleName() + " UUID " + child.getValue());
        }
        assertTrue(substanceRepository.existsById(saved.uuid));
    }

    @Test
    void bulkCreateGeneratesMissingChildUuids() throws Exception {
        ObjectNode json = payload();
        ((ObjectNode) json.at("/names/0")).remove("uuid");
        ((ObjectNode) json.at("/codes/0")).remove("uuid");
        ((ObjectNode) json.at("/references/0")).remove("uuid");
        ((ObjectNode) json.at("/names/0")).remove("references");
        Substance created = ensurePass(substanceEntityService.createEntity(json, true));
        entityManager.flush();
        entityManager.clear();
        Substance saved = substanceRepository.findById(created.uuid).orElseThrow();
        assertNotNull(saved.names.get(0).uuid);
        assertNotNull(saved.codes.get(0).uuid);
        assertNotNull(saved.references.get(0).uuid);
    }

    @ParameterizedTest
    @CsvSource({"names", "references", "codes"})
    void bulkCreateRejectsNestedCollisionWithoutOverwritingOriginal(String field) throws Exception {
        ObjectNode first = payload();
        Substance existing = ensurePass(substanceEntityService.createEntity(first, true));
        ObjectNode second = payload();
        String conflictingId = first.at("/" + field + "/0/uuid").asText();
        ((ObjectNode) second.at("/" + field + "/0")).put("uuid", conflictingId);
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> substanceEntityService.createEntity(second, true));
        assertTrue(failure.getMessage().contains(conflictingId));
        entityManager.clear();
        existing = substanceRepository.findById(existing.uuid).orElseThrow();
        assertEquals(first.at("/names/0/name").asText(), existing.names.get(0).name);
        assertFalse(substanceRepository.existsById(UUID.fromString(second.get("uuid").asText())));
    }

    @Test
    void duplicateNestedIdsInOneImportAreRejected() {
        ObjectNode json = payload();
        ObjectNode duplicate = ((ObjectNode) json.at("/names/0")).deepCopy();
        duplicate.put("name", "duplicate identity");
        ((tools.jackson.databind.node.ArrayNode) json.get("names")).add(duplicate);
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> substanceEntityService.createEntity(json, true));
        assertTrue(failure.getMessage().contains(json.at("/names/0/uuid").asText()));
    }

    @Test
    void bulkCreateRejectsExistingStructureId() throws Exception {
        UUID structureId = UUID.randomUUID();
        ObjectNode first = payload();
        first.put("substanceClass", "chemical");
        first.putObject("structure").put("id", structureId.toString()).put("smiles", "C");
        ensurePass(substanceEntityService.createEntity(first, true));
        entityManager.clear();
        ObjectNode second = payload();
        second.put("substanceClass", "chemical");
        second.putObject("structure").put("id", structureId.toString()).put("smiles", "CC");
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> substanceEntityService.createEntity(second, true));
        assertTrue(failure.getMessage().contains(structureId.toString()));
        assertEquals("C", entityManager.getEntityManager().find(GinasChemicalStructure.class, structureId).smiles);
    }

    @Test
    void bulkCreateRejectsExistingPublicMoietyUuidEvenWithDifferentStructureIds() throws Exception {
        UUID moietyId = UUID.randomUUID();
        ObjectNode first = payload();
        first.put("substanceClass", "chemical");
        first.putObject("structure").put("smiles", "C");
        first.putArray("moieties").addObject().put("id", UUID.randomUUID().toString())
                .put("uuid", moietyId.toString()).put("smiles", "C").put("count", 1);
        ensurePass(substanceEntityService.createEntity(first, true));
        entityManager.clear();
        ObjectNode second = payload();
        second.put("substanceClass", "chemical");
        second.putObject("structure").put("smiles", "CC");
        second.putArray("moieties").addObject().put("id", UUID.randomUUID().toString())
                .put("uuid", moietyId.toString()).put("smiles", "CC").put("count", 1);
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> substanceEntityService.createEntity(second, true));
        assertTrue(failure.getMessage().contains(moietyId.toString()));
        Substance original = substanceRepository.findById(UUID.fromString(first.get("uuid").asText())).orElseThrow();
        assertEquals("C", ((ChemicalSubstance) original).getMoieties().get(0).structure.smiles);
    }

    @Test
    void sourceNumericStructurePropertyKeysDoNotMakeImportedRowsDetached() throws Exception {
        UUID structureId = UUID.randomUUID();
        ObjectNode json = payload();
        json.put("substanceClass", "chemical");
        ObjectNode structure = json.putObject("structure")
                .put("id", structureId.toString()).put("smiles", "C");
        structure.putArray("properties").addObject()
                .put("id", 987654321L).put("label", "test").put("term", "source");
        Substance created = ensurePass(substanceEntityService.createEntity(json, true));
        entityManager.clear();
        ChemicalSubstance saved = (ChemicalSubstance) substanceRepository.findById(created.uuid).orElseThrow();
        assertEquals(structureId, saved.getStructure().id);
        assertEquals(1, saved.getStructure().properties.size());
        assertNotNull(saved.getStructure().properties.get(0).id);
        assertNotEquals(987654321L, saved.getStructure().properties.get(0).id);
    }

    @Test
    void ordinaryCreateStillReplacesNestedIdsAndRemapsReferenceLinks() throws Exception {
        ensurePass(substanceEntityService.createEntity(payload(), true));
        ObjectNode json = payload();
        Substance saved = ensurePass(substanceEntityService.createEntity(json, false));
        assertNotEquals(json.at("/names/0/uuid").asText(), saved.names.get(0).uuid.toString());
        assertNotEquals(json.at("/references/0/uuid").asText(), saved.references.get(0).uuid.toString());
        assertNotEquals(json.at("/codes/0/uuid").asText(), saved.codes.get(0).uuid.toString());
        assertTrue(saved.names.get(0).getReferences().stream()
                .anyMatch(k -> saved.references.get(0).uuid.toString().equals(k.term)));
    }

    private void assertId(JsonNode expected, UUID actual) {
        assertEquals(expected.asText(), actual.toString());
    }

    private void child(ObjectNode json, Class<?> type, Map<Class<?>, UUID> ids) {
        child(json, type, ids, "uuid");
    }

    private void child(ObjectNode json, Class<?> type, Map<Class<?>, UUID> ids, String idField) {
        UUID id = UUID.randomUUID();
        json.put(idField, id.toString());
        ids.put(type, id);
    }
}
