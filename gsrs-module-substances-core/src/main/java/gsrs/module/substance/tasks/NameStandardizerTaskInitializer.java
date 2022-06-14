package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.module.substance.repository.NameRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.utils.NameStandardizer;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.Name;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class NameStandardizerTaskInitializer extends ScheduledTaskInitializer {

    private String nameStandardizerClassName = null;
    private String regenerateNameValue = "";

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private NameRepository nameRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        if( this.nameStandardizerClassName== null || this.nameStandardizerClassName.length()==0) {
            this.nameStandardizerClassName="gsrs.module.substance.utils.FDAFullNameStandardardizer";
        }

        log.trace("Going to instantiate standardizer with name {}", this.nameStandardizerClassName);
        NameStandardizer nameStandardizer;
        try {
            nameStandardizer = (NameStandardizer) Class.forName(this.nameStandardizerClassName).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            log.error(e.getMessage());
            log.error("cannot perform standardization; no standardizer");
            return;
        }
        l.message("Initializing standardization");
        log.trace("Initializing standardization");
        //long totalSubstances =substanceRepository.count();

        /*ProcessListener listen = ProcessListener.onCountChange((sofar, total) -> {
            if (total != null) {
                l.message("Generated standard names for :" + sofar + " of " + total);
            } else {
                l.message("Generated standard names for :" + sofar);
            }
        });*/

        l.message("Initializing name standardization: acquiring list");

        /*listen.newProcess();
        listen.totalRecordsToProcess((int)totalSubstances);
        log.trace("got list. size: {}", totalSubstances);*/

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        l.message("Initializing name standardization: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing name standardization: starting process");
        log.trace("starting process");

        try {
            adminService.runAs(adminAuth, (Runnable) () -> {
                TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                log.trace("got outer tx " + tx);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    processNames(nameStandardizer, l);
                } catch (Exception ex) {
                    l.message("Error standardizing. error: " + ex.getMessage());
                }
            });
        } catch (Exception ee) {
            log.error("error generating standard names: ", ee);
            l.message("ERROR:" + ee.getMessage());
            throw new RuntimeException(ee);
        }

        l.message("Shutting down executor service");
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            //should never happen
            log.error("Interrupted exception!");
        }
        l.message("Task finished");
        l.complete();
        //listen.doneProcess();
    }

    @Override
    public String getDescription() {
        return "Regenerate standardized names for all substances in the database";
    }

    public void setNameStandardizerClassName(String nameStandardizerClassName) {
        this.nameStandardizerClassName = nameStandardizerClassName;
    }

    public void setRegenerateNameValue(String regenerateNameValue) {
        this.regenerateNameValue = regenerateNameValue;
    }

    private boolean standardizeName(Name name, NameStandardizer nameStandardizer){
        log.trace("starting in standardizeName");
        AtomicBoolean nameChanged = new AtomicBoolean(false);
        try {
                log.trace("in StandardNameValidator, Name '{}'; stand.  name: '{}'", name.name, name.stdName);

                if (name.stdName != null && name.stdName.equals(regenerateNameValue)) {
                    name.stdName = null;
                }

                if (name.stdName == null || name.stdName.length()==0) {
                    name.stdName = nameStandardizer.standardize(name.name).getResult();
                    log.debug("set (previously null) stdName to " + name.stdName);
                    nameChanged.set(true);
                }

        } catch (Exception ex) {
            log.error("Error processing names");
            log.error(ex.getMessage());
            ex.printStackTrace();
        }
        return nameChanged.get();
    }

    private void processNames(NameStandardizer nameStandardizer,  SchedulerPlugin.TaskListener l){
        log.trace("starting in processNames");

        List<String> nameIds= nameRepository.getAllUuids();
        log.trace("total names: {}", nameIds.size());
        int soFar =0;
        for (String nameId: nameIds) {
            soFar++;
            log.trace("going to fetch name with ID {}", nameId);
            UUID nameUuid= UUID.fromString(nameId);
            Optional<Name> nameOpt = nameRepository.findById(nameUuid);
            if( !nameOpt.isPresent()){
                log.info("No name found with ID {}", nameId);
                continue;
            }
            Name name = nameOpt.get();
            log.trace("processing name with ID {}", name.uuid.toString());
            if(standardizeName(name, nameStandardizer) ) {
                try {
                    log.trace("resaving name {}", name.name);
                    name.forceUpdate();
                    TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                    log.trace("got tx " + tx);
                    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    tx.setReadOnly(false);
                    tx.executeWithoutResult(c-> {
                        log.trace("before saveAndFlush");
                        log.trace("key: " + EntityUtils.EntityWrapper.of(name).getKey());
                        //log.trace("json: " + EntityUtils.EntityWrapper.of(name).toInternalJson());
                        log.trace("name dirtiness: " + name.isDirty());
                        //hack to make sure name persists
                        Name name2 = StaticContextAccessor.getEntityManagerFor(Name.class).merge(name);
                        //log.trace("name2 dirtiness: " + name2.isDirty());
                        name2.forceUpdate();
                        //log.trace("name2 dirtiness after update: " + name2.isDirty());
                        nameRepository.saveAndFlush(name2);
                        log.trace("finished saveAndFlush");
                    });

                    log.trace("saved name {}", name.name);
                } catch (Exception ex) {
                    log.error("Error during save: {}", ex.getMessage());
                }
            }
            l.message(String.format("Processed %d of %d names", soFar, nameIds.size()));
        }
    }


}
