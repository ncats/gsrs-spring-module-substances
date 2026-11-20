package gsrs.module.substance.services;

import gsrs.module.substance.repository.KeywordRepository;
import gsrs.module.substance.repository.ProcessingJobRepository;
import ix.core.models.Keyword;
import ix.core.models.ProcessingJob;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessingJobEntityServiceTest {
    private final ProcessingJobRepository jobs = mock(ProcessingJobRepository.class);
    private final KeywordRepository keywords = mock(KeywordRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final ProcessingJobEntityService service = new ProcessingJobEntityService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "processingJobRepository", jobs);
        ReflectionTestUtils.setField(service, "keywordRepository", keywords);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    @Test
    void lookupByLabelShouldRefreshStaleJobMetadata() {
        ProcessingJob stale = staleJob();
        Keyword key = new Keyword("ix.core.plugins.GinasRecordProcessorPlugin", "job-key");
        when(keywords.findAll(any(org.springframework.data.domain.Example.class)))
                .thenReturn(Collections.singletonList(key));
        when(jobs.findByKeysIn(Collections.singletonList(key))).thenReturn(Collections.singletonList(stale));

        ProcessingJob result = service.getLoadStatus("job-key").orElseThrow();

        assertCompleted(result);
        verify(entityManager).refresh(stale);
    }

    @Test
    void monitorNumericIdShouldRefreshTheSameJobAsTheUploadResponse() {
        ProcessingJob stale = staleJob();
        when(keywords.findAll(any(org.springframework.data.domain.Example.class)))
                .thenReturn(Collections.emptyList());
        when(jobs.findById(1L)).thenReturn(Optional.of(stale));

        assertCompleted(service.getLoadStatus("1").orElseThrow());
        verify(entityManager).refresh(stale);
    }

    @Test
    void uploadLookupShouldNotRefreshOrSuspendItsCallerTransaction() throws Exception {
        ProcessingJob job = new ProcessingJob();
        when(jobs.findById(1L)).thenReturn(Optional.of(job));

        assertSame(job, service.get(1L).orElseThrow());
        verifyNoInteractions(entityManager);
        Transactional transaction =
                ProcessingJobEntityService.class.getMethod("get", Long.class)
                        .getAnnotation(Transactional.class);
        assertEquals(Propagation.REQUIRED, transaction.propagation());
    }

    @Test
    void missingJobsShouldNotBeRefreshed() {
        when(jobs.findById(1L)).thenReturn(Optional.empty());
        when(keywords.findAll(any(org.springframework.data.domain.Example.class)))
                .thenReturn(Collections.emptyList());

        assertTrue(service.get(1L).isEmpty());
        assertTrue(service.getLoadStatus("missing").isEmpty());
        verifyNoInteractions(entityManager);
    }

    private ProcessingJob staleJob() {
        ProcessingJob job = new ProcessingJob();
        job.status = ProcessingJob.Status.RUNNING;
        job.start = 1_000L;
        job.message = "Indexing loaded substances";
        doAnswer(invocation -> {
            job.status = ProcessingJob.Status.COMPLETE;
            job.stop = 3_000L;
            job.message = "Loading and indexing complete";
            return null;
        }).when(entityManager).refresh(job);
        return job;
    }

    private void assertCompleted(ProcessingJob job) {
        assertEquals(ProcessingJob.Status.COMPLETE, job.status);
        assertEquals(3_000L, job.stop);
        assertEquals(2_000L, job._getDurationAsMs());
        assertEquals("Loading and indexing complete", job.message);
    }
}
