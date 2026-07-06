package example.reindex;

import gsrs.events.EndReindexEvent;
import gsrs.events.IncrementReindexEvent;
import gsrs.events.MaintenanceModeEvent;
import gsrs.events.ReindexOperationEvent;
import gsrs.module.substance.services.ReindexFromBackups;
import gsrs.scheduledTasks.SchedulerPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReIndexAllTest {

    /**
     * Listener for reindex-related events. Unlike {@code ApplicationEvents}, it
     * can observe events produced on worker threads.
     */
    static class ReindexEventEventListener {
        private final Set<Object> events = new LinkedHashSet<>();

        @EventListener({ReindexOperationEvent.class, MaintenanceModeEvent.class})
        public synchronized void onReindexRelatedEvent(Object event) {
            events.add(event);
        }

        public synchronized void clear() {
            events.clear();
        }

        @SuppressWarnings("unchecked")
        public synchronized <T> Stream<T> stream(Class<T> type) {
            return (Stream<T>) events.stream().filter(event -> type.isAssignableFrom(event.getClass()));
        }
    }

    @Test
    @DisplayName("Listener keeps unique events and supports typed lookup")
    void listenerStoresDistinctEventsAndFiltersByType() {
        ReindexEventEventListener listener = new ReindexEventEventListener();

        FakeReindexEvent event1 = new FakeReindexEvent("first");
        FakeReindexEvent event2 = new FakeReindexEvent("second");
        FakeMaintenanceEvent maintenance = new FakeMaintenanceEvent(true);

        listener.onReindexRelatedEvent(event1);
        listener.onReindexRelatedEvent(event1);
        listener.onReindexRelatedEvent(event2);
        listener.onReindexRelatedEvent(maintenance);

        assertEquals(2L, listener.stream(FakeReindexEvent.class).count());
        assertEquals(1L, listener.stream(FakeMaintenanceEvent.class).count());
        assertEquals(3L, listener.stream(Object.class).count());
    }

    @Test
    @DisplayName("Listener clear removes all accumulated events")
    void clearRemovesEvents() {
        ReindexEventEventListener listener = new ReindexEventEventListener();
        listener.onReindexRelatedEvent(new FakeReindexEvent("one"));
        listener.onReindexRelatedEvent(new FakeMaintenanceEvent(false));

        assertTrue(listener.stream(Object.class).count() > 0);

        listener.clear();
        assertEquals(0L, listener.stream(Object.class).count());
    }

    @Test
    @DisplayName("Typed stream can be used for deterministic grouped assertions")
    void streamCanBeGroupedByType() {
        ReindexEventEventListener listener = new ReindexEventEventListener();
        listener.onReindexRelatedEvent(new FakeReindexEvent("one"));
        listener.onReindexRelatedEvent(new FakeReindexEvent("two"));
        listener.onReindexRelatedEvent(new FakeMaintenanceEvent(true));

        Set<String> names = listener.stream(FakeReindexEvent.class)
                .map(FakeReindexEvent::getName)
                .collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder("one", "two");
    }

    @Test
    @DisplayName("End event counts down registered latch and removes it")
    void endEventCountsDownRegisteredLatchAndRemovesIt() {
        ReindexFromBackups service = new ReindexFromBackups();
        UUID reindexId = UUID.randomUUID();
        CountDownLatch latch = new CountDownLatch(1);
        latchMap(service).put(reindexId, latch);

        service.endReindex(new EndReindexEvent(reindexId));

        assertEquals(0L, latch.getCount());
        assertTrue(latchMap(service).isEmpty());
    }

    @Test
    @DisplayName("Increment events update task listener progress when progress state exists")
    void incrementEventUpdatesTaskListenerProgress() throws Exception {
        ReindexFromBackups service = new ReindexFromBackups();
        SchedulerPlugin.TaskListener listener = new SchedulerPlugin.TaskListener();
        UUID reindexId = UUID.randomUUID();
        listenerMap(service).put(reindexId, taskProgress(listener, reindexId, 2L));

        service.endReindex(new IncrementReindexEvent(reindexId));

        assertEquals("Indexed: 1 of 2", listener.getMessage());
    }

    @Test
    @DisplayName("End and increment events for unknown ids are ignored")
    void eventHandlersIgnoreUnknownIds() {
        ReindexFromBackups service = new ReindexFromBackups();

        service.endReindex(new EndReindexEvent(UUID.randomUUID()));
        service.endReindex(new IncrementReindexEvent(UUID.randomUUID()));
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, CountDownLatch> latchMap(ReindexFromBackups service) {
        return (Map<UUID, CountDownLatch>) ReflectionTestUtils.getField(service, "latchMap");
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Object> listenerMap(ReindexFromBackups service) {
        return (Map<UUID, Object>) ReflectionTestUtils.getField(service, "listenerMap");
    }

    private static Object taskProgress(SchedulerPlugin.TaskListener listener,
                                       UUID reindexId,
                                       long totalCount) throws Exception {
        Class<?> progressClass = Class.forName("gsrs.module.substance.services.ReindexFromBackups$TaskProgress");
        Constructor<?> constructor = progressClass.getDeclaredConstructor(
                SchedulerPlugin.TaskListener.class, UUID.class, long.class, long.class);
        constructor.setAccessible(true);
        return constructor.newInstance(listener, reindexId, totalCount, 0L);
    }

    private static class FakeReindexEvent implements ReindexOperationEvent {
        private final UUID id = UUID.randomUUID();
        private final String name;

        private FakeReindexEvent(String name) {
            this.name = name;
        }

        @Override
        public UUID getReindexId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    private static class FakeMaintenanceEvent {
        private final boolean enabled;

        private FakeMaintenanceEvent(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}
