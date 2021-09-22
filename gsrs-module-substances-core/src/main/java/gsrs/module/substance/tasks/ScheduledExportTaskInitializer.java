package gsrs.module.substance.tasks;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import gov.nih.ncats.common.yield.Yield;
import gsrs.controller.GsrsControllerUtil;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.service.ExportService;
import gsrs.service.GsrsEntityService;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Code;
import static java.lang.String.format;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
public class ScheduledExportTaskInitializer extends ScheduledTaskInitializer {

    private String username;
    private String name = "Full Data Export";

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private ExportService exportService;

    @Autowired
    private GsrsExportConfiguration gsrsExportConfiguration;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @Autowired
    private TaskExecutor taskExecutor;


    /*@Override
    public Initializer initializeWith(Map<String, ?> m) {
    	super.initializeWith(m);
        username=Optional.ofNullable((String)m.get("username")).orElse("admin");
        name=Optional.ofNullable((String)m.get("name"))
                     .orElse(name);
        additionalInitializeWith(m);
        
        return this;
    }*/
    @Override
    public String getDescription() {
        return name + " for " + username;
    }

    /*protected void additionalInitializeWith(Map<String, ?> m){

    }*/
    protected String getExtension() {
        return "gsrs";
    }

    protected String getCollectionID() {
        return "export-all-gsrs";
    }

    public boolean publicOnly() {
        return false;
    }

    public Function<String, String> fileNameGenerator() {
        return date -> "auto-export-" + date;
    }

//    protected Supplier<ExportProcessFactory> exportFactorySupplier() {
//        return () -> new ExportProcessFactory();
//    }
    private ExecutorService getExecutorService() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        return executor;
    }

    @Override
    public void run(TaskListener l) {
        // TODO Auto-generated method stub

        log.debug("Running export");
        try {

            Principal user = new Principal(username, null);
            String collectionID = getCollectionID();
            String extension = getExtension();
            boolean publicOnly = publicOnly();

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
            ExportProcess<Substance> p = exportService.createExport(emd,
                    () -> effectivelyFinalStream);
            log.trace("p: " + (p==null ? "null" : "not null"));
            log.trace("taskExecutor: " + (taskExecutor==null ? "null" : "not null"));
            log.trace("publicOnly: " + publicOnly);
            p.run(taskExecutor, out -> Unchecked.uncheck(() -> getExporterFor(extension, out, publicOnly, parameters)));

            /*boolean stillRunning = true;
            do {
                try {
                    future.get(3, TimeUnit.SECONDS);
                    stillRunning = false;
                } catch (TimeoutException ignored) {
//                    if(Thread.currentThread().isInterrupted()){
//                        System.out.println("THREAD WAS INTERRUPTED");
//                        emd.cancel();
//                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("got interrupted exception");
                    emd.cancel();
                }
            } while (stillRunning);*/
        } catch (Exception e) {
            log.error("Error in ScheduledExportTaskInitializer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*private Exporter getExporter(OutputStream out, ExporterFactory.Parameters params) throws IOException {
        ExporterFactory<Substance> factory = gsrsExportConfiguration.getExporterFor("substances", params);
        if (factory == null) {
            // TODO handle null couldn't find factory for params
            throw new IllegalArgumentException("could not find suitable factory for " + params);
        }
        return factory.createNewExporter(out, params);
    }*/
    private Exporter<Substance> getExporterFor(String extension, OutputStream pos, boolean publicOnly, Map<String, String> parameters)
            throws IOException {

        log.trace("getExporterFor, extension: " + extension + "; pos: " + pos + "parameters: " + parameters);
        ExporterFactory.Parameters params = createParamters(extension, publicOnly, parameters);
        log.trace("create params");

        log.trace("gsrsExportConfiguration: " + (gsrsExportConfiguration==null ? "null" : "not null"));
        ExporterFactory<Substance> factory = gsrsExportConfiguration.getExporterFor(this.getEntityService().getContext(), params);
        log.trace("factory: " + factory);
        if (factory == null) {
            // TODO handle null couldn't find factory for params
            throw new IllegalArgumentException("could not find suitable factory for " + params);
        }
        return factory.createNewExporter(pos, params);
    }

    protected ExporterFactory.Parameters createParamters(String extension, boolean publicOnly, Map<String, String> parameters) {
        for (OutputFormat f : gsrsExportConfiguration.getAllSupportedFormats(this.getEntityService().getContext())) {
            if (extension.equals(f.getExtension())) {
                return new DefaultParameters(f, publicOnly);
            }
        }
        throw new IllegalArgumentException("could not find supported exporter for extension '" + extension + "'");

    }

    private Stream<Substance> getStreamSupplier() {
        return substanceRepository.findAll().stream();
    }

    
//        return () -> (Stream<Substance>) Yield.<Substance>create(yieldRecipe -> {
//            substanceRepository.findAll().stream();
//        });
    

    protected Stream<Substance> filterStream(Stream<Substance> stream, boolean publicOnly, Map<String, String> parameters) {
        if (publicOnly) {
            return stream.filter(s -> s.getAccess().isEmpty());
        }
        return stream;
    }

    public GsrsEntityService<Substance, UUID> getEntityService() {
        log.trace("substanceEntityService: " + substanceEntityService);
        return substanceEntityService;
    }

    /*private ResponseEntity createExport(boolean publicOnly, String format, ExportMetaData emd,
            Map<String, String> parameters) throws Exception {
        Stream<Substance> substanceStream = getStreamSupplier().get();
        Stream<Substance> effectivelyFinalStream = filterStream(substanceStream, publicOnly, parameters);
        ExportProcess<Substance> p = exportService.createExport(emd,
                () -> effectivelyFinalStream);
        p.run(taskExecutor, out -> Unchecked.uncheck(() -> getExporterFor(format, out, publicOnly, parameters)));
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(p.getMetaData(), parameters), HttpStatus.OK);
    }*/
}
