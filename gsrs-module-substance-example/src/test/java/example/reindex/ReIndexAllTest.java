package example.reindex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Component;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.events.BackupEvent;
import gsrs.events.BeginReindexEvent;
import gsrs.events.EndReindexEvent;
import gsrs.events.MaintenanceModeEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.events.ReindexOperationEvent;
import gsrs.module.substance.services.ReindexFromBackups;
import gsrs.repository.BackupRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import ix.core.models.BackupEntity;
import ix.core.models.BaseModel;
import ix.ginas.modelBuilders.SubstanceBuilder;


@RecordApplicationEvents()
@Import(ReIndexAllTest.TestConfig.class)
public class ReIndexAllTest extends AbstractSubstanceJpaEntityTest {

    /**
     * This listener specifically listens for events that relate to reindexing
     * and keeps track of them for tests. Unlike {@link ApplicationEvents}, this
     * listener will listen for all indexing events regardless of whether they
     * were spawned by the same thread that executed the test or not.
     * 
     * @author tyler
     *
     */
    @Component
    public static class ReindexEventEventListener {
        private Set<Object> events= new LinkedHashSet<>();
        @EventListener({ReindexOperationEvent.class,
            MaintenanceModeEvent.class})
        public synchronized void onReindexRelatedEvent(Object event){
            events.add(event);
        }
        public void clear() {
            events.clear();
        }
        public <T> Stream<T> stream(Class<T> type){
            return (Stream<T>) events.stream().filter(c->type.isAssignableFrom(c.getClass()));
        }
    }

    @TestConfiguration
    public static class TestConfig{
        @Bean
        public ReindexFromBackups reindexFromBackups(){
            return new ReindexFromBackups();
        }
        @Bean
        public ReindexEventEventListener reindexEventListener(){
            return new ReindexEventEventListener();
        }
    }

    @Autowired
    ReindexEventEventListener eventListener;

    @Autowired
    PlatformTransactionManager platformTransactionManager;

    @Autowired
    ReindexFromBackups reindexFromBackups;



    TransactionTemplate transactionTemplate;

    @Autowired
    BackupRepository backupRepository;

    @BeforeEach
    public void setUp() throws Exception {
        transactionTemplate = new TransactionTemplate(platformTransactionManager);

    }
    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void persistCreatesBackupEvents(@Autowired ApplicationEvents applicationEvents){
        applicationEvents.clear();

        SubstanceBuilder s1 = new SubstanceBuilder()
                .setUUID(UUID.fromString("2947b944-ab26-47d4-b160-f0d8149e4d77"))
                .addName("sub1");


        SubstanceBuilder s2 = new SubstanceBuilder()
                .setUUID(UUID.fromString("ee9d929d-41a8-43ec-a041-f90d0e5dc21c"))
                .addName("sub2");

        assertCreated(s1.buildJson());
        assertCreated(s2.buildJson());
        System.out.println("=====");
        applicationEvents.stream(BackupEvent.class).forEach(System.out::println);
        //        System.out.println("Just backup events:");
        assertThat( applicationEvents.stream(BackupEvent.class).map(e->e.getSource().getRefid()).collect(Collectors.toSet()))
        .contains("ee9d929d-41a8-43ec-a041-f90d0e5dc21c","2947b944-ab26-47d4-b160-f0d8149e4d77");



    }

    private void backup(BaseModel o) throws Exception {
        BackupEntity be = new BackupEntity();
        be.setInstantiated(o);
        backupRepository.saveAndFlush(be);

    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    //this time out is here to fail the test if it takes > 100 secs
    //this can happen if the Reindex from backups isn't set up correctly and the events don't fire
    //so the backup service blocks forever waiting for that event that never comes
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    public void reindex(@Autowired ApplicationEvents applicationEvents) throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setReadOnly(true);
        tx.executeWithoutResult(stat->{
            try {
                SubstanceBuilder s1 = new SubstanceBuilder()
                        .setUUID(UUID.fromString("2947b944-ab26-47d4-b160-f0d8149e4d77"))
                        .addName("sub1");


                SubstanceBuilder s2 = new SubstanceBuilder()
                        .setUUID(UUID.fromString("ee9d929d-41a8-43ec-a041-f90d0e5dc21c"))
                        .addName("sub2");




                backup(assertCreated(s1.buildJson()));
                backup(assertCreated(s2.buildJson()));

                SubstanceBuilder.from(substanceEntityService.get(UUID.fromString("2947b944-ab26-47d4-b160-f0d8149e4d77")).get().toFullJsonNode());

                eventListener.clear();
            }catch(Exception e) {
                throw new RuntimeException(e);
            }
        });


        SchedulerPlugin.TaskListener listener = new SchedulerPlugin.TaskListener();
        UUID uuid = UUID.randomUUID();
        reindexFromBackups.execute(uuid, listener);

        Thread.sleep(20);
        //2 substances each with ( 1 sub, 1 name, 1 ref) = 6 indexed events
        assertEquals(6L, eventListener.stream(ReindexEntityEvent.class).count());
        assertEquals(1L, eventListener.stream(BeginReindexEvent.class).count());
        assertEquals(1L, eventListener.stream(EndReindexEvent.class).count());

        assertEquals(1L, eventListener.stream(MaintenanceModeEvent.class).filter(e -> e.getSource().isInMaintenanceMode()).count());
        assertEquals(1L, eventListener.stream(MaintenanceModeEvent.class).filter(e -> !e.getSource().isInMaintenanceMode()).count());

    }
}
