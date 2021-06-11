package example.reindex;

import example.GsrsModuleSubstanceApplication;
import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.BackupEntityProcessorListener;
import gsrs.backup.BackupEventListener;
import gsrs.events.BackupEvent;
import gsrs.events.MaintenanceModeEvent;
import gsrs.events.ReindexEntityEvent;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.module.substance.services.ReindexFromBackups;
import gsrs.repository.BackupRepository;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.BackupEntity;
import ix.core.models.BaseModel;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;


@RecordApplicationEvents()
//@SpringBootTest(classes = {GsrsModuleSubstanceApplication.class,  GsrsEntityTestConfiguration.class})
//@ActiveProfiles("test")
//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
//@GsrsJpaTest(dirtyMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
//@ContextConfiguration(classes ={ApplicationContextRunner.class, BackupEventListener.class})
public class ReIndexAllTest extends AbstractSubstanceJpaEntityTest {



    @Autowired
    PlatformTransactionManager platformTransactionManager;


//    @Autowired
//    ApplicationContextRunner contextRunner



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
//        applicationEvents.stream().forEach(System.out::println);
//        System.out.println("Just backup events:");
        assertEquals(2, applicationEvents.stream().filter( e -> e instanceof BackupEvent).peek(System.out::println).count());


    }

    private void backup(BaseModel o) throws Exception {
        BackupEntity be = new BackupEntity();
        be.setInstantiated(o);
        backupRepository.saveAndFlush(be);

    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void reindex(@Autowired ApplicationEvents applicationEvents) throws Exception {

        SubstanceBuilder s1 = new SubstanceBuilder()
                .setUUID(UUID.fromString("2947b944-ab26-47d4-b160-f0d8149e4d77"))
                .addName("sub1");


        SubstanceBuilder s2 = new SubstanceBuilder()
                .setUUID(UUID.fromString("ee9d929d-41a8-43ec-a041-f90d0e5dc21c"))
                .addName("sub2");




        backup(assertCreated(s1.buildJson()));
        backup(assertCreated(s2.buildJson()));

        SubstanceBuilder.from(substanceEntityService.get(UUID.fromString("2947b944-ab26-47d4-b160-f0d8149e4d77")).get().toFullJsonNode());

        applicationEvents.clear();

        ReindexFromBackups sut = new ReindexFromBackups();
        AutowireHelper.getInstance().autowire(sut);
        SchedulerPlugin.TaskListener listener = new SchedulerPlugin.TaskListener();
        sut.execute(listener);
        //2 substances each with ( 1 sub, 1 name, 1 ref) = 6 indexed events
        assertEquals(6L, applicationEvents.stream(ReindexEntityEvent.class).count());

        assertEquals(1L, applicationEvents.stream(MaintenanceModeEvent.class).filter(e -> e.getSource().isInMaintenanceMode()).count());
        assertEquals(1L, applicationEvents.stream(MaintenanceModeEvent.class).filter(e -> !e.getSource().isInMaintenanceMode()).count());

    }
}
