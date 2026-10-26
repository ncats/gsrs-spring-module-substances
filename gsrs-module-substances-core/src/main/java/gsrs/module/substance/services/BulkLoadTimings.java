package gsrs.module.substance.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import ix.core.stats.Estimate;

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
    private volatile long startNanos = System.nanoTime();
    private volatile long startedAtMillis = System.currentTimeMillis();
    private volatile Estimate totalRecords;
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
            TIMING_LOG.info("Bulk load {} TIMING SUMMARY (not a completion-status confirmation): {}",
                    jobKey, t.summary());
        }
    }

    public void markProcessingStarted() {
        startedAtMillis = System.currentTimeMillis();
        startNanos = System.nanoTime();
        lastReportNanos.set(startNanos);
        recordsAtLastReport.set(recordsDone.sum());
        TIMING_LOG.info("Bulk load {} started at {}", jobKey, Instant.ofEpochMilli(startedAtMillis));
    }

    public void setTotalRecords(Estimate totalRecords) {
        this.totalRecords = totalRecords;
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
        long elapsedNanos = Math.max(System.nanoTime() - startNanos, 0L);
        double elapsedSeconds = elapsedNanos / 1_000_000_000d;
        double averageRecordsPerSecond = elapsedSeconds > 0 ? done / elapsedSeconds : 0;
        StringBuilder sb = new StringBuilder();
        sb.append("startedAt=").append(Instant.ofEpochMilli(startedAtMillis))
          .append(" running=").append(Duration.ofNanos(elapsedNanos))
          .append(" records=").append(done)
          .append(" average=").append(String.format("%.2f", averageRecordsPerSecond)).append(" records/sec")
          .append(" estimatedTotal=").append(estimatedTotalDuration(elapsedNanos, done))
          .append(" timeLeft=").append(estimatedTimeLeft(elapsedNanos, done));
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

    private String estimatedTotalDuration(long elapsedNanos, long recordsDone) {
        Estimate estimate = totalRecords;
        if (!hasUsableEstimate(estimate)) {
            return "unknown";
        }
        if (recordsDone == 0) {
            return estimate.getCount() == 0 ? Duration.ZERO.toString() : "unknown";
        }
        long totalNanos = estimatedNanos(elapsedNanos, Math.max(estimate.getCount(), recordsDone), recordsDone);
        return Duration.ofNanos(totalNanos).toString();
    }

    private String estimatedTimeLeft(long elapsedNanos, long recordsDone) {
        Estimate estimate = totalRecords;
        if (!hasUsableEstimate(estimate)) {
            return "unknown";
        }
        long remainingRecords = Math.max(estimate.getCount() - recordsDone, 0L);
        if (remainingRecords == 0) {
            return Duration.ZERO.toString();
        }
        if (recordsDone == 0) {
            return "unknown";
        }
        return Duration.ofNanos(estimatedNanos(elapsedNanos, remainingRecords, recordsDone)).toString();
    }

    private boolean hasUsableEstimate(Estimate estimate) {
        return estimate != null
                && estimate.getCount() >= 0
                && (estimate.getType() == Estimate.TYPE.EXACT
                || estimate.getType() == Estimate.TYPE.APPROXIMATE);
    }

    private long estimatedNanos(long elapsedNanos, long recordCount, long recordsDone) {
        double estimate = (double) elapsedNanos * recordCount / recordsDone;
        return estimate >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.ceil(estimate);
    }
}
