package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.sneak.Sneak;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.cache.GsrsCache;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.autoconfigure.GsrsSubstanceModuleAutoConfiguration;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.ETagRepository;
import gsrs.repository.EditRepository;
import gsrs.repository.GroupRepository;
import gsrs.service.ExportService;
import gsrs.service.GsrsEntityService;
import gsrs.startertests.*;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import gsrs.startertests.jupiter.ClearAuditorBeforeEachExtension;
import gsrs.startertests.jupiter.ResetAllEntityServicesBeforeEachExtension;
import ix.core.models.Group;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.Substance;
import ix.seqaln.service.SequenceIndexerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ContextConfiguration(classes = { GsrsEntityTestConfiguration.class, GsrsControllerConfiguration.class},
        initializers= { AbstractSubstanceJpaEntityTest2.Initializer.class}
)
//@SpringBootTest

@Import({AbstractSubstanceJpaEntityTest.TestConfig.class,  GsrsSubstanceModuleAutoConfiguration.class})
public abstract class AbstractSubstanceJpaEntityTest2 extends AbstractGsrsJpaEntityJunit5Test {
    @TestConfiguration
//    @AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)
    public static class TestConfig{
        @Bean
        @Primary
        TestGsrsValidatorFactory gsrsValidatorFactory(){
            return new TestGsrsValidatorFactory();
        }

        @Bean
        SubstanceEntityService substanceEntityService(){
            return new SubstanceEntityServiceImpl();
        }
        @Bean
        @Primary
        TestIndexValueMakerFactory indexValueMakerFactory(){
            return new TestIndexValueMakerFactory();
        }

        @Bean
        @Primary
        TestEntityProcessorFactory entityProcessorFactory(){
            return new TestEntityProcessorFactory();
        }


        @Bean
        @Primary
        public Scheduler getScheduler() throws SchedulerException {
            return StdSchedulerFactory.getDefaultScheduler();
        }


    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "ix.structure.base=" + tempDir+"/structure",
                    "ix.sequence.base=" + tempDir+"/sequence"
            ).applyTo(context);
        }
    }
    //    @Order(Ordered.HIGHEST_PRECEDENCE)
//    @TestConfiguration
//    @AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)
//    @ConditionalOnMissingBean(value = ETagRepository.class)
////    @EnableJpaRepositories(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
//    public static class JpaScanIfNeededConfiguration{
//
//    }

    public interface EntityManagerFacade{

        <T> T persistAndFlush(T entity);
        <T> T persist(T entity);

        static EntityManagerFacade wrap(EntityManager em){
            return new EntityManagerFacade() {
                @Override
                public <T> T persistAndFlush(T entity) {
                    em.persist(entity);
                    em.flush();
                    return entity;
                }
                @Override
                public <T> T persist(T entity) {
                    em.persist(entity);

                    return entity;
                }
            };
        }

        static EntityManagerFacade wrap(TestEntityManager em){
            return new EntityManagerFacade() {
                @Override
                public <T> T persistAndFlush(T entity) {
                    return em.persistAndFlush(entity);
                }
                @Override
                public <T> T persist(T entity) {
                    return em.persist(entity);
                }
            };
        }
    }




    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @Autowired
    protected EditRepository editRepository;

    @Autowired
    protected SubstanceRepository substanceRepository;

    @Autowired
    protected GroupRepository groupRepository;

    protected Principal admin;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @MockBean
    protected GsrsCache mockGsrsCache;


    @MockBean
    protected ExportService mockExportService;

    @MockBean
    protected TaskExecutor mockTaskExecutor;

    @MockBean
    protected GsrsExportConfiguration mockGsrsExportConfiguration;

    @Autowired
    protected ETagRepository eTagRepository;

    @Autowired
    @RegisterExtension
    protected ResetAllEntityServicesBeforeEachExtension resetAllEntityServicesBeforeEachExtension;

    protected abstract EntityManagerFacade getEntityManagerFacade();

    private EntityManagerFacade entityManagerFacade;
    @BeforeEach
    public void init(){
        entityManagerFacade = getEntityManagerFacade();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s-> {
            admin = createUser("admin", Role.values());

            //some  integration tests will make validation messages which will get assigned an "admin" access group
            Group g = groupRepository.saveAndFlush(new Group("admin"));

        });

    }

    protected Principal createUser(String username, Role... roles){
        Principal user = new Principal(username, null);

        UserProfile up = new UserProfile();
        up.setRoles(Arrays.asList(roles));
        up.user= user;
        entityManagerFacade.persist(up);

        return user;
    }

    protected Substance assertCreated(JsonNode json){
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return transactionTemplate.execute( stauts -> {
            try {
                return ensurePass(substanceEntityService.createEntity(json));
            } catch (Exception e) {
                return Sneak.sneakyThrow(e);
            }
        });
    }

    protected Substance assertUpdated(JsonNode json){
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return transactionTemplate.execute( stauts -> {
            try {
                return ensurePass(substanceEntityService.updateEntity(json));
            } catch (Exception e) {
                return Sneak.sneakyThrow(e);
            }
        });
    }
    protected static <T> T ensurePass(GsrsEntityService.UpdateResult<T> creationResult){
        ValidationResponse<T> resp = creationResult.getValidationResponse();
        assertTrue(resp.isValid(), ()->"response is not valid "+ resp.getValidationMessages());
        assertTrue(!resp.hasError(), ()->"response has error "+ resp.getValidationMessages());

        assertEquals(GsrsEntityService.UpdateResult.STATUS.UPDATED, creationResult.getStatus(), ()->"was not updated "+ resp.getValidationMessages());

        return creationResult.getUpdatedEntity();
    }
    protected static <T> T ensurePass(GsrsEntityService.CreationResult<T> creationResult){
        ValidationResponse<T> resp = creationResult.getValidationResponse();
        assertTrue(resp.isValid(), ()->"response is not valid "+ resp.getValidationMessages());
        assertTrue(!resp.hasError(), ()->"response has error "+ resp.getValidationMessages());

        assertTrue(creationResult.isCreated(), ()->"was not created "+ resp.getValidationMessages());

        T sub= creationResult.getCreatedEntity();
        System.out.println("created sub uuid = " + ((Substance)sub).getUuid());
        return sub;
    }
}
