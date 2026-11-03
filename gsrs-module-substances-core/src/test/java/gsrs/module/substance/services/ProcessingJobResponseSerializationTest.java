package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.repository.KeywordRepository;
import gsrs.module.substance.repository.ProcessingJobRepository;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.models.Keyword;
import ix.core.models.ProcessingJob;
import ix.core.models.ProcessingJobUtils;
import ix.core.stats.Statistics;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessingJobResponseSerializationTest {

    @Test
    void uploadResponseShouldSerializeItsJobKeyAfterTheReadTransactionCloses() throws Exception {
        try (SessionFactory factory = newFactory()) {
            insertJob(factory);
            ProcessingJob response;
            try (EntityManager manager = factory.createEntityManager()) {
                manager.getTransaction().begin();
                JobRow row = manager.find(JobRow.class, 1L);
                assertFalse(Hibernate.isInitialized(row.keys));
                ProcessingJob job = responseFrom(row);
                ProcessingJobRepository repository = mock(ProcessingJobRepository.class);
                when(repository.findById(1L)).thenReturn(Optional.of(job));
                ProcessingJobEntityService service = new ProcessingJobEntityService();
                ReflectionTestUtils.setField(service, "processingJobRepository", repository);

                response = service.get(1L).orElseThrow();
                assertTrue(Hibernate.isInitialized(row.keys));
                manager.getTransaction().commit();
            }

            assertEquals("job-key", response.loaderLabel());
            assertResponseJson(response, "RUNNING", null);
        }
    }

    @Test
    void pollingShouldRefreshOldMetadataAndSerializeFinishedTimesAfterTransactionCloses() throws Exception {
        try (SessionFactory factory = newFactory()) {
            insertJob(factory);
            ProcessingJob response;
            try (EntityManager manager = factory.createEntityManager()) {
                JobRow stale = manager.find(JobRow.class, 1L);
                assertEquals("RUNNING", stale.status);
                try (EntityManager writer = factory.createEntityManager()) {
                    writer.getTransaction().begin();
                    JobRow current = writer.find(JobRow.class, 1L);
                    current.status = "COMPLETE";
                    current.stop = 3_000L;
                    writer.getTransaction().commit();
                }

                ProcessingJob job = responseFrom(stale);
                Keyword key = new Keyword(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY, "job-key");
                KeywordRepository keywords = mock(KeywordRepository.class);
                when(keywords.findAll(any(org.springframework.data.domain.Example.class)))
                        .thenReturn(List.of(key));
                ProcessingJobRepository repository = mock(ProcessingJobRepository.class);
                when(repository.findByKeysIn(List.of(key))).thenReturn(List.of(job));
                EntityManager serviceManager = mock(EntityManager.class);
                doAnswer(invocation -> {
                    manager.refresh(stale);
                    assertFalse(Hibernate.isInitialized(stale.keys));
                    job.status = ProcessingJob.Status.valueOf(stale.status);
                    job.stop = stale.stop;
                    job.keys = keysFrom(stale);
                    return null;
                }).when(serviceManager).refresh(job);
                ProcessingJobEntityService service = new ProcessingJobEntityService();
                ReflectionTestUtils.setField(service, "processingJobRepository", repository);
                ReflectionTestUtils.setField(service, "keywordRepository", keywords);
                ReflectionTestUtils.setField(service, "entityManager", serviceManager);

                manager.getTransaction().begin();
                response = service.getLoadStatus("job-key").orElseThrow();
                assertTrue(Hibernate.isInitialized(stale.keys));
                manager.getTransaction().commit();
            }

            assertEquals(2_000L, response._getDurationAsMs());
            assertResponseJson(response, "COMPLETE", 3_000L);
        }
    }

    private void assertResponseJson(ProcessingJob response, String status, Long stop) throws Exception {
        SubstanceBulkLoadService loadService = mock(SubstanceBulkLoadService.class);
        try (MockedStatic<StaticContextAccessor> context = mockStatic(StaticContextAccessor.class)) {
            context.when(() -> StaticContextAccessor.getBean(SubstanceBulkLoadService.class))
                    .thenReturn(loadService);
            // No statistics cache: verify the persisted snapshot also freezes after a restart.
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(mapper.writeValueAsString(response));
            assertEquals(status, json.get("status").asText());
            assertEquals("job-key", json.get("keys").get(0).get("term").asText());
            if (stop != null) {
                assertEquals(stop.longValue(), json.get("stop").asLong());
                assertEquals(stop.longValue(), json.get("statistics").get("stop").asLong());
                assertEquals(0L, json.get("statistics").get("estimatedTimeLeft").asLong());
                Statistics stats = response.getStatistics();
                assertEquals(1_000.0, stats.getAverageTimeToPersistMS(20_000L), 0.0001);
            }
        }
    }

    private SessionFactory newFactory() {
        return new Configuration()
                .addAnnotatedClass(JobRow.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID())
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
    }

    private void insertJob(SessionFactory factory) {
        try (EntityManager manager = factory.createEntityManager()) {
            manager.getTransaction().begin();
            JobRow row = new JobRow();
            row.id = 1L;
            row.status = "RUNNING";
            row.keys.add("job-key");
            manager.persist(row);
            manager.getTransaction().commit();
        }
    }

    private ProcessingJob responseFrom(JobRow row) {
        ProcessingJob job = new ProcessingJob();
        job.id = row.id;
        job.start = 1_000L;
        job.stop = row.stop;
        job.status = ProcessingJob.Status.valueOf(row.status);
        job.statistics = "{\"start\":1000,\"recordsPersistedSuccess\":2}";
        job.keys = keysFrom(row);
        return job;
    }

    private List<Keyword> keysFrom(JobRow row) {
        return new AbstractList<Keyword>() {
            @Override
            public Keyword get(int index) {
                return new Keyword(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY, row.keys.get(index));
            }

            @Override
            public int size() {
                return row.keys.size();
            }
        };
    }

    @Entity(name = "JobResponseRow")
    public static class JobRow {
        @Id
        public Long id;
        public String status;
        public Long stop;
        @ElementCollection
        public List<String> keys = new ArrayList<>();
    }
}
