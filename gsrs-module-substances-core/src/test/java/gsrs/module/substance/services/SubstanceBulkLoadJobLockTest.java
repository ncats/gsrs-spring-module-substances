package gsrs.module.substance.services;

import gsrs.AuditConfig;
import gsrs.module.substance.repository.ProcessingJobRepository;
import gsrs.repository.PayloadRepository;
import gsrs.security.AdminService;
import gsrs.service.PayloadService;
import ix.core.models.Keyword;
import ix.core.models.ProcessingJob;
import ix.core.models.ProcessingJobUtils;
import ix.core.stats.Estimate;
import ix.core.stats.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SubstanceBulkLoadJobLockTest {

    @Test
    void finalPersistenceCallbackShouldNotUpdateJobInsideRecordTransaction() {
        ProcessingJobRepository jobRepository = mock(ProcessingJobRepository.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        SubstanceBulkLoadService service = new SubstanceBulkLoadService(
                mock(SubstanceBulkLoadServiceConfiguration.class),
                jobRepository,
                transactionManager,
                mock(ConsoleFilterService.class),
                mock(PayloadService.class),
                mock(AdminService.class),
                mock(AuditConfig.class),
                mock(PayloadRepository.class),
                mock(TaskExecutor.class),
                mock(ObjectProvider.class),
                mock(ApplicationEventPublisher.class),
                mock(ObjectProvider.class));

        String jobKey = "lock-regression";
        ProcessingJob job = new ProcessingJob();
        job.id = 1L;
        job.addKeyword(new Keyword(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY, jobKey));

        Statistics statistics = new Statistics();
        statistics.totalRecords = new Estimate(1, Estimate.TYPE.EXACT);
        service.storeStatisticsForJob(jobKey, statistics);

        service.applyStatisticsChangeForJob(job, Statistics.CHANGE.ADD_PE_GOOD);

        verifyNoInteractions(jobRepository, transactionManager);
    }
}
