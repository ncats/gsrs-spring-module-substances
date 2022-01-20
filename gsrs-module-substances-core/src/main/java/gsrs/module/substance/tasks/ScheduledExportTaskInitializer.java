package gsrs.module.substance.tasks;

import gov.nih.ncats.common.util.TimeUtil;
import gov.nih.ncats.common.util.Unchecked;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.config.FilePathParserUtils;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.scheduledTasks.SchedulerPlugin.TaskListener;
import gsrs.service.DefaultExportService;
import gsrs.service.ExportService;
import ix.core.models.Principal;
import ix.ginas.exporters.*;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public class ScheduledExportTaskInitializer extends ScheduledTaskInitializer {

    private String username;
    private boolean publicOnly =false;
    private String outputPath =null;
    private String name = "Full Data Export";
    
    
    public void setPublicOnly(boolean p) {
        publicOnly=p;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    

    public void setName(String name) {
        this.name = name;
    }
    

    public void setOutputPath(String path) {
        outputPath=path;
    }

    public File getOutputDir() {
        if(outputPath==null)return null;
        return FilePathParserUtils.getFileParserBuilder()
                           .suppliedFilePath(outputPath)
                           .build()
                           .getFile();
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

    public ExportService getExportService() {
        if(getOutputDir()==null) {
            return this.exportService;
        }else {
            //This isn't ideal as there's a disconnect between what the config says and what the export
            //service says, especially in terms of user
            ExportService es = new DefaultExportService(gsrsExportConfiguration, getOutputDir());
            return es;
        }
    }

    @Override
    public void run(SchedulerPlugin.JobStats stats, TaskListener l) {
        log.debug("About to call runAsAdmin with transaction");
        
        TransactionTemplate transactionRunReport = new TransactionTemplate(transactionManager);
        transactionRunReport.setReadOnly(true);
        transactionRunReport.executeWithoutResult((s)->{
            handleRun(l);
            log.debug("completed handleRun");
        });
    }
    
    private void handleRun(TaskListener l) {
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

            Map<String, String> parameters = new HashMap<>();

            Stream<Substance> substanceStream = getStreamSupplier();
            Stream<Substance> effectivelyFinalStream = filterStream(substanceStream, publicOnly, parameters);
            ExportService usedExportService = getExportService();
            
            log.trace("exportService: " + usedExportService.getClass().getName() + usedExportService.getClass().getCanonicalName());
            ExportProcess<Substance> p = usedExportService.createExport(emd,() -> effectivelyFinalStream);
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
