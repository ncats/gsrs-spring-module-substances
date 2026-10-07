package gsrs.module.substance.controllers;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.module.substance.services.ProcessingJobEntityService;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.module.substance.services.BulkUploadPreflight;
import gsrs.payload.PayloadController;
import gsrs.repository.PayloadRepository;
import gsrs.security.canImportData;
import gsrs.security.canRunTasks;
import gsrs.security.hasAdminRole;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import ix.core.models.ProcessingJob;
import ix.core.processing.PayloadProcessor;
//import jdk.internal.net.http.common.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@Slf4j
public class SubstanceLegacyBulkLoadController {
    private static final long UPLOAD_RESERVED_JOB_ID = -1L;
    private final Map<String, Long> uploadHashToJobId = new ConcurrentHashMap<>();
    private final Object duplicateUploadLock = new Object();

    @Autowired
    private PayloadService payloadService;

    @Autowired
    private PayloadRepository payloadRepository;

    @Autowired
    private SubstanceBulkLoadService substanceBulkLoadService;

    @Autowired
    private GsrsControllerConfiguration controllerConfiguration;

    @Autowired
    private ProcessingJobEntityService processingJobService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private BulkUploadPreflight bulkUploadPreflight;


    //@hasAdminRole
    @canRunTasks
    @GetMapping("/api/v1/admin/{id}")
    public Object getLoadStatus(@PathVariable("id") String id, @RequestParam Map<String, String> queryParameters){
        Optional<ProcessingJob> jobs = processingJobService.flexLookup(id);
        if(!jobs.isPresent()){
            return controllerConfiguration.handleNotFound(queryParameters);
        }
        return jobs.get();
    }

    ///admin/load
    //@hasAdminRole
    @canImportData
    @PostMapping("/api/v1/admin/load")
    public Object handleFileUpload(@RequestParam("file-name") MultipartFile file,
                                                   @RequestParam("file-type") String type,
                                                   @RequestParam Map<String, String> queryParameters) throws IOException {
        if (!"JSON".equals(type)) {
            return controllerConfiguration.handleBadRequest("invalid file type:" + type, queryParameters);
        }
        if (file.isEmpty()) {
            return controllerConfiguration.handleBadRequest("uploaded file is empty", queryParameters);
        }
        Optional<String> rejection = bulkUploadPreflight.rejectionFor(file.getSize());
        if (rejection.isPresent()) {
            log.warn("Bulk import upload rejected: {}", rejection.get());
            return ResponseEntity.status(413).body(Map.of("message", rejection.get()));
        }

        final byte[] fileBytes = file.getBytes();
        final String uploadHash = sha256(fileBytes);
        synchronized (duplicateUploadLock) {
            Long existingJobId = uploadHashToJobId.get(uploadHash);
            if (isDuplicateUploadInProgress(existingJobId)) {
                return controllerConfiguration.handleBadRequest("duplicate bulk import request is already in progress", queryParameters);
            }
            uploadHashToJobId.put(uploadHash, UPLOAD_RESERVED_JOB_ID);
        }

        boolean jobCreated = false;
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            UUID payloadId = transactionTemplate.execute(status -> {
                try {
                    return payloadService.createPayload(file.getOriginalFilename(), PayloadController.predictMimeTypeFromFile(file),
                            fileBytes, PayloadService.PayloadPersistType.PERM).id;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            Payload payload = payloadRepository.findById(payloadId)
                    .orElseThrow(() -> new NoSuchElementException("payload not found after upload: " + payloadId));
            log.trace("fetched payload {}", payload.id);
            boolean preserveAuditInfo = Boolean.parseBoolean(queryParameters.getOrDefault("preserve-audit", "false"));

            PayloadProcessor processor = substanceBulkLoadService.submit(
                    SubstanceBulkLoadService.SubstanceBulkLoadParameters.builder()
                            .payload(payload)
                            .preserveOldEditInfo(preserveAuditInfo)
                            .build());
            synchronized (duplicateUploadLock) {
                uploadHashToJobId.put(uploadHash, processor.jobId);
            }
            jobCreated = true;

            log.trace("bulk service has been submitted");
            transactionTemplate = new TransactionTemplate(platformTransactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return transactionTemplate.execute(s -> processingJobService.get(processor.jobId)
                    .orElseThrow(() -> new NoSuchElementException("processing job not found: " + processor.jobId)));
        } finally {
            if (!jobCreated) {
                synchronized (duplicateUploadLock) {
                    Long currentJobId = uploadHashToJobId.get(uploadHash);
                    if (currentJobId != null && currentJobId == UPLOAD_RESERVED_JOB_ID) {
                        uploadHashToJobId.remove(uploadHash);
                    }
                }
            }
        }
    }

    private boolean isDuplicateUploadInProgress(Long existingJobId) {
        if (existingJobId == null) {
            return false;
        }
        if (existingJobId == UPLOAD_RESERVED_JOB_ID) {
            return true;
        }
        Optional<ProcessingJob> existingJob = processingJobService.get(existingJobId);
        if (!existingJob.isPresent()) {
            return false;
        }
        ProcessingJob.Status status = existingJob.get().status;
        return status == null
                || (status != ProcessingJob.Status.COMPLETE
                && status != ProcessingJob.Status.FAILED
                && status != ProcessingJob.Status.STOPPED);
    }

    private String sha256(byte[] contents) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(contents);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(Character.forDigit((value >>> 4) & 0xF, 16));
                result.append(Character.forDigit(value & 0xF, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available in the Java runtime", e);
        }
    }
}
