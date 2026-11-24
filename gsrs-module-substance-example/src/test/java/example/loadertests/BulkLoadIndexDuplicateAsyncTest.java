package example.loadertests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.repository.ProcessingJobRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.service.PayloadService;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.models.Payload;
import ix.core.models.ProcessingJob;
import ix.core.processing.PayloadProcessor;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.Substance;

/**
 * Bulk load with real asynchronous indexing (as in production, unlike the default test profile)
 * and check that every loaded substance has exactly one document in the text index.
 */
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@Import(BulkLoadIndexDuplicateAsyncTest.AsyncIndexing.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BulkLoadIndexDuplicateAsyncTest extends AbstractSubstanceJpaFullStackEntityTest {

    @TestConfiguration
    @EnableAsync
    static class AsyncIndexing {
    }

    @Autowired
    private SubstanceBulkLoadService bulkLoadService;
    @Autowired
    private PayloadService payloadService;
    @Autowired
    private ProcessingJobRepository processingJobRepository;
    @Autowired
    private SubstanceRepository substanceRepository;
    @Autowired
    private TextIndexerFactory textIndexerFactory;

    public BulkLoadIndexDuplicateAsyncTest() {
        super(false);
    }

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    public void everyBulkLoadedSubstanceHasExactlyOneIndexDocument() throws Exception {
        Resource dataFile = new ClassPathResource("testdumps/rep90.ginas");
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Payload payload = tx.execute(status -> {
            try (InputStream in = dataFile.getInputStream()) {
                return payloadService.createPayload(dataFile.getFilename(), "ignore", in,
                        PayloadService.PayloadPersistType.TEMP);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        PayloadProcessor pp = bulkLoadService.submit(SubstanceBulkLoadService.SubstanceBulkLoadParameters.builder()
                .payload(payload)
                .build());

        waitForFinalJobStatus(pp.jobId, TimeUnit.MINUTES.toMillis(10));
        // let any index work still queued after the job finished settle, so it is counted too
        Thread.sleep(5_000);

        List<UUID> ids = tx.execute(s -> {
            List<UUID> list = new ArrayList<>();
            substanceRepository.findAll().forEach(sub -> list.add(sub.getUuid()));
            return list;
        });
        assertEquals(90, ids.size(), "all rep90 substances should be in the database");

        TextIndexer indexer = textIndexerFactory.getDefaultInstance();
        Map<String, Integer> wrongCounts = new TreeMap<>();
        for (UUID id : ids) {
            int count = countIndexDocuments(indexer, id);
            if (count != 1) {
                wrongCounts.put(id.toString(), count);
            }
        }
        assertTrue(wrongCounts.isEmpty(),
                "substances with duplicate or missing index documents (uuid=count): " + wrongCounts);
    }

    private static int countIndexDocuments(TextIndexer indexer, UUID id) throws Exception {
        EntityUtils.Key key = EntityUtils.Key.of(Substance.class, id).toRootKey();
        BooleanQuery q = new BooleanQuery();
        q.add(new TermQuery(new Term(key.asLuceneIdTuple().k(), key.asLuceneIdTuple().v())), BooleanClause.Occur.MUST);
        q.add(new TermQuery(new Term(TextIndexer.FIELD_KIND, key.getKind())), BooleanClause.Occur.MUST);
        return indexer.withSearcher(searcher -> searcher.count(q));
    }

    private void waitForFinalJobStatus(Long jobId, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.setReadOnly(true);
            ProcessingJob.Status status = tx.execute(s ->
                    processingJobRepository.findById(jobId).map(j -> j.status).orElse(null));
            if (status == ProcessingJob.Status.COMPLETE || status == ProcessingJob.Status.STOPPED
                    || status == ProcessingJob.Status.FAILED) {
                // COMPLETE may be written by a progress save before the final reconciliation finishes,
                // so also wait until the loader thread is gone
                if (!loaderThreadRunning()) {
                    return;
                }
            }
            Thread.sleep(500);
        }
        fail("Timed out waiting for bulk load job " + jobId);
    }

    private static boolean loaderThreadRunning() {
        return Thread.getAllStackTraces().entrySet().stream().anyMatch(e -> {
            for (StackTraceElement el : e.getValue()) {
                if (el.getClassName().startsWith(SubstanceBulkLoadService.class.getName() + "$")
                        && "run".equals(el.getMethodName())) {
                    return true;
                }
            }
            return false;
        });
    }
}
