package gsrs.module.substance.services;

import gsrs.events.ReindexEntityEvent;
import ix.core.util.EntityUtils;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
}
