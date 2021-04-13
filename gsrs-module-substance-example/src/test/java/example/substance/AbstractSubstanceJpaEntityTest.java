package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.sneak.Sneak;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.EditRepository;
import gsrs.repository.GroupRepository;
import gsrs.service.GsrsEntityService;
import gsrs.startertests.*;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.Group;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@GsrsJpaTest(classes = { GsrsEntityTestConfiguration.class, GsrsControllerConfiguration.class})
//@SpringBootTest
@Import({AbstractSubstanceJpaEntityTest.TestConfig.class})
public abstract class AbstractSubstanceJpaEntityTest extends AbstractGsrsJpaEntityJunit5Test {
    @TestConfiguration
    public static class TestConfig{
        @Bean
        @Primary
        TestGsrsValidatorFactory gsrsValidatorFactory(){
            return new TestGsrsValidatorFactory();
        }

        @Bean
        SubstanceEntityService substanceEntityService(){
            return new SubstanceEntityService();
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
    }
    @Autowired
    protected TestEntityManager entityManager;

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
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    public void init(){
        admin = createUser("admin", Role.values());

        //some  integration tests will make validation messages which will get assigned an "admin" access group
        groupRepository.saveAndFlush(new Group("admin"));

    }

    protected Principal createUser(String username, Role... roles){
        Principal user = new Principal(username, null);

        UserProfile up = new UserProfile();
        up.setRoles(Arrays.asList(roles));
        up.user= user;
        entityManager.persistAndFlush(up);
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
