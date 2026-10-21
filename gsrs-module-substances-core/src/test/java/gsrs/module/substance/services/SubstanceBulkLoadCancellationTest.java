package gsrs.module.substance.services;

import gsrs.AuditConfig;
import gsrs.module.substance.repository.ProcessingJobRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.PayloadRepository;
import gsrs.security.AdminService;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import ix.core.models.ProcessingJob;
import ix.core.processing.PayloadProcessor;
import ix.core.processing.PersistRecordWorker;
import ix.core.processing.PersistRecordWorkerFactory;
import ix.core.processing.RecordExtractor;
import ix.core.processing.RecordExtractorFactory;
import ix.core.stats.Estimate;
import ix.core.util.FilteredPrintStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reproduces "RejectedExecutionException ... [Shutting down ...]" during a bulk load:
 * once the loading executor is shut down (cancel or server shutdown), the reader must stop
 * instead of rejecting every remaining record, and the job must not be reported as COMPLETE.
 */
class SubstanceBulkLoadCancellationTest {

    private static final int RECORDS_IN_FILE = 5_000;

    @Test
    void cancellingARunningLoadStopsReadingAndMarksTheJobStopped() throws Exception {
        Fixture f = new Fixture();
        PayloadProcessor pp = f.service.submit(f.parameters());

        assertTrue(f.firstWorkerStarted.await(10, TimeUnit.SECONDS), "first record should start loading");
        Thread.sleep(300); // let the reader fill the queue and block on the next submit

        assertTrue(f.service.cancel(pp.key));

        ProcessingJob.Status finalStatus = f.awaitFinalStatus();
        assertEquals(ProcessingJob.Status.STOPPED, finalStatus);
        assertTrue(f.recordsRead.get() < 100,
                "reader must stop after the executor shuts down, but read " + f.recordsRead.get() + " records");
    }

    @Test
    void serverShutdownDuringALoadStopsReadingAndMarksTheJobStopped() throws Exception {
        Fixture f = new Fixture();
        f.service.submit(f.parameters());

        assertTrue(f.firstWorkerStarted.await(10, TimeUnit.SECONDS), "first record should start loading");
        Thread.sleep(300);

        f.service.onStop();

        ProcessingJob.Status finalStatus = f.awaitFinalStatus();
        assertEquals(ProcessingJob.Status.STOPPED, finalStatus);
        assertTrue(f.recordsRead.get() < 100,
                "reader must stop after the executor shuts down, but read " + f.recordsRead.get() + " records");
    }

    @Test
    void uninterruptedLoadStillCompletes() throws Exception {
        Fixture f = new Fixture(3, false);
        f.service.submit(f.parameters());

        ProcessingJob.Status finalStatus = f.awaitFinalStatus();
        assertEquals(ProcessingJob.Status.COMPLETE, finalStatus);
        assertEquals(4, f.recordsRead.get(), "3 records plus the end-of-file read");
    }

    @Test
    void progressSavesAreThrottledButFinalStatusIsStillSaved() throws Exception {
        int records = 200;
        Fixture f = new Fixture(records, false, 60_000L);
        f.service.submit(f.parameters());

        ProcessingJob.Status finalStatus = f.awaitFinalStatus();
        assertEquals(ProcessingJob.Status.COMPLETE, finalStatus);
        assertEquals(records + 1, f.recordsRead.get());
        assertTrue(f.jobSaves.get() < 10,
                "progress should be saved at most once per interval, but the job was saved " + f.jobSaves.get() + " times");
    }

    @Test
    void zeroProgressIntervalKeepsSavingAfterEveryRecord() throws Exception {
        int records = 20;
        Fixture f = new Fixture(records, false, 0L);
        f.service.submit(f.parameters());

        ProcessingJob.Status finalStatus = f.awaitFinalStatus();
        assertEquals(ProcessingJob.Status.COMPLETE, finalStatus);
        assertTrue(f.jobSaves.get() >= records,
                "expected a save per record, but the job was saved " + f.jobSaves.get() + " times");
    }

    private static class Fixture {
        final CountDownLatch firstWorkerStarted = new CountDownLatch(1);
        final AtomicInteger recordsRead = new AtomicInteger();
        final AtomicInteger jobSaves = new AtomicInteger();
        final AtomicReference<ProcessingJob.Status> lastFinalStatus = new AtomicReference<>();
        final CountDownLatch finalStatusSaved = new CountDownLatch(1);
        final SubstanceBulkLoadService service;
        final Payload payload = new Payload();

        Fixture() throws Exception {
            this(RECORDS_IN_FILE, true);
        }

        Fixture(int recordCount, boolean blockWorkers) throws Exception {
            this(recordCount, blockWorkers, 0L);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Fixture(int recordCount, boolean blockWorkers, long progressSaveIntervalMs) throws Exception {
            payload.id = UUID.randomUUID();

            ProcessingJob job = new ProcessingJob();
            ProcessingJobRepository jobRepository = mock(ProcessingJobRepository.class);
            when(jobRepository.saveAndFlush(any())).thenAnswer(inv -> {
                jobSaves.incrementAndGet();
                ProcessingJob saved = inv.getArgument(0);
                if (saved.id == null) {
                    saved.id = 1L;
                }
                if (saved.status == ProcessingJob.Status.STOPPED || saved.status == ProcessingJob.Status.COMPLETE) {
                    lastFinalStatus.set(saved.status);
                }
                return saved;
            });
            when(jobRepository.findById(anyLong())).thenReturn(Optional.of(job));

            PayloadRepository payloadRepository = mock(PayloadRepository.class);
            when(payloadRepository.findById(any())).thenReturn(Optional.of(payload));
            PayloadService payloadService = mock(PayloadService.class);
            when(payloadService.getPayloadAsInputStream(any(Payload.class)))
                    .thenAnswer(inv -> Optional.of(new ByteArrayInputStream(new byte[0])));

            RecordExtractor extractor = mock(RecordExtractor.class);
            when(extractor.getNextRecord()).thenAnswer(inv ->
                    recordsRead.incrementAndGet() <= recordCount ? new Object() : null);
            RecordExtractorFactory extractorFactory = mock(RecordExtractorFactory.class);
            when(extractorFactory.estimateRecordCount(any())).thenReturn(new Estimate(recordCount, Estimate.TYPE.EXACT));
            when(extractorFactory.createNewExtractorFor(any())).thenReturn(extractor);

            CountDownLatch release = new CountDownLatch(1);
            PersistRecordWorker worker = mock(PersistRecordWorker.class);
            doAnswer(inv -> {
                firstWorkerStarted.countDown();
                if (blockWorkers) {
                    try {
                        release.await(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return null;
            }).when(worker).run();
            PersistRecordWorkerFactory workerFactory = mock(PersistRecordWorkerFactory.class);
            when(workerFactory.newWorkerFor(any(), any(), any(), any())).thenReturn(worker);

            SubstanceBulkLoadServiceConfiguration configuration = mock(SubstanceBulkLoadServiceConfiguration.class);
            when(configuration.getRecordExtractorFactory()).thenReturn(extractorFactory);
            when(configuration.getPersistRecordWorkerFactory(any())).thenReturn(workerFactory);
            when(configuration.getLoadingThreads()).thenReturn(1);
            when(configuration.getMaxQueueSize()).thenReturn(1);
            when(configuration.getProgressSaveIntervalMs()).thenReturn(progressSaveIntervalMs);

            FilteredPrintStream filter = mock(FilteredPrintStream.class);
            when(filter.newFilter(any())).thenReturn(mock(FilteredPrintStream.FilterSession.class));
            ConsoleFilterService consoleFilterService = mock(ConsoleFilterService.class);
            when(consoleFilterService.getStdOutOutputFilter()).thenReturn(filter);
            when(consoleFilterService.getStdErrOutputFilter()).thenReturn(filter);

            AdminService adminService = mock(AdminService.class);
            doAnswer(inv -> {
                ((Runnable) inv.getArgument(1)).run();
                return null;
            }).when(adminService).runAs(any(), any(Runnable.class));

            ObjectProvider<SubstanceRepository> repositoryProvider = mock(ObjectProvider.class);
            when(repositoryProvider.getIfAvailable()).thenReturn(mock(SubstanceRepository.class));

            service = new SubstanceBulkLoadService(
                    configuration,
                    jobRepository,
                    mock(PlatformTransactionManager.class),
                    consoleFilterService,
                    payloadService,
                    adminService,
                    mock(AuditConfig.class),
                    payloadRepository,
                    mock(TaskExecutor.class),
                    mock(ObjectProvider.class),
                    mock(ApplicationEventPublisher.class),
                    repositoryProvider);
        }

        SubstanceBulkLoadService.SubstanceBulkLoadParameters parameters() {
            return SubstanceBulkLoadService.SubstanceBulkLoadParameters.builder()
                    .payload(payload)
                    .preserveOldEditInfo(false)
                    .build();
        }

        /** Waits for the background loader to finish and returns the last terminal status it saved. */
        ProcessingJob.Status awaitFinalStatus() throws InterruptedException {
            long deadline = System.currentTimeMillis() + 60_000;
            ProcessingJob.Status previous = null;
            int stableChecks = 0;
            while (System.currentTimeMillis() < deadline) {
                ProcessingJob.Status current = lastFinalStatus.get();
                boolean loaderFinished = Thread.getAllStackTraces().keySet().stream()
                        .noneMatch(t -> t.isAlive() && isLoaderThread(t));
                if (current != null && current == previous && loaderFinished) {
                    if (++stableChecks >= 3) {
                        return current;
                    }
                } else {
                    stableChecks = 0;
                }
                previous = current;
                Thread.sleep(200);
            }
            return lastFinalStatus.get();
        }

        private static boolean isLoaderThread(Thread t) {
            for (StackTraceElement e : t.getStackTrace()) {
                if (e.getClassName().startsWith(SubstanceBulkLoadService.class.getName() + "$")
                        && "run".equals(e.getMethodName())) {
                    return true;
                }
            }
            return false;
        }
    }
}
