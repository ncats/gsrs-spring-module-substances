package gsrs.module.substance.identity;

import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.NameOrg;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubstanceGraphIdentityPolicyTest {

    private final SubstanceGraphIdentityPolicy policy = new SubstanceGraphIdentityPolicy();

    @Test
    void createAndBulkImportPreserveAssignedIdsButClearGeneratedStructureIds() {
        for (SubstanceGraphOperation operation : new SubstanceGraphOperation[]{
                SubstanceGraphOperation.CREATE,
                SubstanceGraphOperation.BULK_IMPORT}) {
            TestGraph graph = testGraph();

            policy.prepareForPersistence(graph.substance, null, operation);

            assertEquals(graph.substanceUuid, graph.substance.getUuid());
            assertEquals(graph.nameUuid, graph.substance.names.get(0).getUuid());
            assertEquals(graph.nameOrgUuid,
                    graph.substance.names.get(0).nameOrgs.get(0).getUuid());
            assertEquals(graph.moietyUuid, graph.substance.getMoieties().get(0).getUuid());
            assertNull(graph.substance.getStructure().id);
            assertNull(graph.substance.getStructure().version);
            assertNull(graph.substance.getMoieties().get(0).structure.id);
            assertNull(graph.substance.getMoieties().get(0).structure.version);
            assertEquals("1", graph.substance.version);
        }
    }

    @Test
    void updatePreservesIdentityAndRejectsChangingTheRootUuid() {
        TestGraph persisted = testGraph();
        TestGraph incoming = testGraph();
        incoming.substance.uuid = persisted.substanceUuid;

        policy.prepareForPersistence(
                incoming.substance, persisted.substance, SubstanceGraphOperation.UPDATE);

        assertEquals(incoming.nameUuid, incoming.substance.names.get(0).getUuid());
        assertNotNull(incoming.substance.getStructure().id);

        incoming.substance.uuid = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> policy.prepareForPersistence(
                incoming.substance, persisted.substance, SubstanceGraphOperation.UPDATE));
    }

    @Test
    void cloneClearsRootAssignedAndGeneratedIds() {
        TestGraph graph = testGraph();

        policy.prepareForPersistence(graph.substance, null, SubstanceGraphOperation.CLONE);

        assertNull(graph.substance.getUuid());
        assertNull(graph.substance.names.get(0).getUuid());
        assertNull(graph.substance.names.get(0).nameOrgs.get(0).getUuid());
        assertNull(graph.substance.getMoieties().get(0).getUuid());
        assertNull(graph.substance.getStructure().id);
        assertNull(graph.substance.getMoieties().get(0).structure.id);
        assertEquals("1", graph.substance.version);
    }

    private TestGraph testGraph() {
        TestGraph graph = new TestGraph();
        graph.substance = new ChemicalSubstance();
        graph.substanceUuid = UUID.randomUUID();
        graph.substance.uuid = graph.substanceUuid;

        Name name = new Name();
        graph.nameUuid = UUID.randomUUID();
        name.setUuid(graph.nameUuid);
        NameOrg nameOrg = new NameOrg("Example organization");
        graph.nameOrgUuid = UUID.randomUUID();
        nameOrg.setUuid(graph.nameOrgUuid);
        name.nameOrgs.add(nameOrg);
        graph.substance.names.add(name);

        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.id = UUID.randomUUID();
        structure.version = 3L;
        graph.substance.setStructure(structure);

        Moiety moiety = new Moiety();
        graph.moietyUuid = UUID.randomUUID();
        moiety.uuid = graph.moietyUuid;
        moiety.structure = new GinasChemicalStructure();
        moiety.structure.id = UUID.randomUUID();
        moiety.structure.version = 2L;
        graph.substance.addMoiety(moiety);
        return graph;
    }

    private static class TestGraph {
        private ChemicalSubstance substance;
        private UUID substanceUuid;
        private UUID nameUuid;
        private UUID nameOrgUuid;
        private UUID moietyUuid;
    }
}
