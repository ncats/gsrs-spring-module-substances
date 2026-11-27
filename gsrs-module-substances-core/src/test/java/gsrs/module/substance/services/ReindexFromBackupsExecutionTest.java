package gsrs.module.substance.services;

import gsrs.events.BeginReindexEvent;
import gsrs.events.EndReindexEvent;
import gsrs.events.IncrementReindexEvent;
import gsrs.events.listeners.ReindexEventListener;
import gsrs.repository.BackupRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.models.BackupEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReindexFromBackupsExecutionTest {
    private ReindexFromBackups service;
    private BackupRepository repository;
    private TypedQuery<Long> idQuery;
    private ApplicationEventPublisher publisher;
    private SchedulerPlugin.TaskListener listener;
    private final AtomicInteger processed = new AtomicInteger();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        service = new ReindexFromBackups() {
            @Override
            void reindexBackup(UUID id, BackupEntity backup, Set<String> seen) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                processed.incrementAndGet();
            }
        };
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        service.transactionManager = new DataSourceTransactionManager(source);
        repository = mock(BackupRepository.class);
        EntityManager em = mock(EntityManager.class);
        idQuery = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(idQuery);
        ReflectionTestUtils.setField(service, "entityManager", em);
        ReflectionTestUtils.setField(service, "backupRepository", repository);
        ReflectionTestUtils.setField(service, "batchSize", 2);
        publisher = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(service, "eventPublisher", publisher);
        listener = mock(SchedulerPlugin.TaskListener.class);
        // Exercise the actual completion counter, including the empty-database case.
        ReindexEventListener lifecycle = new ReindexEventListener();
        ReflectionTestUtils.setField(lifecycle, "applicationEventPublisher", publisher);
        doAnswer(invocation -> {
            Object event = invocation.getArgument(0);
            if (event instanceof BeginReindexEvent) {
                lifecycle.onNewReindex((BeginReindexEvent) event);
            } else if (event instanceof IncrementReindexEvent) {
                service.endReindex((IncrementReindexEvent) event);
                lifecycle.reindexEntity((IncrementReindexEvent) event);
            } else if (event instanceof EndReindexEvent) {
                service.endReindex((EndReindexEvent) event);
            }
            return null;
        }).when(publisher).publishEvent(any(Object.class));
    }

    @Test
    void loadsOnlyBoundedBatchesAndPublishesOneCompletionPerBackup() throws Exception {
        when(idQuery.getResultList()).thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
        List<List<Long>> batches = new ArrayList<>();
        when(repository.findAllById(any())).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            List<Long> ids = invocation.getArgument(0);
            batches.add(new ArrayList<>(ids));
            List<BackupEntity> backups = new ArrayList<>();
            ids.forEach(id -> { BackupEntity backup = new BackupEntity(); backup.data = new byte[0]; backups.add(backup); });
            return backups;
        });

        service.execute(UUID.randomUUID(), listener);

        assertEquals(List.of(List.of(1L, 2L), List.of(3L, 4L), List.of(5L)), batches);
        assertEquals(5, processed.get());
        verify(repository, never()).findAll();
        verify(publisher, times(5)).publishEvent(any(IncrementReindexEvent.class));
        verify(publisher).publishEvent(any(EndReindexEvent.class));
        assertCleanedUp();
    }

    @Test
    void removedBackupsStillCompleteTheSnapshotCount() throws Exception {
        when(idQuery.getResultList()).thenReturn(List.of(1L, 2L));
        when(repository.findAllById(any())).thenReturn(List.of());

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> service.execute(UUID.randomUUID(), listener));

        assertEquals(0, processed.get());
        verify(publisher, times(2)).publishEvent(any(IncrementReindexEvent.class));
        verify(publisher).publishEvent(any(EndReindexEvent.class));
        assertCleanedUp();
    }

    @Test
    void workerFailureIsPropagatedInsteadOfWaitingForever() {
        when(idQuery.getResultList()).thenReturn(List.of(1L));
        when(repository.findAllById(any())).thenThrow(new IllegalStateException("database read failed"));

        IOException error = assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(IOException.class, () -> service.execute(UUID.randomUUID(), listener)));

        assertNotNull(error.getCause());
        assertCleanedUp();
    }

    @Test
    void recordFailureIsReportedAfterCompletionAndCleanup() {
        service = spy(service);
        when(idQuery.getResultList()).thenReturn(List.of(1L));
        BackupEntity backup = new BackupEntity();
        backup.data = new byte[0];
        when(repository.findAllById(any())).thenReturn(List.of(backup));
        doThrow(new IllegalStateException("index listener failed"))
                .when(service).reindexBackup(any(), any(), any());

        IOException error = assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThrows(IOException.class, () -> service.execute(UUID.randomUUID(), listener)));

        assertTrue(error.getMessage().contains("1 backups"));
        verify(publisher).publishEvent(any(EndReindexEvent.class));
        assertCleanedUp();
    }

    @Test
    void interruptionLetsActiveWritesFinishAndSkipsRemainingWork() throws Exception {
        service = spy(service);
        ReflectionTestUtils.setField(service, "batchSize", 1);
        when(idQuery.getResultList()).thenReturn(List.of(1L, 2L, 3L));
        BackupEntity backup = new BackupEntity();
        backup.data = new byte[0];
        when(repository.findAllById(any())).thenReturn(List.of(backup));
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch releaseWrite = new CountDownLatch(1);
        CountDownLatch stopping = new CountDownLatch(1);
        AtomicBoolean workerInterrupted = new AtomicBoolean();
        AtomicBoolean callerInterruptPreserved = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        doAnswer(invocation -> {
            writing.countDown();
            try {
                releaseWrite.await();
            } catch (InterruptedException e) {
                workerInterrupted.set(true);
                throw e;
            }
            workerInterrupted.set(Thread.currentThread().isInterrupted());
            return null;
        }).when(service).reindexBackup(any(), any(), any());
        doAnswer(invocation -> {
            if (((String) invocation.getArgument(0)).startsWith("Stopping reindex:")) {
                stopping.countDown();
            }
            return null;
        }).when(listener).message(anyString());
        Thread caller = new Thread(() -> {
            try {
                service.execute(UUID.randomUUID(), listener);
            } catch (Throwable e) {
                failure.set(e);
            } finally {
                callerInterruptPreserved.set(Thread.currentThread().isInterrupted());
            }
        }, "reindex-cancellation-test");
        caller.start();
        try {
            assertTrue(writing.await(5, TimeUnit.SECONDS));
            caller.interrupt();
            assertTrue(stopping.await(5, TimeUnit.SECONDS));
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                while (caller.isAlive() && caller.getState() != Thread.State.TIMED_WAITING) {
                    Thread.sleep(1);
                }
            });
            assertTrue(caller.isAlive(), "Caller must wait for the active writer");
            verify(publisher, never()).publishEvent(any(EndReindexEvent.class));
        } finally {
            releaseWrite.countDown();
            caller.join(5000);
        }
        assertFalse(caller.isAlive());
        assertFalse(workerInterrupted.get(), "An interrupted NIO write can close the shared Lucene writer");
        assertTrue(callerInterruptPreserved.get());
        assertInstanceOf(IOException.class, failure.get());
        verify(repository, times(1)).findAllById(any());
        // One processed backup plus two skipped backups balance the lifecycle.
        verify(publisher, times(3)).publishEvent(any(IncrementReindexEvent.class));
        verify(publisher).publishEvent(any(EndReindexEvent.class));
        assertCleanedUp();
    }

    @Test
    void emptySnapshotCompletesWithoutLoadingBackups() throws Exception {
        when(idQuery.getResultList()).thenReturn(List.of());

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> service.execute(UUID.randomUUID(), listener));

        verify(repository, never()).findAllById(any());
        verify(publisher).publishEvent(any(EndReindexEvent.class));
        assertCleanedUp();
    }

    private void assertCleanedUp() {
        assertTrue(((Map<?, ?>) ReflectionTestUtils.getField(service, "latchMap")).isEmpty());
        assertTrue(((Map<?, ?>) ReflectionTestUtils.getField(service, "listenerMap")).isEmpty());
    }
}
