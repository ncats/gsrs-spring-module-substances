package gsrs.module.substance.indexers;

import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.indexer.IndexerEventFactory;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.Substance;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Emits an {@link IndexUpdateEntityEvent} - rather than the default
 * {@code IndexCreateEntityEvent} - when a Substance is first persisted.
 *
 * <p>Both events are delivered after commit on the async pool, but their text-indexer
 * handlers differ in a way that matters under concurrent load:
 *
 * <ul>
 *   <li>the create handler performs {@code remove()} and then {@code add()} as two
 *       independently locked operations, so a second event for the same key can
 *       interleave between them and leave two documents behind;</li>
 *   <li>the update handler performs a single atomic {@code update()} under one lock,
 *       which deletes every document matching the key before adding exactly one.</li>
 * </ul>
 *
 * <p>Using the update event therefore makes indexing idempotent per key and removes the
 * duplicate-document race entirely. The structure and sequence indexers both expose an
 * equivalent {@code onUpdate} handler, so they keep indexing newly created substances.
 */
@Component
public class SubstanceIndexerEventFactory implements IndexerEventFactory {

    @Override
    public Object newCreateEventFor(EntityUtils.EntityWrapper entityWrapper) {
        Optional<EntityUtils.Key> key = entityWrapper.getOptionalKey();
        if (key.isEmpty()) {
            return null;
        }
        return new IndexUpdateEntityEvent(key.get(), Optional.of(entityWrapper));
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof Substance;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
