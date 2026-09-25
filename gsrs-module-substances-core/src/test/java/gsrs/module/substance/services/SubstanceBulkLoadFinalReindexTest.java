package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.AuditConfig;
import gsrs.events.ReindexEntityEvent;
import gsrs.module.substance.repository.ProcessingJobRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.PayloadRepository;
import gsrs.security.AdminService;
import gsrs.service.PayloadService;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubstanceBulkLoadFinalReindexTest {

    @Test
    void completedBulkLoadShouldDeleteFirstReindexEachSuccessfulUuidOnce() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        UUID uuid = UUID.randomUUID();

        Substance substance = new Substance();
        substance.setUuid(uuid);
        SubstanceRepository substanceRepository = mock(SubstanceRepository.class);
        when(substanceRepository.findById(uuid)).thenReturn(Optional.of(substance));

        SubstanceBulkLoadService service =
                newService(mock(TaskExecutor.class), publisher, substanceRepository);
        SubstanceBulkLoadService.BulkLoadServiceCallBackImpl callback =
                service.new BulkLoadServiceCallBackImpl("job-key");
        ObjectMapper mapper = new ObjectMapper();

        callback.persistedSuccess(mapper.createObjectNode().put("uuid", uuid.toString()));
        callback.persistedSuccess(mapper.createObjectNode().put("uuid", uuid.toString()));
        callback.reindexPersistedSubstances();

        ArgumentCaptor<ReindexEntityEvent> captor = ArgumentCaptor.forClass(ReindexEntityEvent.class);
        verify(publisher, times(1)).publishEvent(captor.capture());

        ReindexEntityEvent event = captor.getValue();
        assertTrue(event.isRequiresDelete(), "reconciliation must delete existing documents first");
        assertTrue(event.getOptionalEntityWrapper().isPresent(),
                "the resolved entity must travel with the event so the listener cannot no-op");
        assertEquals(uuid, event.getEntityKey().getIdNative());
    }

    @Test
    void substancesMissingFromTheDatabaseShouldNotBeReindexed() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        UUID uuid = UUID.randomUUID();
        SubstanceRepository substanceRepository = mock(SubstanceRepository.class);
        when(substanceRepository.findById(uuid)).thenReturn(Optional.empty());

        SubstanceBulkLoadService service =
                newService(mock(TaskExecutor.class), publisher, substanceRepository);
        SubstanceBulkLoadService.BulkLoadServiceCallBackImpl callback =
                service.new BulkLoadServiceCallBackImpl("job-key");

        callback.persistedSuccess(new ObjectMapper().createObjectNode().put("uuid", uuid.toString()));
        callback.reindexPersistedSubstances();

        verify(publisher, never()).publishEvent(any(ReindexEntityEvent.class));
    }

    @Test
    void reconciliationShouldWaitUntilAsynchronousIndexingHasDrained() throws Exception {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.initialize();

        SubstanceBulkLoadService service = newService(taskExecutor,
                mock(ApplicationEventPublisher.class), mock(SubstanceRepository.class));

        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean indexingFinished = new AtomicBoolean(false);
        taskExecutor.execute(() -> {
            started.countDown();
            try {
                Thread.sleep(1_500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            indexingFinished.set(true);
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));

        try {
            service.awaitAsyncIndexingQuiescence();
            assertTrue(indexingFinished.get(),
                    "reconciliation must not start while index events are still running");
        } finally {
            taskExecutor.shutdown();
        }
    }

    @Test
    void reindexEventsMustBePublishedWhileTheTransactionIsStillOpen() {
        UUID uuid = UUID.randomUUID();
        Substance substance = new Substance();
        substance.setUuid(uuid);
        SubstanceRepository substanceRepository = mock(SubstanceRepository.class);
        when(substanceRepository.findById(uuid)).thenReturn(Optional.of(substance));

        AtomicBoolean transactionClosed = new AtomicBoolean(false);
        AtomicBoolean publishedAfterClose = new AtomicBoolean(false);

        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        doAnswer(invocation -> {
            transactionClosed.set(true);
            return null;
        }).when(transactionManager).commit(any());
        doAnswer(invocation -> {
            transactionClosed.set(true);
            return null;
        }).when(transactionManager).rollback(any());

        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        doAnswer(invocation -> {
            if (transactionClosed.get()) {
                publishedAfterClose.set(true);
            }
            return null;
        }).when(publisher).publishEvent(any(ReindexEntityEvent.class));

        SubstanceBulkLoadService service = newService(mock(TaskExecutor.class), publisher,
                substanceRepository, transactionManager);
        SubstanceBulkLoadService.BulkLoadServiceCallBackImpl callback =
                service.new BulkLoadServiceCallBackImpl("job-key");

        callback.persistedSuccess(new ObjectMapper().createObjectNode().put("uuid", uuid.toString()));
        callback.reindexPersistedSubstances();

        verify(publisher, times(1)).publishEvent(any(ReindexEntityEvent.class));
        assertFalse(publishedAfterClose.get(),
                "the reindex listeners run synchronously, so the event must be published before the "
                        + "transaction closes or lazy collections throw LazyInitializationException");
    }

    @SuppressWarnings("unchecked")
    private SubstanceBulkLoadService newService(TaskExecutor taskExecutor,
                                                ApplicationEventPublisher publisher,
                                                SubstanceRepository substanceRepository) {
        return newService(taskExecutor, publisher, substanceRepository,
                mock(PlatformTransactionManager.class));
    }

    @SuppressWarnings("unchecked")
    private SubstanceBulkLoadService newService(TaskExecutor taskExecutor,
                                                ApplicationEventPublisher publisher,
                                                SubstanceRepository substanceRepository,
                                                PlatformTransactionManager transactionManager) {
        ObjectProvider<SubstanceRepository> substanceRepositoryProvider = mock(ObjectProvider.class);
        when(substanceRepositoryProvider.getIfAvailable()).thenReturn(substanceRepository);
        return new SubstanceBulkLoadService(
                mock(SubstanceBulkLoadServiceConfiguration.class),
                mock(ProcessingJobRepository.class),
                transactionManager,
                mock(ConsoleFilterService.class),
                mock(PayloadService.class),
                mock(AdminService.class),
                mock(AuditConfig.class),
                mock(PayloadRepository.class),
                taskExecutor,
                mock(ObjectProvider.class),
                publisher,
                substanceRepositoryProvider);
    }
}
