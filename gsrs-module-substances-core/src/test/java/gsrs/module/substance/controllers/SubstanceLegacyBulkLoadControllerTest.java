package gsrs.module.substance.controllers;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.module.substance.services.ProcessingJobEntityService;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.payload.PayloadController;
import gsrs.repository.PayloadRepository;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import ix.core.models.ProcessingJob;
import ix.core.processing.PayloadProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubstanceLegacyBulkLoadControllerTest {

    @Mock
    private PayloadService payloadService;

    @Mock
    private PayloadRepository payloadRepository;

    @Mock
    private SubstanceBulkLoadService substanceBulkLoadService;

    @Mock
    private GsrsControllerConfiguration controllerConfiguration;

    @Mock
    private ProcessingJobEntityService processingJobService;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    private SubstanceLegacyBulkLoadController controller;

    @BeforeEach
    void setUp() {
        controller = new SubstanceLegacyBulkLoadController();
        ReflectionTestUtils.setField(controller, "payloadService", payloadService);
        ReflectionTestUtils.setField(controller, "payloadRepository", payloadRepository);
        ReflectionTestUtils.setField(controller, "substanceBulkLoadService", substanceBulkLoadService);
        ReflectionTestUtils.setField(controller, "controllerConfiguration", controllerConfiguration);
        ReflectionTestUtils.setField(controller, "processingJobService", processingJobService);
        ReflectionTestUtils.setField(controller, "platformTransactionManager", platformTransactionManager);
    }

    @Test
    void handleFileUploadShouldRejectInvalidTypeBeforeCreatingPayload() throws IOException {
        Map<String, String> queryParameters = Collections.emptyMap();
        MockMultipartFile file = new MockMultipartFile("file-name", "legacy.ginas", "application/octet-stream", "data".getBytes());

        ResponseEntity<Object> expected = ResponseEntity.badRequest().body("bad-request");
        when(controllerConfiguration.handleBadRequest(eq("invalid file type:SDF"), eq(queryParameters))).thenReturn(expected);

        Object result = controller.handleFileUpload(file, "SDF", queryParameters);

        assertSame(expected, result);
        verify(payloadService, never()).createPayload(anyString(), anyString(), any(byte[].class), any());
        verify(substanceBulkLoadService, never()).submit(any());
    }

    @Test
    void handleFileUploadShouldRejectDuplicateRequestWhilePriorJobIsRunning() throws IOException {
        stubTransactions();
        Map<String, String> queryParameters = Collections.singletonMap("preserve-audit", "true");
        MockMultipartFile file = new MockMultipartFile("file-name", "legacy.ginas", "application/octet-stream", "same-content".getBytes());

        Payload payload = new Payload();
        payload.id = UUID.randomUUID();

        PayloadProcessor payloadProcessor = new PayloadProcessor(payload);
        payloadProcessor.jobId = 101L;

        ProcessingJob runningJob = new ProcessingJob();
        runningJob.id = payloadProcessor.jobId;
        runningJob.status = ProcessingJob.Status.RUNNING;

        when(payloadService.createPayload(eq("legacy.ginas"), eq(PayloadController.predictMimeTypeFromFile(file)),
                any(byte[].class), eq(PayloadService.PayloadPersistType.PERM))).thenReturn(payload);
        when(payloadRepository.findById(payload.id)).thenReturn(Optional.of(payload));
        when(substanceBulkLoadService.submit(any())).thenReturn(payloadProcessor);
        when(processingJobService.get(payloadProcessor.jobId)).thenReturn(Optional.of(runningJob));
        ResponseEntity<Object> duplicateResponse = ResponseEntity.badRequest().body("duplicate-request");
        when(controllerConfiguration.handleBadRequest(eq("duplicate bulk import request is already in progress"), eq(queryParameters)))
                .thenReturn(duplicateResponse);

        Object firstResult = controller.handleFileUpload(file, "JSON", queryParameters);
        Object secondResult = controller.handleFileUpload(file, "JSON", queryParameters);

        assertSame(runningJob, firstResult);
        assertEquals(duplicateResponse, secondResult);
        verify(substanceBulkLoadService, times(1)).submit(any());
    }

    private void stubTransactions() {
        when(platformTransactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }
}
