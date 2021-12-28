package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.config.FilePathParserUtils;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import ix.core.utils.executor.ProcessListener;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class DEACodeScheduledTaskInitializer extends ScheduledTaskInitializer {

    private String outputPath;
    private String deaScheduleFilePath;
    private String deaListFilePath;

    private String name = "deaNumberReport";

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private SubstanceRepository substanceRepository;

    @Override
    public void run(SchedulerPlugin.TaskListener l) {
        l.message("Initializing rehashing");
        ProcessListener listen = ProcessListener.onCountChange((sofar, total) ->
        {
            if (total != null)
            {
                l.message("Rehashed:" + sofar + " of " + total);
            } else
            {
                l.message("Rehashed:" + sofar);
            }
        });

        l.message("Initializing rehashing: acquiring list");
        List<UUID> ids = substanceRepository.findAll().stream().map(s->s.getUuid()).collect(Collectors.toList());

        listen.newProcess();
        listen.totalRecordsToProcess(ids.size());

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        l.message("Initializing DEA ID recalculation: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing DEA ID recalculation: starting process");

        try{
            for (UUID id : ids) {
                executor.submit(() -> {
                    try{
                        adminService.runAs(adminAuth, () -> {
                            TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                            try {
                                tx.executeWithoutResult(status -> {
                                    substanceRepository.findById(id).ifPresent(s -> {
                                        listen.preRecordProcess(s);
                                        try {
                                            log.debug("examining "+  id);
                                            if(s.substanceClass.equals(Substance.SubstanceClass.chemical)) {
                                                log.debug("processing chemical");

                                            }
                                            else {
                                                log.debug("skipping non-chemical");
                                            }

                                            log.debug("done processing "+ id);
                                            listen.recordProcessed(s);
                                        } catch(Throwable t) {
                                            log.error("error processing "+  id, t);
                                            listen.error(t);
                                            l.message("Error reindexing ... " + id + " error: " + t.getMessage());
                                        }
                                    });
                                });
                            } catch (Throwable ex) {
                                log.error("error processing DEA status", ex);
                                l.message("Error processing DEA status for ... " + id
                                        + " error: " + ex.getMessage());
                            }
                        });
                    }catch(Exception ex) {
                        l.message("Error reindexing ... " + id + " error: " + ex.getMessage());
                        return;
                    }
                });
            }
        }catch(Exception ee){
            log.error("error processing ", ee);
            l.message("ERROR:" + ee.getMessage());
            throw new RuntimeException(ee);
        }

        l.message("Shutting down executor service");
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS );
        } catch (InterruptedException e) {
            //should never happen

        }
        l.message("Task finished");
        listen.doneProcess();

    }

    @Override
    public String getDescription() {
        return "Assign DEA Schedules as notes to each record and codes with the DEA Number";
    }

    /**
     * Returns the File used to output the report
     *
     * @return
     */
    public File getOutputFile() {
        return FilePathParserUtils.getFileParserBuilder()
                .suppliedFilePath(outputPath)
                .defaultFilePath("reports/" + name + "-%DATE%.txt")
                .build()
                .getFile();
    }

    private PrintStream makePrintStream(File writeFile) throws IOException {
        return new PrintStream(
                new BufferedOutputStream(new FileOutputStream(writeFile)),
                false, "UTF-8");
    }

}
