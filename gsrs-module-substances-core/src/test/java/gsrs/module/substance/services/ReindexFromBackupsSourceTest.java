package gsrs.module.substance.services;

import gsrs.events.ReindexEntityEvent;
import ix.core.util.EntityUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReindexFromBackupsSourceTest {

    @Test
    void staleBackupWithoutCurrentDatabaseEntityShouldNotProduceIndexEvent() {
        ReindexFromBackups service = new ReindexFromBackups();
        EntityUtils.Key key = mock(EntityUtils.Key.class);
        when(key.fetch()).thenReturn(Optional.empty());

        Optional<ReindexEntityEvent> event =
                service.currentEntityReindexEvent(UUID.randomUUID(), key);

        assertTrue(event.isEmpty());
    }

    @Test
    void currentDatabaseEntityShouldProduceDeleteFirstIndexEvent() {
        ReindexFromBackups service = new ReindexFromBackups();
        EntityUtils.Key key = mock(EntityUtils.Key.class);
        EntityUtils.EntityWrapper<?> currentEntity = mock(EntityUtils.EntityWrapper.class);
        when(key.fetch()).thenReturn(Optional.of(currentEntity));

        ReindexEntityEvent event = service
                .currentEntityReindexEvent(UUID.randomUUID(), key)
                .orElseThrow();

        assertTrue(event.isRequiresDelete());
        assertSame(currentEntity, event.getOptionalFetchedEntityToReindex().orElseThrow());
        assertFalse(event.isExcludeExternal());
    }

    @Test
    void currentEntityIsFetchedAndPublishedInsideWorkerTransaction() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(new DataSourceTransactionManager(source));
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.setReadOnly(true);

        ReindexFromBackups service = new ReindexFromBackups();
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(service, "eventPublisher", publisher);

        EntityUtils.Key key = mock(EntityUtils.Key.class);
        EntityUtils.EntityWrapper<?> currentEntity = mock(EntityUtils.EntityWrapper.class);
        when(key.fetch()).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return Optional.of(currentEntity);
        });
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(publisher).publishEvent(any(Object.class));

        service.publishCurrentEntityReindexEvent(UUID.randomUUID(), key, transactionTemplate);

        verify(publisher).publishEvent(any(ReindexEntityEvent.class));
    }
}
