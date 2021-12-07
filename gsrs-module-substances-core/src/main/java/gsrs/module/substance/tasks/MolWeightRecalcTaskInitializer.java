package gsrs.module.substance.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.module.substance.processors.ConfigurableMolweightProcessor;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.services.RecalcStructurePropertiesService;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import ix.core.utils.executor.ProcessListener;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MolWeightRecalcTaskInitializer extends ScheduledTaskInitializer {

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private RecalcStructurePropertiesService recalcStructurePropertiesService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private SubstanceRepository substanceRepository;

    private String atomWeightFilePath;
    private String persistenceMode;
    private String propertyName;
    private String oldPropertyName;

    private Integer decimalDigits = 0;

    @Autowired
    private AdminService adminService;

    @Override
    @Transactional
    public void run(SchedulerPlugin.TaskListener l)
    {
        l.message("Initializing rehashing");
        Map<String, Object> configValues = new HashMap<>();
        configValues.put("atomWeightFilePath", atomWeightFilePath);
        log.trace("atomWeightFilePath: " + atomWeightFilePath);
        configValues.put("persistenceMode", persistenceMode);
        log.trace("persistenceMode: " + persistenceMode);
        configValues.put("propertyName", propertyName);
        configValues.put("decimalDigits", decimalDigits);
        configValues.put("oldPropertyName", oldPropertyName);

        ConfigurableMolweightProcessor processor = new ConfigurableMolweightProcessor(configValues);
        log.trace("instantiated processor");

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

        listen.newProcess();
        //listen.totalRecordsToProcess(ids.size());

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        l.message("Initializing rehashing: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing rehashing: starting process");

        try{
            for (Substance substance : substanceRepository.findAll()) {
                executor.submit(() -> {
                    try{
                        adminService.runAs(adminAuth, () -> {
                            TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                            try {
                                tx.executeWithoutResult(status -> {
                                    if ((substance instanceof ChemicalSubstance)) {
                                        ChemicalSubstance chem = (ChemicalSubstance) substance;
                                        log.trace("Starting calc for " + substance.uuid);
                                        processor.calculateMw(chem);
                                        substanceRepository.saveAndFlush(substance);
                                        log.trace("completed");
                                    }

                                });
                            } catch (Throwable ex) {
                                log.error("error recalculating molecular weight", ex);
                                l.message("Error recalculating molecular weight ... " + substance.getUuid() + " error: " + ex.getMessage());
                            }
                        });
                    }catch(Exception ex) {
                        l.message("Error reindexing ... " + substance.getUuid() + " error: " + ex.getMessage());
                    }
                });
            }
        }catch(Exception ee){
            log.error("error recalculating molecular weight ", ee);
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
        return "Generate or update a molecular weight (property) for every chemical substance in the database using configured atomic weights";
    }

    public String getAtomWeightFilePath() {
        return atomWeightFilePath;
    }

    @JsonProperty("atomWeightFilePath")
    public void setAtomWeightFilePath(String atomWeightFilePath) {

        this.atomWeightFilePath = atomWeightFilePath;
    }

    public String getPersistenceMode() {
        return persistenceMode;
    }

    @JsonProperty("persistenceMode")
    public void setPersistenceMode(String persistenceMode) {
        this.persistenceMode = persistenceMode;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Integer getDecimalDigits() {
        return decimalDigits;
    }

    public void setDecimalDigits(Integer decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    public String getOldPropertyName() {
        return oldPropertyName;
    }

    public void setOldPropertyName(String oldPropertyName) {
        this.oldPropertyName = oldPropertyName;
    }
}
