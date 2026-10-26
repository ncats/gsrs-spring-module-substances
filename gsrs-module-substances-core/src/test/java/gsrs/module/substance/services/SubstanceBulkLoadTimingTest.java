package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.AuditConfig;
import gsrs.module.substance.repository.ProcessingJobRepository;
import gsrs.repository.PayloadRepository;
import gsrs.security.AdminService;
import gsrs.service.PayloadService;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.models.ProcessingJob;
import ix.core.stats.Estimate;
import ix.core.stats.Statistics;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubstanceBulkLoadTimingTest {

    @Test
    void terminalStatusesShouldPersistStableStopTimesAndStatistics() throws Exception {
        for (ProcessingJob.Status status : new ProcessingJob.Status[]{
                ProcessingJob.Status.COMPLETE, ProcessingJob.Status.FAILED, ProcessingJob.Status.STOPPED}) {
            ProcessingJob job = new ProcessingJob();
            job.start = 1_000L;
            Statistics stats = new Statistics();
            stats.recordsPersistedSuccess.set(1);
            SubstanceBulkLoadService service = newService(job);

            save(service, stats, status);

            assertEquals(status, job.status);
            assertNotNull(job.stop);
            if (status == ProcessingJob.Status.COMPLETE) {
                assertEquals("Loading and indexing complete", job.message);
            }
            assertEquals(job.stop, stats.getStop());
            assertEquals(job.stop.longValue(), new ObjectMapper().readTree(job.statistics).get("stop").asLong());
            long stoppedAt = job.stop;
            long duration = job._getDurationAsMs();
            double average = stats.getAverageTimeToPersistMS(stoppedAt + 1_000);

            save(service, stats, null);

            assertEquals(status, job.status);
            assertEquals(stoppedAt, job.stop);
            assertEquals(duration, job._getDurationAsMs());
            assertEquals(average, stats.getAverageTimeToPersistMS(stoppedAt + 10_000));
            assertEquals(0L, stats.getEstimatedTimeLeft(stoppedAt + 10_000));
        }
    }

    @Test
    void completedRecordCountsShouldNotStopTheClockBeforeFinalIndexing() {
        ProcessingJob job = new ProcessingJob();
        job.status = ProcessingJob.Status.RUNNING;
        Statistics stats = new Statistics();
        stats.totalRecords = new Estimate(1, Estimate.TYPE.EXACT);
        stats.recordsPersistedSuccess.set(1);
        assertTrue(stats._isDone());

        save(newService(job), stats, null);

        assertEquals(ProcessingJob.Status.RUNNING, job.status);
        assertNull(job.stop);
        assertNull(stats.getStop());
    }

    @Test
    void missingCachedStatisticsShouldNotSkipTheCompletionTransaction() {
        ProcessingJob job = new ProcessingJob();
        job.status = ProcessingJob.Status.RUNNING;
        job.message = "Indexing loaded substances";
        job.statistics = "{\"recordsPersistedSuccess\":109}";
        SubstanceBulkLoadService service = newService(job);

        ProcessingJob saved = ReflectionTestUtils.invokeMethod(service, "saveJobInSeparateTransaction",
                1L, null, ProcessingJob.Status.COMPLETE, null);

        assertSame(job, saved);
        assertEquals(ProcessingJob.Status.COMPLETE, job.status);
        assertNotNull(job.stop);
        assertEquals("Loading and indexing complete", job.message);
        assertEquals("{\"recordsPersistedSuccess\":109}", job.statistics);
    }

    @Test
    void completedJobShouldFreezeStoredStatisticsEvenWhenTheCacheWasMissingAtCompletion() {
        ProcessingJob job = new ProcessingJob();
        job.status = ProcessingJob.Status.COMPLETE;
        job.stop = 3_000L;
        job.statistics = "{\"start\":1000,\"recordsPersistedSuccess\":2}";
        SubstanceBulkLoadService service = newService(job);

        try (MockedStatic<StaticContextAccessor> context = mockStatic(StaticContextAccessor.class)) {
            context.when(() -> StaticContextAccessor.getBean(SubstanceBulkLoadService.class)).thenReturn(service);

            Statistics stats = job.getStatistics();

            assertNotNull(stats);
            assertEquals(3_000L, stats.getStop());
            assertEquals(1_000.0, stats.getAverageTimeToPersistMS(20_000L), 0.0001);
            assertEquals(0L, stats.getEstimatedTimeLeft(20_000L));
        }
    }

    private void save(SubstanceBulkLoadService service, Statistics stats, ProcessingJob.Status status) {
        ReflectionTestUtils.invokeMethod(service, "saveJobInCurrentTransaction", 1L, stats, status, null);
    }

    private SubstanceBulkLoadService newService(ProcessingJob job) {
        ProcessingJobRepository repository = mock(ProcessingJobRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(job));
        when(repository.saveAndFlush(any(ProcessingJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new SubstanceBulkLoadService(
                mock(SubstanceBulkLoadServiceConfiguration.class), repository,
                mock(PlatformTransactionManager.class), mock(ConsoleFilterService.class),
                mock(PayloadService.class), mock(AdminService.class), mock(AuditConfig.class),
                mock(PayloadRepository.class), mock(TaskExecutor.class), mock(ObjectProvider.class),
                mock(ApplicationEventPublisher.class), mock(ObjectProvider.class));
    }
}
