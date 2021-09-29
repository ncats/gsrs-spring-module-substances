package gsrs.module.substance.tasks;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import gov.nih.ncats.common.util.TimeUtil;
import gov.nih.ncats.common.util.Unchecked;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin.TaskListener;
import ix.core.models.Principal;
import ix.ginas.exporters.ExportMetaData;
import ix.ginas.exporters.ExportProcess;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.models.v1.Substance;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.service.ExportService;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.OutputFormat;
import java.util.HashMap;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class ScheduledExportTaskInitializer extends ScheduledTaskInitializer {

    private String username;
    private boolean publicOnly =false;
    
    //removing @JsonProperty per suggestion from Danny 29 September 2021
    //@JsonProperty("publicOnly")
    public void setPublicOnly(boolean p) {
        publicOnly=p;
    }

    //@JsonProperty("username")
    public void setUsername(String username) {
        this.username = username;
    }
    private String name = "Full Data Export";

    //@JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private ExportService exportService;

    @Autowired
    private GsrsExportConfiguration gsrsExportConfiguration;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    
    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Override
    public String getDescription() {
        return name + " for " + username;
    }

    public String getExtension() {
        return "gsrs";
    }

    public String getCollectionID() {
        return "export-all-gsrs";
    }

    public Function<String, String> fileNameGenerator() {
        return date -> "auto-export-" + date;
    }


    @Override
    public void run(TaskListener l) {
        log.debug("About to call runAsAdmin with transaction");
        
        TransactionTemplate transactionRunReport = new TransactionTemplate(transactionManager);
        transactionRunReport.setReadOnly(true);
        transactionRunReport.executeWithoutResult((s)->{
            handleRun(l);
            log.debug("completed handleRun");
        });
    }
    
    private void handleRun(TaskListener l) {
        // TODO Auto-generated method stub

        log.debug("Running export");
        try {

            Principal user = new Principal(username, null);
            String collectionID = getCollectionID();
            String extension = getExtension();

            ExportMetaData emd = new ExportMetaData(collectionID, null, user.username, publicOnly, extension)
                    .onTotalChanged((c) -> {
                        l.message("Exported " + c + " records");
                    });

            LocalDate ld = TimeUtil.getCurrentLocalDate();
            String date = ld.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String fname = fileNameGenerator().apply(date) + "." + extension;

            emd.setDisplayFilename(fname);
            emd.originalQuery = null;

//TODO: There's no equivalent to any of this in 3.0 right now.
// we probably need to add it.
//            ExportProcess<Substance> p = new ExportProcessFactory().getProcess(emd,
//                    ProcessExecutionService.CommonStreamSuppliers.allForDeep(Substance.class));
            //can get all substances from repository... get a supplier for a stream 
            //don't run in a background thread
            //  inject substance repo
            //  inject gsrs configuration
            Map<String, String> parameters = new HashMap<>();

            Stream<Substance> substanceStream = getStreamSupplier();
            Stream<Substance> effectivelyFinalStream = filterStream(substanceStream, publicOnly, parameters);
            log.trace("exportService: " + exportService.getClass().getName() + exportService.getClass().getCanonicalName());
            ExportProcess<Substance> p = exportService.createExport(emd,() -> effectivelyFinalStream);
            log.trace("p: " + (p==null ? "null" : "not null"));
                        log.trace("publicOnly: " + publicOnly);
            //based on troubleshooting session 27 Sept 2021
            p.run(r->r.run(), out -> Unchecked.uncheck(() -> getExporterFor(extension, out, publicOnly, parameters)));

        } catch (Exception e) {
            log.error("Error in ScheduledExportTaskInitializer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Exporter<Substance> getExporterFor(String extension, OutputStream pos, boolean publicOnly, Map<String, String> parameters)
            throws IOException {

        log.trace("getExporterFor, extension: " + extension + "; pos: " + pos + "parameters: " + parameters);
        ExporterFactory.Parameters params = createParamters(extension, publicOnly, parameters);
        log.trace("create params");

        log.trace("gsrsExportConfiguration: " + (gsrsExportConfiguration==null ? "null" : "not null"));
        ExporterFactory<Substance> factory = gsrsExportConfiguration.getExporterFor(substanceEntityService.getContext(), params);
        log.trace("factory: " + factory);
        if (factory == null) {
            // TODO handle null couldn't find factory for params
            throw new IllegalArgumentException("could not find suitable factory for " + params);
        }
        return factory.createNewExporter(pos, params);
    }

    protected ExporterFactory.Parameters createParamters(String extension, boolean publicOnly, Map<String, String> parameters) {
        for (OutputFormat f : gsrsExportConfiguration.getAllSupportedFormats(substanceEntityService.getContext())) {
            if (extension.equals(f.getExtension())) {
                return new DefaultParameters(f, publicOnly);
            }
        }
        throw new IllegalArgumentException("could not find supported exporter for extension '" + extension + "'");

    }

    private Stream<Substance> getStreamSupplier() {
        return substanceRepository.streamAll();
    }


    protected Stream<Substance> filterStream(Stream<Substance> stream, boolean publicOnly, Map<String, String> parameters) {
        if (publicOnly) {
            return stream.filter(s -> s.getAccess().isEmpty());
        }
        return stream;
    }


}
