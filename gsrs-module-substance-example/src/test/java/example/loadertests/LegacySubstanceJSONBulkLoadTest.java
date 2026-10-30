package example.loadertests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.service.PayloadService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.models.Payload;
import ix.core.processing.PayloadProcessor;
import ix.core.stats.Statistics;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LegacySubstanceJSONBulkLoadTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private SubstanceBulkLoadService bulkLoadService;

    @Autowired
    private PayloadService payloadService;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    public LegacySubstanceJSONBulkLoadTest(){
        super(false);
    }
    @Transactional
    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    @Timeout(value = 10, unit = TimeUnit.MINUTES) //this will fail and kill the test if it takes more than 1 min
    public void loadGsrsFile() throws IOException, InterruptedException {

        AtomicLong startCount = new AtomicLong();
        TransactionTemplate tx4 = new TransactionTemplate(transactionManager);
        tx4.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx4.executeWithoutResult( ignored ->{
            startCount.set(substanceEntityService.count());
            System.out.println("Starting count:"+ startCount.get());
        });
        
        Resource dataFile = new ClassPathResource("testdumps/rep90.ginas");

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Payload payload = tx.execute(status-> {
//            createUser("admin2", Role.values());
            //the old json has user TYLER and FDA-SRS too
//            createUser("TYLER", Role.values());
//            createUser("FDA_SRS", Role.values());
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
        Statistics statistics = waitForCompletion(statKey, TimeUnit.MINUTES.toMillis(10));
        assertNotNull(statistics, "statistics should be present after waitForCompletion");
        //depending on the order the bulk load might fail if the substance currently requires a related substance to be present
        //(for example alt def?)
        assertNotNull(statistics.totalRecords, "totalRecords should be populated when job is complete");
        assertEquals(90L, statistics.totalRecords.getCount(), "rep90 fixture should report exact total record count");
        assertEquals(0, statistics.recordsExtractedFailed.get(), "extraction failures should be zero for rep90");
        assertEquals(0, statistics.recordsProcessedFailed.get(), "processing failures should be zero for rep90");
        assertEquals(0, statistics.recordsPersistedFailed.get(), "persistence failures should be zero for rep90");
        assertEquals(90, statistics.recordsPersistedSuccess.get(), "rep90 should persist all records");
        assertEquals(90, statistics.totalFailedAndPersisted(), "all rep90 records should be accounted for");

        long observedDelta = waitForPersistedDelta(startCount.get(), statistics.recordsPersistedSuccess.get(), TimeUnit.SECONDS.toMillis(20));
        assertEquals(statistics.recordsPersistedSuccess.get(), observedDelta,
                "persisted-success and count delta should match once transactions are flushed");


    }

    private Statistics waitForCompletion(String statKey, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Statistics statistics = bulkLoadService.getStatisticsFor(statKey);
            if (statistics != null && statistics._isDone()) {
                System.out.println(statistics);
                return statistics;
            }
            Thread.sleep(500);
        }
        fail("Timed out waiting for bulk load statistics for key: " + statKey);
        return null;
    }

    private long waitForPersistedDelta(long startCount, long expectedDelta, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        long lastDelta = Long.MIN_VALUE;
        while (System.currentTimeMillis() < deadline) {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            Long delta = tx.execute(status -> substanceEntityService.count() - startCount);
            if (delta != null) {
                lastDelta = delta;
                if (delta == expectedDelta) {
                    return delta;
                }
            }
            Thread.sleep(200);
        }
        fail("Timed out waiting for persisted delta " + expectedDelta + "; last observed=" + lastDelta);
        return lastDelta;
    }
}
