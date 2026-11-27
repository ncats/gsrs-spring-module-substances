package gsrs.module.substance.services;

import gsrs.events.ReindexEntityEvent;
import ix.core.models.BackupEntity;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.Name;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.HashSet;
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
    void currentDatabaseEntityShouldProduceAddOnlyEventForWipedIndexes() {
        ReindexFromBackups service = new ReindexFromBackups();
        EntityUtils.Key key = mock(EntityUtils.Key.class);
        EntityUtils.EntityWrapper<?> currentEntity = mock(EntityUtils.EntityWrapper.class);
        when(key.fetch()).thenReturn(Optional.of(currentEntity));

        ReindexEntityEvent event = service
                .currentEntityReindexEvent(UUID.randomUUID(), key)
                .orElseThrow();

        assertFalse(event.isRequiresDelete());
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

        EntityUtils.EntityWrapper<?> currentEntity = mock(EntityUtils.EntityWrapper.class);
        ReindexFromBackups service = new ReindexFromBackups() {
            @Override
            Optional<ReindexEntityEvent> currentEntityReindexEvent(UUID id, EntityUtils.Key key) {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                return Optional.of(new ReindexEntityEvent(id, key, Optional.of(currentEntity), false));
            }
        };
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(service, "eventPublisher", publisher);

        Name name = new Name();
        name.uuid = UUID.randomUUID();
        name.name = "Example";
        BackupEntity backup = mock(BackupEntity.class);
        when(backup.getOptionalInstantiated()).thenReturn(Optional.of(name));
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(publisher).publishEvent(any(Object.class));

        transactionTemplate.executeWithoutResult(status ->
                service.reindexBackup(UUID.randomUUID(), backup, new HashSet<>()));

        verify(publisher).publishEvent(any(ReindexEntityEvent.class));
    }
}
