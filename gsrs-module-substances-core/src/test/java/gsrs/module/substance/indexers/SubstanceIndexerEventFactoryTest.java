package gsrs.module.substance.indexers;

import gsrs.indexer.IndexUpdateEntityEvent;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubstanceIndexerEventFactoryTest {

    @Test
    void substanceCreationShouldUseTheAtomicUpdateEventToAvoidDuplicateDocuments() {
        Substance substance = new Substance();
        substance.getOrGenerateUUID();
        SubstanceIndexerEventFactory factory = new SubstanceIndexerEventFactory();

        Object createdEvent =
                factory.newCreateEventFor(EntityUtils.EntityWrapper.of(substance));

        IndexUpdateEntityEvent event =
                assertInstanceOf(IndexUpdateEntityEvent.class, createdEvent);
        assertTrue(event.getOptionalFetchedEntity().isPresent(),
                "the already-loaded entity must be supplied so the indexer does not refetch it");
        assertEquals(substance.getOrGenerateUUID().toString(),
                event.getSource().getIdString());
        assertTrue(factory.supports(substance));
    }

    @Test
    void factoryShouldNotCaptureNonSubstanceEntities() {
        SubstanceIndexerEventFactory factory = new SubstanceIndexerEventFactory();

        assertFalse(factory.supports(new Object()));
    }
}
