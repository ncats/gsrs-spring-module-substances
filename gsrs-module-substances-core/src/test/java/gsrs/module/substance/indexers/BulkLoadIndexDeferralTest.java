package gsrs.module.substance.indexers;

import gsrs.indexer.IndexUpdateEntityEvent;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkLoadIndexDeferralTest {

    private final SubstanceIndexerEventFactory factory = new SubstanceIndexerEventFactory();

    private static Substance substance() {
        Substance s = new ChemicalSubstance();
        s.setUuid(UUID.randomUUID());
        return s;
    }

    @Test
    void withoutDeferralTheFactoryPublishesIndexEvents() {
        Substance s = substance();
        EntityUtils.EntityWrapper<?> ew = EntityUtils.EntityWrapper.of(s);

        assertFalse(BulkLoadIndexDeferral.isActive());
        assertInstanceOf(IndexUpdateEntityEvent.class, factory.newCreateEventFor(ew));
        assertInstanceOf(IndexUpdateEntityEvent.class, factory.newUpdateEventFor(ew));
    }

    @Test
    void duringDeferralCreateAndUpdateAreRecordedInsteadOfPublished() {
        Substance created = substance();
        Substance updated = substance();
        Set<UUID> sink = new HashSet<>();
        Object[] events = new Object[2];

        BulkLoadIndexDeferral.runDeferred(sink, () -> {
            events[0] = factory.newCreateEventFor(EntityUtils.EntityWrapper.of(created));
            events[1] = factory.newUpdateEventFor(EntityUtils.EntityWrapper.of(updated));
        });

        assertInstanceOf(BulkLoadIndexDeferral.Deferred.class, events[0]);
        assertInstanceOf(BulkLoadIndexDeferral.Deferred.class, events[1]);
        assertEquals(Set.of(created.getUuid(), updated.getUuid()), sink);
        assertFalse(BulkLoadIndexDeferral.isActive(), "deferral must end with the work");
    }

    @Test
    void deferralIsClearedEvenWhenTheWorkFails() {
        Set<UUID> sink = new HashSet<>();
        try {
            BulkLoadIndexDeferral.runDeferred(sink, () -> {
                assertTrue(BulkLoadIndexDeferral.isActive());
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException expected) {
            // expected
        }
        assertFalse(BulkLoadIndexDeferral.isActive());
    }

    @Test
    void nonSubstancesAreNeverDeferred() {
        Set<UUID> sink = new HashSet<>();
        BulkLoadIndexDeferral.runDeferred(sink, () -> assertFalse(BulkLoadIndexDeferral.deferIfActive("not a substance")));
        assertTrue(sink.isEmpty());
    }
}
