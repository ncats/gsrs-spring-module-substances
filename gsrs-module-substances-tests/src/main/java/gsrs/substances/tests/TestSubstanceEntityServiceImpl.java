package gsrs.substances.tests;

import gsrs.module.substance.SubstanceEntityServiceImpl;
import ix.ginas.models.v1.Substance;

import jakarta.persistence.EntityManager;

/** Test-only service that guarantees UUIDs exist before persistence. */
public class TestSubstanceEntityServiceImpl extends SubstanceEntityServiceImpl {

    @Override
    protected Substance create(Substance substance) {
        TestPersistUuidSupport.ensurePersistableIds(substance);
        EntityManager entityManager = getEntityManager();
        entityManager.persist(substance);
        entityManager.flush();
        return substance;
    }
}

