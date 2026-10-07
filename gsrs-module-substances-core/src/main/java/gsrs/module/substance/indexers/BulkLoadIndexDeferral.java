package gsrs.module.substance.indexers;

import ix.ginas.models.v1.Substance;

import java.util.Set;
import java.util.UUID;

/**
 * Lets a bulk load postpone indexing of the substances it creates or changes.
 * <p>
 * While a bulk-load worker thread has an active deferral, {@link SubstanceIndexerEventFactory}
 * does not publish the usual asynchronous index event for a substance; it records the substance
 * UUID instead. The bulk load then indexes every recorded substance exactly once, synchronously,
 * after all workers have finished. That removes the race between the background index events and
 * the end-of-load reindex that could leave duplicate documents in the indexes.
 */
public final class BulkLoadIndexDeferral {

    private static final ThreadLocal<Set<UUID>> SINK = new ThreadLocal<>();

    private BulkLoadIndexDeferral() {
    }

    /**
     * Runs {@code work} on the current thread with indexing deferred; the UUIDs of substances that
     * would have been indexed are added to {@code sink}.
     */
    public static void runDeferred(Set<UUID> sink, Runnable work) {
        Set<UUID> previous = SINK.get();
        SINK.set(sink);
        try {
            work.run();
        } finally {
            if (previous == null) {
                SINK.remove();
            } else {
                SINK.set(previous);
            }
        }
    }

    public static boolean isActive() {
        return SINK.get() != null;
    }

    /**
     * @return {@code true} if indexing of this entity was deferred (and recorded), in which case
     * no index event should be published.
     */
    public static boolean deferIfActive(Object entity) {
        Set<UUID> sink = SINK.get();
        if (sink == null || !(entity instanceof Substance substance) || substance.getUuid() == null) {
            return false;
        }
        sink.add(substance.getUuid());
        return true;
    }

    /**
     * Published in place of an index event when indexing is deferred. Spring does not allow a
     * {@code null} event, and no listener handles this type, so publishing it does nothing.
     */
    public record Deferred(UUID substanceUuid) {
    }
}
