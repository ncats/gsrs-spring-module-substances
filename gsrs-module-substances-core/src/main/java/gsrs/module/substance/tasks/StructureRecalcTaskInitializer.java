/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.services.RecalcStructurePropertiesService;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import ix.core.utils.executor.ProcessListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Mitch Miller
 */
@Slf4j
public class StructureRecalcTaskInitializer extends ScheduledTaskInitializer{
    @Autowired
    private StructureRepository structureRepository;
    @Autowired
    private RecalcStructurePropertiesService recalcStructurePropertiesService;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private AdminService adminService;

    @Override
    @Transactional
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l)
    {
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
        List<UUID> ids = structureRepository.getAllIds();

        listen.newProcess();
        listen.totalRecordsToProcess(ids.size());

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        l.message("Initializing rehashing: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing rehashing: starting process");

        try{
            for (UUID id : ids) {
                executor.submit(() -> {
                    try{
                        adminService.runAs(adminAuth, () -> {
                            TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                            try {
                                tx.executeWithoutResult(status -> {
                                    structureRepository.findById(id).ifPresent(s -> {
                                        listen.preRecordProcess(s);
                                        try {
                                            log.debug("recalcing "+  id);
                                            recalcStructurePropertiesService.recalcStructureProperties(s);
                                            log.debug("done recalcing "+ id);
                                            listen.recordProcessed(s);
                                        } catch(Throwable t) {
                                            log.error("error recalcing "+  id, t);
                                            listen.error(t);
                                            l.message("Error reindexing ... " + id + " error: " + t.getMessage());
                                        }
                                    });
                                });
                            } catch (Throwable ex) {
                                log.error("error recalcing structural properties", ex);
                                l.message("Error reindexing ... " + id + " error: " + ex.getMessage());
                            }
                        });
                    }catch(Exception ex) {
                        l.message("Error reindexing ... " + id + " error: " + ex.getMessage());
                        return;
                    }
                });
            }
        }catch(Exception ee){
            log.error("error recalcing ", ee);
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
        return "Regenerate structure properties collection for all chemicals in the database";
    }

}
