package gsrs.module.substance.tasks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.config.FilePathParserUtils;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.standardizer.SubstanceSynchronizer;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.core.utils.executor.ProcessListener;
import ix.ginas.models.v1.Substance;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    SubstanceSynchronizer substanceSynchronizer;

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
                                    substanceSynchronizer.fixSubstanceReferences(fullSubstance, out::println, refUuidCodeSystem,
                                            refApprovalIdCodeSystem);
                                    listen.recordProcessed(s);
                                });
                            } catch (Throwable ex) {
                                log.error("error processing record {}", s.uuid, ex);
                                l.message("Error processing references for " + s.uuid + " error: " + ex.getMessage());
                                listen.error(ex);
                            }
                        });
                    });
                    out.println();
                    out.println("report completed at " + (new Date()));
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
