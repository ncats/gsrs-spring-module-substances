package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.config.FilePathParserUtils;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.services.RecalcStructurePropertiesService;
import gsrs.module.substance.utils.DEADataTable;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import ix.core.utils.executor.ProcessListener;
import ix.ginas.models.GinasCommonData;
import ix.ginas.models.v1.ChemicalSubstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class DEAScheduledTaskInitializer extends ScheduledTaskInitializer {

    @Autowired
    private SubstanceRepository substanceRepository;

//    @Autowired
//    private StructureRepository structureRepository;

    @Autowired
    private RecalcStructurePropertiesService recalcStructurePropertiesService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private AdminService adminService;

    private String name = "deaNumberReport";
    private String deaNumberFileName;
    private String outputFilePath;

    public String getDeaNumberFileName() {
        return deaNumberFileName;
    }

    public void setDeaNumberFileName(String deaNumberFileName) {
        this.deaNumberFileName = deaNumberFileName;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    @Override
    @Transactional
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        l.message("Initializing rehashing");
        ProcessListener listen = ProcessListener.onCountChange((sofar, total) ->
        {
            if (total != null) {
                l.message("Rehashed:" + sofar + " of " + total);
            } else {
                l.message("Rehashed:" + sofar);
            }
        });

        l.message("Initializing rehashing: acquiring list");
        DEADataTable deaDataTable = new DEADataTable(deaNumberFileName);

        List<UUID> ids = substanceRepository.findAll().stream().map(GinasCommonData::getUuid).collect(Collectors.toList());
        //structureRepository.getAllIds();

        listen.newProcess();
        listen.totalRecordsToProcess(ids.size());

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(1, 10);
        l.message("Initializing calculation: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing calculation: starting process");

        File reportFile = getOutputFile();
        try (PrintStream out = makePrintStream(reportFile)){
            for (UUID id : ids) {
                executor.submit(() -> {
                    try {
                        adminService.runAs(adminAuth, (Runnable)() -> {
                            TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                            try {
                                tx.executeWithoutResult(status -> {
                                    substanceRepository.findById(id).ifPresent(s -> {
                                        listen.preRecordProcess(s);
                                        try {
                                            if ((s instanceof ChemicalSubstance)) {
                                                log.debug("computing DEA data for  " + id);
                                                //recalcStructurePropertiesService.recalcStructureProperties(s);
                                                if (deaDataTable.assignIds(((ChemicalSubstance) s), out)) {
                                                    substanceRepository.saveAndFlush(s);
                                                    log.debug("saved");
                                                }
                                                log.debug("done computing DEA data for  " + id);
                                                listen.recordProcessed(s);
                                            }
                                        } catch (Throwable t) {
                                            log.error("error computing DEA data for  " + id, t);
                                            listen.error(t);
                                            l.message("Error computing DEA data for ... " + id + " error: " + t.getMessage());
                                        }
                                    });
                                });
                            } catch (Throwable ex) {
                                log.error("error computing DEA data for record", ex);
                                l.message("Error computing DEA data for ... " + id + " error: " + ex.getMessage());
                            }
                        });
                        //out.flush();
                    } catch (Throwable ex) {
                        l.message("Error computing DEA data for ... " + id + " error: " + ex.getMessage());
                    }
                });
            }
        } catch (Exception ee) {
            log.error("error computing DEA data for  ", ee);
            l.message("ERROR:" + ee.getMessage());
            throw new RuntimeException(ee);
        }

        l.message("Shutting down executor service");
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            //should never happen

        }
        l.message("Task finished");
        listen.doneProcess();


    }

    @Override
    public String getDescription() {
        return "Compute DEA code and schedule for all chemicals in the database";
    }

    /**
     * Returns the File used to output the report
     *
     * @return a File object that can be used for a report
     */
    public File getOutputFile() {
        return FilePathParserUtils.getFileParserBuilder()
                .suppliedFilePath(outputFilePath)
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
