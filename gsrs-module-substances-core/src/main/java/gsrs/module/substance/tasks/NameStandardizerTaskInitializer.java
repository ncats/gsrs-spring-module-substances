package gsrs.module.substance.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import gov.nih.ncats.common.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
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
                    // this isn't actually used.
                    // TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                    // log.trace("got outer tx " + tx);
                    // tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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

        // l.message("Shutting down executor service");
        // executor modified above we only have one so we don't need to wait/shutdown

        long end = TimeUtil.getCurrentTimeMillis();
        l.complete();
        l.message("Full time to execute, (End-Start): "+ (end-start));
        // change later to info or nothing
        log.error("Full time to execute, (End-Start): "+ (end-start));
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
        return FilePathParserUtils.getFileParserBuilder()
                // will use supplied if outputPath specified in config; it is the full filepath.
                // otherwise it will use the defaultFilePath
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
                // If told explicitly to regen all names, or if stdname not null but effectively null, then null out the stdName field
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
        // ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);

        List<String> nameIds= nameRepository.getAllUuids();
        log.trace("total names: {}", nameIds.size());
        EntityManager em = StaticContextAccessor.getEntityManagerFor(Name.class);
            AtomicInteger soFar = new AtomicInteger(0);
nameIds.parallelStream().forEach(nameId->{
    soFar.incrementAndGet();
    log.trace("going to fetch name with ID {}", nameId);
    // change later or remove
    log.error("going to fetch name with ID {}", nameId);
    UUID nameUuid= UUID.fromString(nameId);
    Optional<Name> nameOpt = nameRepository.findById(nameUuid);
    if( !nameOpt.isPresent()){
        log.info("No name found with ID {}", nameId);
        return;
    }
    Name name = nameOpt.get();
    log.trace("processing name with ID {}", name.uuid.toString());
    // If this method standardizeName ends up mutating the name, the we do the following steps
    if(standardizeName(name, printStream) ) {
                try {
                    log.trace("resaving name {}", name.getName());
                    //name.forceUpdate();
                    TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                    //log.trace("got tx " + tx);
                    tx.setReadOnly(false);
                    tx.executeWithoutResult(c-> {
                        log.trace("before saveAndFlush");
                        //log.trace("key: " + EntityUtils.EntityWrapper.of(name).getKey());
                        //log.trace("json: " + EntityUtils.EntityWrapper.of(name).toInternalJson());
                        //hack to make sure name persists
                        Name name2 = em.merge(name);
                        //log.trace("name2 dirtiness: " + name2.isDirty());
                        name2.forceUpdate();
                        // change was trace and commented out
                        log.error("name2 dirtiness after update: " + name2.isDirty());
                        nameRepository.saveAndFlush(name2);
                        //log.trace("finished saveAndFlush");
                    });

                    log.trace("saved name {}", name.getName());
                } catch (Exception ex) {
                    log.error("Error during save: {}", ex.getMessage());
                }
    }
    l.message(String.format("Processed %d of %d names", soFar, nameIds.size()));
});

    }

    private PrintStream makePrintStream(File writeFile) throws IOException {
        return new PrintStream(
                new BufferedOutputStream(new FileOutputStream(writeFile)),
                false, STANDARD_FILE_ENCODING);
    }
}
