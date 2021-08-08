package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.io.InputStreamSupplier;
import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.common.yield.Yield;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.cache.GsrsCache;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.autoconfigure.GsrsSubstanceModuleAutoConfiguration;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.payload.PayloadController;
import gsrs.repository.ETagRepository;
import gsrs.repository.EditRepository;
import gsrs.repository.GroupRepository;
import gsrs.service.ExportService;
import gsrs.service.GsrsEntityService;
import gsrs.startertests.*;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import gsrs.startertests.jupiter.ResetAllEntityServicesBeforeEachExtension;
import ix.core.models.Group;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.core.util.EntityUtils;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parent Super-class of that should be used to
 * test Substances interacting with a test database.
 * Subclasses will have further configurations for different
 * granularity tests unit vs end to end etc.
 */
@ActiveProfiles("test")
@ContextConfiguration(classes = { GsrsEntityTestConfiguration.class, GsrsControllerConfiguration.class},
        initializers= { AbstractSubstanceJpaEntityTestSuperClass.Initializer.class}
)
//@SpringBootTest

@Import({AbstractSubstanceJpaEntityTest.TestConfig.class,  GsrsSubstanceModuleAutoConfiguration.class})
public abstract class AbstractSubstanceJpaEntityTestSuperClass extends AbstractGsrsJpaEntityJunit5Test {
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

    @MockBean
    protected PayloadController payloadController;

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

    protected TransactionTemplate newTransactionTemplate(){
        return new TransactionTemplate(transactionManager);
    }

    /**
     * Load the specially formatted GSRS format file (often with {@code .gsrs} extension although some
     * GSRS 1.x verion files have the extension {@code .ginas}.  Either way, the file is a GZIPPED
     * encoded file of tab delimited text where each row contains the 1 Substance JSON record one line per record along
     * with additional metadata.
     * @param gsrsFile the GSRS file to load.
     * @return a List of {@link gsrs.service.GsrsEntityService.CreationResult}s one per record in the GSRS file.
     * @throws IOException if there is a problem parsing the file.
     */
    protected List<GsrsEntityService.CreationResult<Substance>> loadGsrsFile(Resource gsrsFile, Substance.SubstanceClass... substanceClasses) throws IOException {
        return loadGsrsFile(gsrsFile.getFile(), substanceClasses);
    }
    /**
     * Load the specially formatted GSRS format file (often with {@code .gsrs} extension although some
     * GSRS 1.x verion files have the extension {@code .ginas}.  Either way, the file is a GZIPPED
     * encoded file of tab delimited text where each row contains the 1 Substance JSON record one line per record along
     * with additional metadata.
     * @param gsrsFile the GSRS file to load.
     * @return a List of {@link gsrs.service.GsrsEntityService.CreationResult}s one per record in the GSRS file.
     * @throws IOException if there is a problem parsing the file.
     */
    protected List<GsrsEntityService.CreationResult<Substance>> loadGsrsFile(Resource gsrsFile) throws IOException {
        return loadGsrsFile(gsrsFile.getFile());
    }
    /**
     * Load the specially formatted GSRS format file (often with {@code .gsrs} extension although some
     * GSRS 1.x verion files have the extension {@code .ginas}.  Either way, the file is a GZIPPED
     * encoded file of tab delimited text where each row contains the 1 Substance JSON record one line per record along
     * with additional metadata.
     * @param gsrsFile the GSRS file to load.
     * @return a List of {@link gsrs.service.GsrsEntityService.CreationResult}s one per record in the GSRS file.
     * @throws IOException if there is a problem parsing the file.
     */
    protected List<GsrsEntityService.CreationResult<Substance>> loadGsrsFile(File gsrsFile) throws IOException {
        return loadGsrsFile(gsrsFile, null);
    }
        /**
         * Load the specially formatted GSRS format file (often with {@code .gsrs} extension although some
         * GSRS 1.x verion files have the extension {@code .ginas}.  Either way, the file is a GZIPPED
         * encoded file of tab delimited text where each row contains the 1 Substance JSON record one line per record along
         * with additional metadata.
         * @param gsrsFile the GSRS file to load.
         * @param substanceClasses a list of {@link ix.ginas.models.v1.Substance.SubstanceClass}es to include.
         *                         only the records in the given file of these kinds will be loaded.
         *                         If this var args is empty or null then all records in the file will be loaded.
         * @return a List of {@link gsrs.service.GsrsEntityService.CreationResult}s one per record in the GSRS file.
         * @throws IOException if there is a problem parsing the file.
         */
    protected List<GsrsEntityService.CreationResult<Substance>> loadGsrsFile(File gsrsFile, Substance.SubstanceClass... substanceClasses) throws IOException {

            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            List<GsrsEntityService.CreationResult<Substance>> list = new ArrayList<>();

        yieldSubstancesFromGsrsFile(gsrsFile, substanceClasses)
                .forEach(json->{
                    list.add(transactionTemplate.execute(status ->{
                        try {
                            GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(json);
                            //full fetch
                            if(result.isCreated()){
                                result.getCreatedEntity().toFullJsonNode();
                            }
                            return result;
                        }catch(IOException e){
                            return Sneak.sneakyThrow(e);
                        }
                    }));
                });


            return list;

    }


    /**
     * Parse a Gsrs encoded Spring Resource object and return a {@link Yield}
     * for each JsonNode parsed object.
     * @param gsrsFile the file to parse.
     * @param substanceClasses a list of {@link ix.ginas.models.v1.Substance.SubstanceClass}es to include.
     *          only the records in the given file of these kinds will be included in the Yield.
     *          If this var args is empty or null then all records in the file will be included.
     * @return a {@link Yield}
     * @throws IOException
     */
    protected Yield<JsonNode> yieldSubstancesFromGsrsFile(Resource gsrsFile,Substance.SubstanceClass... substanceClasses) throws IOException{
        return yieldSubstancesFromGsrsFile(gsrsFile.getFile(), substanceClasses);
    }

    protected Yield<JsonNode> yieldSubstancesFromGsrsFile(File gsrsFile, Substance.SubstanceClass... substanceClass){
        Yield<JsonNode> yield=null;

        yield = Yield.create(r->{
                String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(InputStreamSupplier.forFile(gsrsFile).get()))) {

                ObjectMapper mapper = new ObjectMapper();
                Pattern gsrsFilePattern = Pattern.compile("\t");
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty() || line.startsWith("#")) {
                            //skip
                            continue;
                        }
                        String[] cols = gsrsFilePattern.split(line);
                        String json = cols[2];
                        boolean include=true;
                        if(substanceClass !=null && substanceClass.length >0){
                            include=false;
                            for(Substance.SubstanceClass c : substanceClass){
                                if(json.contains("\"substanceClass\":\""+c.jsonValue()+ "\"")){
                                    include=true;
                                    break;
                                }
                            }
                        }
                        if(!include){
                            continue;
                        }
                        r.returning(mapper.readTree(json));

                    }
                }catch(IOException e){
                    Sneak.sneakyThrow(e);
                }finally{
                    r.signalComplete();
                }

            });


    return yield;
    }
    protected Substance assertCreated(JsonNode json){
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return transactionTemplate.execute( status -> {
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
        EntityUtils.EntityWrapper.of(sub).toFullJson();
        return sub;
    }

    protected static <T> Collection<GsrsEntityService.CreationResult<T>> ensurePass(Collection<GsrsEntityService.CreationResult<T>> creationResults){
        for(GsrsEntityService.CreationResult<T> creationResult: creationResults) {
            ValidationResponse<T> resp = creationResult.getValidationResponse();
            assertTrue(resp.isValid(), () -> "response is not valid " + resp.getValidationMessages());
            assertTrue(!resp.hasError(), () -> "response has error " + resp.getValidationMessages());

            assertTrue(creationResult.isCreated(), () -> "was not created " + resp.getValidationMessages());

            T sub = creationResult.getCreatedEntity();

        }
        return creationResults;
    }
}
