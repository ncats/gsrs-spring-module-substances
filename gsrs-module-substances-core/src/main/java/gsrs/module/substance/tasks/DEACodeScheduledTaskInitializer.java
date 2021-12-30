package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.config.FilePathParserUtils;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.utils.DEADataTable;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import ix.core.utils.executor.ProcessListener;
import ix.ginas.models.GinasCommonData;
import ix.ginas.models.v1.ChemicalSubstance;
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

    private String outputFilePath;

    private String name = "deaNumberReport";

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private SubstanceRepository substanceRepository;

    private String deaNumberFileName;

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
    public void run(SchedulerPlugin.TaskListener l) {
        l.message("Initializing DEA ID Calculation");
        ProcessListener listen = ProcessListener.onCountChange((sofar, total) ->
        {
            if (total != null)
            {
                l.message("Processed: " + sofar + " of " + total);
            } else
            {
                l.message("Processed: " + sofar);
            }
        });

        DEADataTable deaDataTable = new DEADataTable(deaNumberFileName);

        l.message("Initializing DEA ID Calculation: acquiring list");
        List<UUID> ids = substanceRepository.findAll().stream().map(GinasCommonData::getUuid).collect(Collectors.toList());

        listen.newProcess();
        listen.totalRecordsToProcess(ids.size());

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        l.message("Initializing DEA ID recalculation: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing DEA ID recalculation: starting process");
        File reportFile=getOutputFile();
        try (PrintStream out = makePrintStream(reportFile)){
            for (UUID id : ids) {
                executor.submit(() -> {
                    try {
                        adminService.runAs(adminAuth, () -> {
                            TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                            try {
                                tx.executeWithoutResult(status -> {
                                    substanceRepository.findById(id).ifPresent(s -> {
                                        listen.preRecordProcess(s);
                                        try {
                                            if(s.substanceClass.equals(Substance.SubstanceClass.chemical)) {
                                                boolean addedCode=false;
                                                boolean addedNote=false;
                                                log.debug("processing chemical with id " + id);
                                                String deaNumber =deaDataTable.getDeaNumberForChemical((ChemicalSubstance) s);
                                                log.trace("deaNumber: " + deaNumber);
                                                if( deaNumber!=null) {
                                                    addedCode=deaDataTable.assignCodeForDea(s, deaNumber);
                                                    out.format("assigned DEA number %s to substance %s\r\n", deaNumber, s.uuid);
                                                }
                                                String deaSchedule = deaDataTable.getDeaScheduleForChemical((ChemicalSubstance) s);
                                                log.trace("deaSchedule: " + deaSchedule);
                                                if( deaSchedule !=null ) {
                                                    addedNote=deaDataTable.assignNoteForDea(s, deaSchedule);
                                                    out.format("assigned DEA schedule %s to substance %s\n", deaSchedule, s.uuid);
                                                }
                                                if(addedCode || addedNote) {
                                                    log.trace("will save");
                                                    substanceRepository.saveAndFlush(s);
                                                } else {
                                                    log.trace("omitting save");
                                                }
                                            }

                                            out.println();
                                            log.debug("done processing "+ id);
                                            listen.recordProcessed(s);
                                        } catch(Throwable t) {
                                            log.error("error processing "+  id, t);
                                            listen.error(t);
                                            l.message("Error processing ... " + id + " error: " + t.getMessage());
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
                        l.message("Error processing ... " + id + " error: " + ex.getMessage());
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
        return "Assign DEA Schedules as notes and DEA Numbers as codes to each relevant record";
    }

    /**
     * Returns the File used to output the report
     *
     * @return
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
