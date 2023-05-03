package gsrs.module.substance.tasks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.config.FilePathParserUtils;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import gsrs.service.GsrsEntityService;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.core.utils.executor.ProcessListener;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.v1.*;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Data
public class SubstanceRefTaskInitializer extends ScheduledTaskInitializer {

    private String reportFilePath;

    private String refUuidCodeSystem;

    private String refApprovalIdCodeSystem;

    @Autowired
    SubstanceRepository substanceRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @Autowired
    private AdminService adminService;

    @JsonIgnore
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    private final String name = "substance_reference_report";

    private Map<UUID, CountDownLatch> latchMap = new ConcurrentHashMap<>();
    private Map<UUID, TaskProgress> listenerMap = new ConcurrentHashMap<>();

    @Data
    @Builder
    private static class TaskProgress {
        private SchedulerPlugin.TaskListener listener;
        private UUID id;
        private long totalCount;
        private long currentCount;

        public synchronized void increment() {
            listener.message("Indexed: " + (++currentCount) + " of " + totalCount);
        }
    }


    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        log.trace("starting in run");
        l.message("Starting resolution");
        ProcessListener listen = ProcessListener.onCountChange((sofar, total) ->
        {
            if (total != null) {
                l.message("Processed:" + sofar + " of " + total);
            } else {
                l.message("Processed:" + sofar);
            }
        });
        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        try {
            listen.newProcess();
            listen.totalRecordsToProcess((int) substanceRepository.count());

            Authentication adminAuth = adminService.getAnyAdmin();
            File writeFile = getOutputFile();
            TransactionTemplate txRead = new TransactionTemplate(transactionManager);
            txRead.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            txRead.executeWithoutResult(t -> {
                try (PrintStream out = makePrintStream(writeFile)) {

                    substanceRepository.streamAll().parallel().forEach(s -> {
                        log.trace("starting substance {}", s.getUuid().toString());
                        executor.submit(() -> {
                            try {
                                Substance fullSubstance = (Substance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, s.getUuid())).call();
                                listen.preRecordProcess(s);
                                adminService.runAs(adminAuth, (Runnable) () -> {
                                    fixSubstanceReference(fullSubstance, out::println, out::println);
                                    listen.recordProcessed(s);
                                });
                            } catch (Throwable ex) {
                                log.error("error processing", ex);
                                l.message("Error processing references for " + s.uuid + " error: " + ex.getMessage());
                                listen.error(ex);
                            }
                        });
                    });
                } catch (Exception ex) {
                    l.message("Error processing substance references " + ex.getMessage());
                }
            });

            l.message("Shutting down executor service");
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                //should never happen
            }
            l.message("Task finished");
            listen.doneProcess();
        } catch (Exception ee) {
            log.error("error resolving references ", ee);
            l.message("ERROR:" + ee.getMessage());
            throw new RuntimeException(ee);
        }
    }

    @Override
    public String getDescription() {
        return "Fix references from one substance to another when possible";
    }


    /*
    Search order:
    1) UUID. When the UUID matches, the reference will already show up as existing so no need to do searches
    2) UUID as xcode
    3) Approval ID
    4) Approval ID as code
    5) name as primary name
    6) name, any
     */
    public void fixSubstanceReference(Substance startingSubstance, Consumer<String> missingMessages, Consumer<String> multipleMatchMessages) {
        AtomicBoolean substanceNeedsToSave = new AtomicBoolean(false);
        startingSubstance.relationships.forEach(r -> {
            SubstanceReferenceState state = resolveSubstanceReference(r.relatedSubstance, multipleMatchMessages);
            if (state == SubstanceReferenceState.JUST_RESOLVED) {
                substanceNeedsToSave.set(true);
            } else if (state == SubstanceReferenceState.UNRESOLVABLE) {
                String message = String.format("Referenced substance %s for relationship of type %s was not found on substance %s!",
                        r.relatedSubstance.refuuid, r.type, startingSubstance.uuid.toString());
                missingMessages.accept(message);
            }
            if (r.mediatorSubstance != null) {
                state = resolveSubstanceReference(r.mediatorSubstance, multipleMatchMessages);
                if (state == SubstanceReferenceState.JUST_RESOLVED) {
                    substanceNeedsToSave.set(true);
                } else if (state == SubstanceReferenceState.UNRESOLVABLE) {
                    String message = String.format("Mediator substance %s for relationship of type %s was not found on substance %s!",
                            r.relatedSubstance.refuuid, r.type, startingSubstance.uuid.toString());
                    missingMessages.accept(message);
                }
            }
        });
        List<Tuple<GinasAccessControlled, SubstanceReference>> refs = startingSubstance.getDependsOnSubstanceReferencesAndParents();
        refs.forEach(r -> {
            SubstanceReferenceState state = resolveSubstanceReference(r.v(), multipleMatchMessages);
            if (state == SubstanceReferenceState.JUST_RESOLVED) {
                substanceNeedsToSave.set(true);
            }
            if (state == SubstanceReferenceState.UNRESOLVABLE) {
                String message = String.format("Referenced substance %s for %s of %s was not found!", r.v().refuuid, r.k().getClass().getName(),
                        startingSubstance.uuid.toString());
                missingMessages.accept(message);
            }
        });
        if (substanceNeedsToSave.get()) {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(s -> {
                try {
                    log.trace("going to save substance {}", startingSubstance.getUuid().toString());
                    GsrsEntityService.UpdateResult<Substance> updateResult = substanceEntityService.updateEntity(startingSubstance.toFullJsonNode());
                    if (updateResult.getStatus() == GsrsEntityService.UpdateResult.STATUS.ERROR) {
                        log.error("Error updating substance: {}", updateResult.getValidationResponse().toString());
                        if (updateResult.getThrowable() != null) {
                            log.error(updateResult.getThrowable().getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error saving substance", e);
                }
            });
        }
    }

    public SubstanceReferenceState resolveSubstanceReference(SubstanceReference substanceReference,
                                                             Consumer<String> multipleMatchMessages) {
        SubstanceReferenceState substanceReferenceState = SubstanceReferenceState.UNRESOLVABLE;
        if (substanceReference == null) {
            log.info("resolveSubstanceReference called with null parameter");
            return substanceReferenceState;
        }
        if (substanceRepository.exists(substanceReference)) {
            return SubstanceReferenceState.ALREADY_RESOLVED;
        }
        boolean currentReferenceResolved = false;
        List<Substance> uuidMatches = refUuidCodeSystem != null && refUuidCodeSystem.length() > 0 ? ValidationUtils.findSubstancesByCode(refUuidCodeSystem, substanceReference.refuuid,
                transactionManager, searchService) : Collections.emptyList();

        if (uuidMatches.size() > 1) {
            String message = String.format("More than one record found with UUID code %s and code system %s. Using first matching record",
                    substanceReference.refuuid, refUuidCodeSystem);
            log.warn(message);
            multipleMatchMessages.accept(message);
            substanceReference.refuuid = uuidMatches.get(0).uuid.toString();
            currentReferenceResolved = true;
            substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
        } else if (uuidMatches.size() == 1) {
            substanceReference.refuuid = uuidMatches.get(0).uuid.toString();
            currentReferenceResolved = true;
            substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
        }
        if (!currentReferenceResolved && substanceReference.approvalID != null && substanceReference.approvalID.length() > 0) {
            Substance approvalIdMatch = substanceRepository.findByApprovalID(substanceReference.approvalID);
            if (approvalIdMatch != null) {
                substanceReference.refuuid = approvalIdMatch.uuid.toString();
                currentReferenceResolved = true;
                substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
            } else {
                List<Substance> approvalIdMatches = ValidationUtils.findSubstancesByCode(this.refApprovalIdCodeSystem,
                        substanceReference.approvalID, transactionManager, searchService);
                if (approvalIdMatches != null && !approvalIdMatches.isEmpty()) {
                    if (approvalIdMatches.size() > 1) {
                        String message = String.format("More than one record found with Approval ID code %s and code system %s. Using first matching record",
                                substanceReference.refuuid, refApprovalIdCodeSystem);
                        log.warn(message);
                        multipleMatchMessages.accept(message);
                    }
                    substanceReference.refuuid = approvalIdMatches.get(0).uuid.toString();
                    currentReferenceResolved = true;
                    substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
                }
            }
        }
        if (!currentReferenceResolved && substanceReference.refPname != null && substanceReference.refPname.length() > 0) {
            //use the name
            List<Substance> nameMatches = ValidationUtils.findSubstancesByName(substanceReference.refPname, transactionManager,
                    searchService);
            if (nameMatches != null && !nameMatches.isEmpty()) {
                if (nameMatches.size() > 1) {
                    String message = String.format("More than one record found with Name %s. Using first matching record",
                            substanceReference.refPname);
                    log.warn(message);
                    multipleMatchMessages.accept(message);
                }
                substanceReference.refuuid = nameMatches.get(0).uuid.toString();
                substanceReferenceState = SubstanceReferenceState.JUST_RESOLVED;
            }
        }

        return substanceReferenceState;
    }

    public File getOutputFile() {
        return FilePathParserUtils.getFileParserBuilder()
                .suppliedFilePath(reportFilePath)
                .defaultFilePath("reports/" + name + "-%DATE%.txt")
                .dateFormatter(formatter)
                .build()
                .getFile();
    }

    private PrintStream makePrintStream(File writeFile) throws IOException {
        return new PrintStream(
                new BufferedOutputStream(Files.newOutputStream(writeFile.toPath())),
                false, "UTF-8");
    }
}
