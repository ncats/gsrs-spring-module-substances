package gsrs.module.substance.services;

import gsrs.events.BeginReindexEvent;
import gsrs.events.EndReindexEvent;
import gsrs.events.IncrementReindexEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.repository.BackupRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.models.BackupEntity;
import ix.core.util.EntityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Rebuilds the indexes from backup keys, using current database entities. */
@Slf4j
public class ReindexFromBackups implements ReindexService {
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private BackupRepository backupRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Value("${gsrs.reindex.batch-size:128}")
    private int batchSize = 128;

    private final Map<UUID, CountDownLatch> latchMap = new ConcurrentHashMap<>();
    private final Map<UUID, TaskProgress> listenerMap = new ConcurrentHashMap<>();

    private static class TaskProgress {
        private final SchedulerPlugin.TaskListener listener;
        private final long total;
        private long current;

        TaskProgress(SchedulerPlugin.TaskListener listener, long total) {
            this.listener = listener;
            this.total = total;
        }

        synchronized void increment() {
            listener.message("Indexed: " + (++current) + " of " + total);
            if (current % 1000 == 0 || current == total) {
                log.info("Reindex processed {} of {} backups", current, total);
            }
        }
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void executeAsync(Object id, SchedulerPlugin.TaskListener listener) throws IOException {
        execute(id, listener);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void execute(Object id, SchedulerPlugin.TaskListener listener) throws IOException {
        if (batchSize < 1) {
            throw new IllegalArgumentException("gsrs.reindex.batch-size must be positive");
        }
        UUID reindexId = (UUID) id;
        listener.message("Initializing reindexing: acquiring backup IDs");
        TransactionTemplate readTx = readTransaction();
        // Snapshot IDs only, not all backup LOBs. This fixes the progress total
        // even if backups are inserted or removed during the run.
        List<Long> ids = readTx.execute(status -> entityManager.createQuery(
                "select b.id from BackupEntity b order by b.id", Long.class).getResultList());
        Set<String> seen = ConcurrentHashMap.newKeySet();
        AtomicLong failures = new AtomicLong();
        CountDownLatch endLatch = new CountDownLatch(1);
        latchMap.put(reindexId, endLatch);
        listenerMap.put(reindexId, new TaskProgress(listener, ids.size()));
        ForkJoinPool pool = new ForkJoinPool(4);
        try {
            // Observe the task: a discarded Future hides failures and can leave
            // the caller waiting forever for the completion event.
            pool.submit(() -> {
                eventPublisher.publishEvent(new BeginReindexEvent(reindexId, ids.size(),
                        BeginReindexEvent.IndexBehavior.WIPE_ALL_INDEXES, Collections.emptyList()));
                log.info("Reindex {} starting with {} backups, batch size {}", reindexId, ids.size(), batchSize);
                for (int offset = 0; offset < ids.size(); offset += batchSize) {
                    List<Long> batchIds = ids.subList(offset, Math.min(offset + batchSize, ids.size()));
                    List<BackupEntity> backups = readTx.execute(status -> {
                        List<BackupEntity> batch = backupRepository.findAllById(batchIds);
                        // Access LOBs in the loading transaction before passing
                        // detached backups to workers.
                        batch.forEach(backup -> Objects.requireNonNull(backup.data, "Backup data"));
                        return batch;
                    });
                    backups.parallelStream().forEach(backup -> {
                        try {
                            // One context per backup reuses entities loaded for its
                            // root when indexing children. No enclosing transaction
                            // retains the entire database or an idle connection.
                            readTx.executeWithoutResult(status -> reindexBackup(reindexId, backup, seen));
                        } catch (Exception e) {
                            failures.incrementAndGet();
                            log.warn("Reindex {} failed for backup {}", reindexId, backup.getRefid(), e);
                        }
                        eventPublisher.publishEvent(new IncrementReindexEvent(reindexId));
                    });
                    // Removed backups must not leave the completion count stuck.
                    for (int missing = backups.size(); missing < batchIds.size(); missing++) {
                        eventPublisher.publishEvent(new IncrementReindexEvent(reindexId));
                    }
                    log.info("Reindex {} processed {} of {} backups; failed backups: {}",
                            reindexId, offset + batchIds.size(), ids.size(), failures.get());
                }
            }).get();
            // Listeners are synchronous, so a missing event after workers finish
            // is a lifecycle error rather than a reason to wait indefinitely.
            if (!endLatch.await(30, TimeUnit.SECONDS)) {
                throw new IOException("Reindex workers finished without an EndReindexEvent: " + reindexId);
            }
            if (failures.get() != 0) {
                throw new IOException("Reindex failed for " + failures.get() + " backups; see the log");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Reindex interrupted: " + reindexId, e);
        } catch (ExecutionException e) {
            throw new IOException("Reindex worker failed: " + reindexId, e.getCause());
        } finally {
            pool.shutdownNow();
            latchMap.remove(reindexId);
            listenerMap.remove(reindexId);
        }
    }

    private TransactionTemplate readTransaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setReadOnly(true);
        return tx;
    }

    void reindexBackup(UUID reindexId, BackupEntity backup, Set<String> seen) {
        Object value = backup.getOptionalInstantiated().orElseThrow(() ->
                new IllegalStateException("Unable to instantiate backup " + backup.getRefid()));
        EntityUtils.EntityWrapper.of(value).traverse().execute((path, child) -> {
            EntityUtils.EntityWrapper<?> wrapped = EntityUtils.EntityWrapper.of(child);
            if (wrapped.isEntity() && wrapped.isRootIndex()) {
                EntityUtils.Key key = wrapped.getKey();
                if (seen.add(key.toString())) {
                    // Preserve stale-backup protection. Fetching and synchronous
                    // index listeners share this backup's transaction.
                    currentEntityReindexEvent(reindexId, key).ifPresent(eventPublisher::publishEvent);
                }
            }
        });
    }

    Optional<ReindexEntityEvent> currentEntityReindexEvent(UUID reindexId, EntityUtils.Key key) {
        return key.fetch().map(currentEntity ->
                // BeginReindexEvent wiped the indexes; seen deduplicates keys.
                // Deletion would needlessly refresh/search the growing indexes.
                new ReindexEntityEvent(reindexId, key, Optional.of(currentEntity), false));
    }

    @EventListener(EndReindexEvent.class)
    public void endReindex(EndReindexEvent event) {
        CountDownLatch latch = latchMap.remove(event.getId());
        if (latch != null) {
            latch.countDown();
        }
    }

    @EventListener(IncrementReindexEvent.class)
    public void endReindex(IncrementReindexEvent event) {
        TaskProgress progress = listenerMap.get(event.getId());
        if (progress != null) {
            progress.increment();
        }
    }
}
