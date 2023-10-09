package gsrs.module.substance.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.EntityPersistAdapter;
import gsrs.GsrsEntityProcessorListener;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gsrs.config.FilePathParserUtils;
import gsrs.module.substance.repository.NameRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.standardizer.NameStandardizer;
import gsrs.module.substance.standardizer.NameStandardizerConfiguration;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import gsrs.springUtils.StaticContextAccessor;
import ix.ginas.models.v1.Name;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.EntityManager;

@Slf4j
@Data
public class NameStandardizerTaskInitializer extends ScheduledTaskInitializer {

    private String regenerateNameValue = "";
    private Boolean forceRecalculationOfAll =false;

    @JsonIgnore
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
    @JsonIgnore
    private DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH-mm-ss");


    private String outputPath;
    private String name = "nameStandardizationReport";
    private String STANDARD_FILE_ENCODING ="UTF-8";
    private String description;
    private boolean disabledHistory = false;
    private boolean disabledHooks = false;
    private boolean reportAutoflush = false;

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private NameRepository nameRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;


    @Autowired
    private NameStandardizerConfiguration nameStdConfig;

    private NameStandardizer stdNameStandardizer;

    @JsonProperty("formatter")
    public void setFormat(String format) {
        if(format !=null){
            formatter = DateTimeFormatter.ofPattern(format);
        }
    }

    @JsonProperty("formatterTime")
    public void setFormatTime(String format) {
        if(format !=null){
            formatterTime = DateTimeFormatter.ofPattern(format);
        }
    }

    private void initIfNeeded() {
        try {
            if(stdNameStandardizer==null) {
                if(nameStdConfig!=null) {
                    stdNameStandardizer=nameStdConfig.stdNameStandardizer();
                }
            }
        }catch(Exception e) {
            log.warn("trouble instantiating name standardizer", e);
        }
    }

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        long start = TimeUtil.getCurrentTimeMillis();
        initIfNeeded();
        File writeFile = getOutputFile();
        File abfile = writeFile.getAbsoluteFile();
        File pfile = abfile.getParentFile();

        pfile.mkdirs();

        boolean canWrite = abfile.canWrite();
        System.out.println(String.format("The Name standardizer task wants to write to the file: %s ...  This file is writeable: %s",
        abfile.getAbsolutePath(), canWrite));

        log.trace("Going to instantiate standardizer with name {}; forceRecalculationOfAll {}", this.stdNameStandardizer.getClass().getName(),
            this.forceRecalculationOfAll);
        l.message("Initializing standardization");
        log.trace("Initializing standardization");

        l.message("Initializing name standardization: acquiring list");

        l.message("Initializing name standardization: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing name standardization: starting process");
        log.trace("starting process");

        try (PrintStream out = makePrintStream(writeFile)){
            out.print("Existing standardized name\tNew standardized name\tMessage\n");
            adminService.runAs(adminAuth, (Runnable) () -> {
                try {
                    processNames(l, out);
                } catch (Exception ex) {
                    l.message("Error standardizing. error: " + ex.getMessage());
                }
            });
        } catch (Exception ee) {
            log.error("Error generating standard names: ", ee);
            l.message("ERROR:" + ee.getMessage());
            throw new RuntimeException(ee);
        }

        long end = TimeUtil.getCurrentTimeMillis();
        l.complete();
        l.message("Full time to execute, (End-Start): "+ (end-start));
        log.info("Full time to execute, (End-Start): "+ (end-start));
    }

    @Override
    public String getDescription() {
        if( description == null) {
            return "Regenerate standardized names for all substances in the database";
        }
        else {
            return description;
        }
    }

    public void setNameStandardizerClassName(String stdNameStandardizerClassName) throws Exception {
        this.stdNameStandardizer = (NameStandardizer) Class.forName(stdNameStandardizerClassName).getDeclaredConstructor().newInstance();
    }

    /**
     * Returns the File used to output the report
     *
     * @return File object ready for report output
     */
    private File getOutputFile() {
        // It will use supplied path if outputPath is specified in the task config.
        // On single Tomcat, it may be best to use a full path for the supplied path.
        return FilePathParserUtils.getFileParserBuilder()
            .suppliedFilePath(outputPath)
            .defaultFilePath("reports/" + name + "-%DATE% %TIME%.txt")
            .dateFormatter(formatter)
            .timeFormatter(formatterTime)
            .build()
            .getFile();
    }

    private boolean standardizeName(Name name, PrintStream printStream){
        initIfNeeded();
        //log.trace("starting in standardizeName");
        Boolean nameChanged = false;
        try {
            log.trace("in StandardNameValidator, Name '{}'; stand.  name: '{}'", name.getName(), name.stdName);

            String prevStdName = name.stdName;
            String newlyStdName =this.stdNameStandardizer.standardize(name.getName()).getResult();
            if (!newlyStdName.equals(prevStdName)) {
                printStream.format( "%s\t%s\tExisting standardized name for %s, '%s' differs from automatically standardized name: '%s'\n",
                prevStdName, newlyStdName, name.getName(), prevStdName, newlyStdName);
            }
            // If told explicitly to regenerate all names, or if stdName not null but effectively null, then null out the stdName field
            // to signal that the name should be regenerated.
            if (forceRecalculationOfAll || (name.stdName != null && name.stdName.equals(regenerateNameValue))) {
                name.stdName = null;
            }

            if (name.stdName == null || name.stdName.length()==0) {
                name.stdName = newlyStdName;
                log.debug("set (previously null) stdName to " + name.stdName);
                nameChanged= true;
            }

        } catch (Exception ex) {
            log.error("Error processing names");
            log.error(ex.getMessage());
        }
        return nameChanged;
    }

    private void processNames(SchedulerPlugin.TaskListener l, PrintStream printStream){
        log.trace("starting in processNames");
        List<String> nameIds= nameRepository.getAllUuids();
        log.trace("total names: {}", nameIds.size());
        log.info(String.format("Running NameStandardizer Task with disabledHistory=%s, disabledHooks=%s", disabledHistory, disabledHooks));
        EntityManager em = StaticContextAccessor.getEntityManagerFor(Name.class);
        EntityPersistAdapter epa = StaticContextAccessor.getBean(EntityPersistAdapter.class);
        EntityUtils.EntityInfo<Name> nei= EntityUtils.getEntityInfoFor(Name.class);
        AtomicInteger soFar = new AtomicInteger(0);
        nameIds.parallelStream().forEach(nameId->{
            soFar.incrementAndGet();
            log.trace("going to fetch name with ID {}", nameId);
            /*
            This way of getting the nameUuid did not work after moving the code block
            UUID nameUuid= UUID.fromString(nameId);
            Optional<Name> nameOpt = nameRepository.findById(nameUuid);
            */
            EntityUtils.Key key = EntityUtils.Key.ofStringId(nei, nameId);
            Optional<Name> nameOpt = EntityFetcher.of(key).getIfPossible().map(n -> (Name) n);
            if( !nameOpt.isPresent()){
                log.info("No name found with ID {}", nameId);
                return;
            }
            Name name = nameOpt.get();
            log.trace("processing name with ID {}", name.uuid.toString());
            // If this method (standardizeName) ends up mutating the name, then we do the following steps
            if(standardizeName(name, printStream) ) {
                try {
                    log.trace("resaving name {}", name.getName());
                    TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                    tx.setReadOnly(false);

                    tx.executeWithoutResult(c-> {
                        GsrsEntityProcessorListener b =  StaticContextAccessor.getBean(GsrsEntityProcessorListener.class);
                        if (disabledHistory && disabledHooks) {
                            // Case 1 both disabled History and Hooks
                            epa.runWithDisabledHistory(()-> {
                                b.runWithDisabledHooks(()->{
                                    // System.out.println("After runWithDisabledHooks, the Thread name is " + Thread.currentThread().getName());
                                    // System.out.println("The processorId is: " + b.getProcessorId());
                                    saveWork(em, name);
                                });
                            });
                        } else if (disabledHistory) {
                            // Case 2 only disabledHistory
                            epa.runWithDisabledHistory(()-> {
                                saveWork(em, name);
                            });
                        } else if (disabledHooks) {
                            // Case 3 only disabledHooks
                            b.runWithDisabledHooks(()->{
                                saveWork(em, name);
                            }); // HOOKS
                        } else {
                            // Case 4 neither disabled
                            saveWork(em, name);
                        }
                    }); // executeWithoutResult
                } catch (Exception ex) {
                    log.error("Error during save: {}", ex.getMessage());
                }
            }
            l.message(String.format("Processed %d of %d names", soFar.get(), nameIds.size()));
            log.trace(String.format("Processed %d of %d names", soFar.get(), nameIds.size()));
        });
    }

    private void saveWork(EntityManager em, Name name) {
        try {
            log.trace("before saveAndFlush");
            // log.trace("key: " + EntityUtils.EntityWrapper.of(name).getKey());
            // log.trace("json: " + EntityUtils.EntityWrapper.of(name).toInternalJson());
            // hack to make sure name persists
            Name name2 = em.merge(name);
            // log.trace("name2 dirtiness: {}" , (log.isTraceEnabled()) ? name2.isDirty(): "");
            name2.forceUpdate();
            log.trace("name2 dirtiness after update: {}", (log.isTraceEnabled()) ? name2.isDirty() : "");
            nameRepository.saveAndFlush(name2);
            // log.trace("finished saveAndFlush");
        } catch (Exception ex) {
            log.error("Error during save while executing transaction: {}", ex.getMessage());
        }
    }

    private PrintStream makePrintStream(File writeFile) throws IOException {
        return new PrintStream(
            new BufferedOutputStream(new FileOutputStream(writeFile)),
            reportAutoflush, STANDARD_FILE_ENCODING
        );
    }
}

