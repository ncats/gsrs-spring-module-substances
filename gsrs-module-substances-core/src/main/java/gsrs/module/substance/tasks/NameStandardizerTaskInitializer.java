package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.config.FilePathParserUtils;
import gsrs.module.substance.repository.NameRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.utils.FDAFullNameStandardizer;
import gsrs.module.substance.utils.NameStandardizer;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.Name;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Data
public class NameStandardizerTaskInitializer extends ScheduledTaskInitializer {

    private String regenerateNameValue = "";
    private Boolean forceRecalculationOfAll =false;
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH-mm-ss");
    private String outputPath;
    private String name = "nameStandardizationReport";
    private String STANDARD_FILE_ENCODING ="UTF-8";
    private NameStandardizer nameStandardizer = new FDAFullNameStandardizer();

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

        File writeFile = getOutputFile();
        File abfile = writeFile.getAbsoluteFile();
        File pfile = abfile.getParentFile();

        pfile.mkdirs();
        log.trace("Going to instantiate standardizer with name {}; forceRecalculationOfAll {}", this.nameStandardizer.getClass().getName(),
                this.forceRecalculationOfAll);
        l.message("Initializing standardization");
        log.trace("Initializing standardization");

        l.message("Initializing name standardization: acquiring list");

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        l.message("Initializing name standardization: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing name standardization: starting process");
        log.trace("starting process");

        try (PrintStream out = makePrintStream(writeFile)){
            out.print("Existing standardized name\tNew standardized name\tMessage\n");
            adminService.runAs(adminAuth, (Runnable) () -> {
                TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                log.trace("got outer tx " + tx);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    processNames(l, out);
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

    public void setNameStandardizerClassName(String nameStandardizerClassName) throws Exception {
        this.nameStandardizer = (NameStandardizer) Class.forName(nameStandardizerClassName).getDeclaredConstructor().newInstance();
    }

    /**
     * Returns the File used to output the report
     *
     * @return File object ready for report output
     */
    private File getOutputFile() {
        return FilePathParserUtils.getFileParserBuilder()
                .suppliedFilePath(outputPath)
                .defaultFilePath("reports/" + name + "-%DATE% %TIME%.txt")
                .dateFormatter(formatter)
                .timeFormatter(formatterTime)
                .build()
                .getFile();
    }

    private boolean standardizeName(Name name, PrintStream printStream){
        log.trace("starting in standardizeName");
        AtomicBoolean nameChanged = new AtomicBoolean(false);
        try {
                log.trace("in StandardNameValidator, Name '{}'; stand.  name: '{}'", name.name, name.stdName);

                String prevStdName = name.stdName;
                String newlyStdName =this.nameStandardizer.standardize(name.name).getResult();
                if (!newlyStdName.equals(prevStdName)) {
                    printStream.format( "%s\t%s\tExisting standardized name for %s, '%s' differs from automatically standardized name: '%s'\n",
                            prevStdName, newlyStdName, name.name, prevStdName, newlyStdName);
                }
                if (forceRecalculationOfAll || (name.stdName != null && name.stdName.equals(regenerateNameValue))) {
                    name.stdName = null;
                }

                if (name.stdName == null || name.stdName.length()==0) {
                    name.stdName = newlyStdName;
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

    private void processNames(SchedulerPlugin.TaskListener l, PrintStream printStream){
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
            if(standardizeName(name, printStream) ) {
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

    private PrintStream makePrintStream(File writeFile) throws IOException {
        return new PrintStream(
                new BufferedOutputStream(new FileOutputStream(writeFile)),
                false, STANDARD_FILE_ENCODING);
    }
}
