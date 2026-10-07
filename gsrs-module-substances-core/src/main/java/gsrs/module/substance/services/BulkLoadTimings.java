package gsrs.module.substance.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Measures where a bulk load spends its time so a slow load can be diagnosed from the log
 * (logger name {@code bulkLoadTimings}) without a profiler.
 * <p>
 * Workers bind the job's timings to their thread with {@link #bind(String)}, so deeper code
 * (the persister) can record phases through {@link #record(Phase, long)} without new parameters.
 */
public final class BulkLoadTimings {

    private static final Logger TIMING_LOG = LoggerFactory.getLogger("bulkLoadTimings");
    private static final long REPORT_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(60);

    private static final Map<String, BulkLoadTimings> BY_JOB = new ConcurrentHashMap<>();
    private static final ThreadLocal<BulkLoadTimings> CURRENT = new ThreadLocal<>();

    public enum Phase {
        /** reading the next record from the file (reader thread) */
        READ,
        /** reader waiting because the worker queue is full */
        SUBMIT_WAIT,
        /** JSON record to substance JSON */
        TRANSFORM,
        /** waiting for the single-threaded persist lock */
        PERSIST_LOCK_WAIT,
        /** validation + database insert of the substance (createEntity) */
        VALIDATE_AND_SAVE,
        /** saving the ProcessingRecord row for the substance */
        PROCESSING_RECORD_SAVE,
        /** saving the ProcessingJob progress row */
        JOB_PROGRESS_SAVE,
        /** end of load: waiting for queued asynchronous index work */
        WAIT_ASYNC_INDEX,
        /** end of load: reindexing all persisted substances */
        FINAL_REINDEX
    }

    private final String jobKey;
    private final long startNanos = System.nanoTime();
    private final EnumMap<Phase, LongAdder> nanos = new EnumMap<>(Phase.class);
    private final EnumMap<Phase, LongAdder> counts = new EnumMap<>(Phase.class);
    private final LongAdder recordsDone = new LongAdder();
    private final AtomicLong lastReportNanos = new AtomicLong(System.nanoTime());
    private final AtomicLong recordsAtLastReport = new AtomicLong();

    private BulkLoadTimings(String jobKey) {
        this.jobKey = jobKey;
        for (Phase p : Phase.values()) {
            nanos.put(p, new LongAdder());
            counts.put(p, new LongAdder());
        }
    }

    public static BulkLoadTimings start(String jobKey) {
        BulkLoadTimings t = new BulkLoadTimings(jobKey);
        BY_JOB.put(jobKey, t);
        return t;
    }

    public static BulkLoadTimings forJob(String jobKey) {
        return jobKey == null ? null : BY_JOB.get(jobKey);
    }

    /** Logs the final summary and forgets the job. */
    public static void finish(String jobKey) {
        BulkLoadTimings t = jobKey == null ? null : BY_JOB.remove(jobKey);
        if (t != null) {
            TIMING_LOG.info("Bulk load {} FINISHED: {}", jobKey, t.summary());
        }
    }

    /** Binds the job's timings to the current worker thread; returns the previous binding. */
    public static BulkLoadTimings bind(String jobKey) {
        BulkLoadTimings previous = CURRENT.get();
        BulkLoadTimings t = forJob(jobKey);
        if (t == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(t);
        }
        return previous;
    }

    public static void restore(BulkLoadTimings previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    /** Adds the time since {@code startNanos} to the phase of the job bound to this thread. */
    public static void record(Phase phase, long startNanos) {
        BulkLoadTimings t = CURRENT.get();
        if (t != null) {
            t.add(phase, System.nanoTime() - startNanos);
        }
    }

    public void add(Phase phase, long elapsedNanos) {
        nanos.get(phase).add(elapsedNanos);
        counts.get(phase).increment();
    }

    /** Called once per finished record; logs a progress summary at most once a minute. */
    public void recordDone() {
        recordsDone.increment();
        long now = System.nanoTime();
        long last = lastReportNanos.get();
        if (now - last >= REPORT_INTERVAL_NANOS && lastReportNanos.compareAndSet(last, now)) {
            long done = recordsDone.sum();
            long sinceLast = done - recordsAtLastReport.getAndSet(done);
            double minutes = (now - last) / 60_000_000_000d;
            TIMING_LOG.info("Bulk load {} progress: last minute {} records/min | {}",
                    jobKey, Math.round(sinceLast / minutes), summary());
        }
    }

    public long getTotalNanos(Phase phase) {
        return nanos.get(phase).sum();
    }

    public long getCount(Phase phase) {
        return counts.get(phase).sum();
    }

    public long getRecordsDone() {
        return recordsDone.sum();
    }

    public String summary() {
        long done = recordsDone.sum();
        double elapsedMin = (System.nanoTime() - startNanos) / 60_000_000_000d;
        StringBuilder sb = new StringBuilder();
        sb.append("records=").append(done)
          .append(" elapsed=").append(String.format("%.1f", elapsedMin)).append("min")
          .append(" avg=").append(elapsedMin > 0 ? Math.round(done / elapsedMin) : 0).append(" records/min");
        for (Phase p : Phase.values()) {
            long n = counts.get(p).sum();
            if (n == 0) {
                continue;
            }
            long totalMs = TimeUnit.NANOSECONDS.toMillis(nanos.get(p).sum());
            sb.append(" | ").append(p).append(": total=").append(totalMs).append("ms")
              .append(" avg=").append(String.format("%.1f", totalMs / (double) n)).append("ms")
              .append(" n=").append(n);
        }
        return sb.toString();
    }
}
