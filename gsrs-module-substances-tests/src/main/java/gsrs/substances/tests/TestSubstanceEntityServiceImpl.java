package gsrs.substances.tests;

import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.service.GsrsEntityService;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Substance;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;

/** Test-only service that guarantees UUIDs exist before persistence. */
public class TestSubstanceEntityServiceImpl extends SubstanceEntityServiceImpl {

    private final ThreadLocal<Boolean> preserveCreateSubstanceUuid = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    public GsrsEntityService.CreationResult<Substance> createEntity(JsonNode json, boolean isBatch) {
        Boolean previous = preserveCreateSubstanceUuid.get();
        preserveCreateSubstanceUuid.set(isBatch || Boolean.TRUE.equals(previous));
        try {
            return super.createEntity(json, isBatch);
        } finally {
            preserveCreateSubstanceUuid.set(previous);
        }
    }

    @Override
    protected Substance create(Substance substance) {
        normalizeCreateGraphForTest(substance);
        TestPersistUuidSupport.refreshReferenceIds(substance);
        // Hibernate 6 requires assigned-id entities in the graph to have IDs pre-set.
        TestPersistUuidSupport.ensurePersistableIds(substance);
        EntityManager entityManager = getEntityManager();
        entityManager.persist(substance);
        entityManager.flush();
        return substance;
    }

    private void normalizeCreateGraphForTest(Substance substance) {
        if (substance == null) {
            return;
        }
        if (!preserveCreateSubstanceUuid.get()) {
            substance.uuid = null;
        }
        substance.version = "1";
        if (substance.modifications != null) {
            substance.modifications.uuid = null;
        }

        if (substance instanceof ChemicalSubstance chemicalSubstance) {
            resetChemicalGraphIds(chemicalSubstance);
        }
    }

    private void resetChemicalGraphIds(ChemicalSubstance chemicalSubstance) {
        if (chemicalSubstance == null) {
            return;
        }
        resetChemicalStructureIds(chemicalSubstance.getStructure());
        if (chemicalSubstance.getMoieties() != null) {
            for (Moiety moiety : chemicalSubstance.getMoieties()) {
                moiety.uuid = null;
                moiety.innerUuid = null;
                resetChemicalStructureIds(moiety.structure);
            }
        }
    }

    private void resetChemicalStructureIds(GinasChemicalStructure structure) {
        if (structure == null) {
            return;
        }
        structure.id = null;
        structure.version = null;
    }

    @Override
    protected Substance update(Substance substance) {
        // Ensure all nested entities have UUIDs before update to prevent Hibernate 6
        // IdentifierGenerationException when persisting entities with assigned ID strategy
        TestPersistUuidSupport.ensurePersistableIds(substance);
        return super.update(substance);
    }
}
