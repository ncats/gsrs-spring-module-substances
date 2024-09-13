package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.module.substance.repository.ChemicalSubstanceRepository;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.services.RecalcStructurePropertiesService;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import ix.core.utils.executor.ProcessListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class StructureInstrumeterTaskInitializer extends ScheduledTaskInitializer{
    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private RecalcStructurePropertiesService recalcStructurePropertiesService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private AdminService adminService;

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private ChemicalSubstanceRepository chemicalSubstanceRepository;

    @Override
    @Transactional
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l)
    {
        l.message("Initializing rehashing");
        ProcessListener listen = ProcessListener.onCountChange((sofar, total) ->
        {
            if (total != null)
            {
                l.message("Retrieved:" + sofar + " of " + total);
            } else
            {
                l.message("Retrieved:" + sofar);
            }
        });

        l.message("Initializing instrumentation: acquiring list");
        List<UUID> ids = chemicalSubstanceRepository.getAllIds();

        listen.newProcess();
        listen.totalRecordsToProcess(ids.size());

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(1, 1);
        l.message("Initializing instrumenting: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing instrumenting: starting process");

        try{
            for (UUID id : ids) {
                executor.submit(() -> {
                    try{
                        adminService.runAs(adminAuth, (Runnable)() -> {
                            TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                            try {
                                tx.executeWithoutResult(status -> {
                                    chemicalSubstanceRepository.findById(id).ifPresent(s -> {
                                        listen.preRecordProcess(s);
                                        try {
                                            log.debug("instrumenting "+  id);
                                            recalcStructurePropertiesService.recalcStructurePropertiesWithoutSaving(s.getStructure());
                                            log.debug("done instrumenting {}, for substance {}", id, s.getUuid());
                                            listen.recordProcessed(s);
                                        } catch(Throwable t) {
                                            log.error("error instrumenting "+  id, t);
                                            listen.error(t);
                                            l.message("Error instrumenting ... " + id + " error: " + t.getMessage());
                                        }
                                    });
                                });
                            } catch (Throwable ex) {
                                log.error("error instrumenting structural properties", ex);
                                l.message("Error instrumenting ... " + id + " error: " + ex.getMessage());
                            }
                        });
                    }catch(Exception ex) {
                        l.message("Error instrumenting ... " + id + " error: " + ex.getMessage());
                        return;
                    }
                });
            }
        }catch(Exception ee){
            log.error("error instrumenting ", ee);
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
    public String getDescription()
    {
        return "Retrieve and process (instrument) each structure in the database without saving to see messages from the underlying toolkit";
    }

}
