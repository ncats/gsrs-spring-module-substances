package example.loadertests;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.service.PayloadService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.GsrsValidatorFactory;
import ix.core.models.Payload;
import ix.core.models.Role;
import ix.core.processing.PayloadProcessor;
import ix.core.stats.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LegacySubstanceJSONBulkLoadTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private SubstanceBulkLoadService bulkLoadService;

    @Autowired
    private PayloadService payloadService;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @Autowired
    private GsrsValidatorFactory validatorFactory;

    public LegacySubstanceJSONBulkLoadTest(){
        super(false);
    }
    @Transactional
    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    @Timeout(value = 10, unit = TimeUnit.MINUTES) //this will fail and kill the test if it takes more than 1 min
    public void loadGsrsFile() throws IOException, InterruptedException {

        Resource dataFile = new ClassPathResource("testdumps/rep90.ginas");

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Payload payload = tx.execute(status-> {
            createUser("admin", Role.values());
            //the old json has user TYLER and FDA-SRS too
            createUser("TYLER", Role.values());
            createUser("FDA_SRS", Role.values());
                    try (InputStream in = dataFile.getInputStream()) {
                        return payloadService.createPayload(dataFile.getFilename(), "ignore",
                                in, PayloadService.PayloadPersistType.TEMP);
                    }catch(IOException e){
                        throw new UncheckedIOException(e);
                    }
                });
        PayloadProcessor pp = bulkLoadService.submit(SubstanceBulkLoadService.SubstanceBulkLoadParameters.builder()
                .payload(payload)

                .build());

        String statKey = pp.key;
        boolean done =false;
        Statistics statistics=null;
        while(!done){
            statistics = bulkLoadService.getStatisticsFor(statKey);

            if(statistics._isDone()){
                System.out.println(statistics);
                break;
            }
            Thread.sleep(1000);
        }
        //depending on the order the bulk load might fail if the substance currently requires a related substance to be present
        //(for example alt def?)
        assertEquals(statistics.totalFailedAndPersisted(), 90);
        Statistics effectivelyFinalStatistics = statistics;
        TransactionTemplate tx3 = new TransactionTemplate(transactionManager);
        tx3.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx3.executeWithoutResult( ignored ->{
            assertEquals(effectivelyFinalStatistics.recordsPersistedSuccess.get(), substanceEntityService.count());
        });


    }
}
