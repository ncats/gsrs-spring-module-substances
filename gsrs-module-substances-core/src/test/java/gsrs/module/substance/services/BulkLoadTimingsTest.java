package gsrs.module.substance.services;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkLoadTimingsTest {

    @Test
    void phasesRecordedOnABoundThreadAreAddedToThatJob() {
        BulkLoadTimings timings = BulkLoadTimings.start("job-a");
        BulkLoadTimings previous = BulkLoadTimings.bind("job-a");
        try {
            BulkLoadTimings.record(BulkLoadTimings.Phase.VALIDATE_AND_SAVE,
                    System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(5));
            timings.recordDone();
        } finally {
            BulkLoadTimings.restore(previous);
        }

        assertEquals(1, timings.getCount(BulkLoadTimings.Phase.VALIDATE_AND_SAVE));
        assertTrue(timings.getTotalNanos(BulkLoadTimings.Phase.VALIDATE_AND_SAVE) >= TimeUnit.MILLISECONDS.toNanos(5));
        assertEquals(1, timings.getRecordsDone());
        assertTrue(timings.summary().contains("VALIDATE_AND_SAVE"));

        BulkLoadTimings.finish("job-a");
        assertNull(BulkLoadTimings.forJob("job-a"));
    }

    @Test
    void recordingWithoutABoundJobIsIgnored() {
        BulkLoadTimings timings = BulkLoadTimings.start("job-b");
        BulkLoadTimings.record(BulkLoadTimings.Phase.TRANSFORM, System.nanoTime());
        assertEquals(0, timings.getCount(BulkLoadTimings.Phase.TRANSFORM));
        BulkLoadTimings.finish("job-b");
    }

    @Test
    void restoreReturnsThePreviousBinding() {
        BulkLoadTimings outer = BulkLoadTimings.start("job-outer");
        BulkLoadTimings.start("job-inner");
        BulkLoadTimings none = BulkLoadTimings.bind("job-outer");
        BulkLoadTimings previous = BulkLoadTimings.bind("job-inner");
        assertSame(outer, previous);
        BulkLoadTimings.restore(previous);
        BulkLoadTimings.record(BulkLoadTimings.Phase.READ, System.nanoTime());
        assertEquals(1, outer.getCount(BulkLoadTimings.Phase.READ));
        BulkLoadTimings.restore(none);
        BulkLoadTimings.finish("job-outer");
        BulkLoadTimings.finish("job-inner");
    }
}
