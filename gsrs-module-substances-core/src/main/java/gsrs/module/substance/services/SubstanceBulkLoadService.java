package gsrs.module.substance.services;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import gsrs.security.canImportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.AuditConfig;
import gsrs.DefaultDataSourceConfig;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.repository.ProcessingJobRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.repository.ProcessingRecordRepository;
import gsrs.module.substance.repository.XRefRepository;
import gsrs.repository.PayloadRepository;
import gsrs.security.AdminService;

import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import gsrs.events.ReindexEntityEvent;
import ix.core.models.Keyword;
import ix.core.models.Payload;
import ix.core.models.ProcessingJob;
import ix.core.models.ProcessingJobUtils;
import ix.core.models.ProcessingRecord;
import ix.core.processing.PayloadExtractedRecord;
import ix.core.processing.PayloadProcessor;
import ix.core.processing.PersistRecordWorkerFactory;
import ix.core.processing.RecordExtractor;
import ix.core.processing.RecordPersister;
import ix.core.processing.RecordTransformer;
import ix.core.processing.TransformedRecord;
import ix.core.stats.Estimate;
import ix.core.stats.Statistics;
import ix.core.util.EntityUtils;
import ix.core.util.FilteredPrintStream;
import ix.core.util.Filters;
import ix.core.validator.ValidationMessage;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SubstanceBulkLoadService {
    /*
    These are the Loggers that are used in GSRS 2.x to log bulk loading problems
    and are configured in the logger xml configuration files by name in the resources area.
     */
    private static final Logger PersistFailLogger = LoggerFactory.getLogger("persistFail");
    private static final Logger TransformFailLogger = LoggerFactory.getLogger("transformFail");
    private static final Logger ExtractFailLogger = LoggerFactory.getLogger("extractFail");

    public static Logger getPersistFailureLogger(){
        return PersistFailLogger;
    }

    public static Logger getTransformFailureLogger(){
        return TransformFailLogger;
    }

    private final Object jobLock = new Object();

    /** Set when the application context is closing so running loads stop instead of finishing as COMPLETE. */
    private volatile boolean shuttingDown = false;

    private static final String KEY_PROCESS_QUEUE_SIZE = "PROCESS_QUEUE_SIZE";

    private static final long INDEX_QUIESCENCE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long INDEX_QUIESCENCE_POLL_MS = 250L;
    private static final int INDEX_QUIESCENCE_STABLE_SAMPLES = 8;
    //Hack variable for resisting buildup
    //of extracted records not yet transformed
    private static Map<String,Long> queueStatistics = new ConcurrentHashMap<String,Long>();
    private static Map<String,Statistics> jobCacheStatistics = new ConcurrentHashMap<>();

    private static final JsonMapper mapper = JsonMapper.builderWithJackson2Defaults()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private SubstanceBulkLoadServiceConfiguration configuration;

    private final Map<String, ExecutorService> executorServices = new ConcurrentHashMap<>();

    private final ProcessingJobRepository processingJobRepository;

    private final PlatformTransactionManager transactionManager;

    private final ConsoleFilterService consoleFilterService;

    private TaskExecutor taskExecutor;

    private final PayloadService payloadService;

    private final AdminService adminService;

    private final AuditConfig auditConfig;

    private PayloadRepository payloadRepository;
    private ObjectProvider<SubstanceLobSchemaCompatibilityInitializer> lobSchemaCompatibilityInitializerProvider;
    private ApplicationEventPublisher applicationEventPublisher;
    private ObjectProvider<SubstanceRepository> substanceRepositoryProvider;


    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
    private EntityManager entityManager;

    @Autowired
    public SubstanceBulkLoadService(
            SubstanceBulkLoadServiceConfiguration configuration,
            ProcessingJobRepository processingJobRepository,
            PlatformTransactionManager transactionManager,
            ConsoleFilterService consoleFilterService,
            PayloadService payloadService,
            AdminService adminService,
            AuditConfig auditConfig,
            PayloadRepository payloadRepository,
            TaskExecutor taskExecutor,
            ObjectProvider<SubstanceLobSchemaCompatibilityInitializer> lobSchemaCompatibilityInitializerProvider,
            ApplicationEventPublisher applicationEventPublisher,
            ObjectProvider<SubstanceRepository> substanceRepositoryProvider
            ) {
        this.configuration = configuration;
        this.processingJobRepository = processingJobRepository;
        this.transactionManager= transactionManager;
        this.consoleFilterService = consoleFilterService;
        this.payloadService = payloadService;
        this.adminService = adminService;
        this.auditConfig = auditConfig;
        this.payloadRepository = payloadRepository;
        this.taskExecutor = taskExecutor;
        this.lobSchemaCompatibilityInitializerProvider = lobSchemaCompatibilityInitializerProvider;
        this.applicationEventPublisher = applicationEventPublisher;
        this.substanceRepositoryProvider = substanceRepositoryProvider;
    }

    public Statistics getStatisticsFor(String jobId){
        return getStatisticsForJob(jobId);
    }

    private ProcessingJob saveJobInSeparateTransaction(long jobId, Statistics stats, ProcessingJob.Status desiredStatus,
                                                       String desiredMessage){
        synchronized (jobLock) {
            if(stats==null ) {
                //log.info("skipping save because stats is null");
                return null;
            }
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return tx.execute(status -> saveJobInCurrentTransaction(jobId, stats, desiredStatus, desiredMessage));
        }
    }

    private ProcessingJob saveJobInCurrentTransaction(long jobId, Statistics stats, ProcessingJob.Status desiredStatus,
                                                      String desiredMessage) {
        ProcessingJob job = processingJobRepository.findById(jobId).get();
        if (desiredStatus != null) {
            log.trace("using supplied status in saveJobInSeparateTransaction");
            job.status = desiredStatus;
        } else if (stats._isDone()) {
            log.trace("setting status to complete in saveJobInSeparateTransaction");
            job.status = ProcessingJob.Status.COMPLETE;
        } else {
            log.trace("setting status to running in saveJobInSeparateTransaction");
            job.status = ProcessingJob.Status.RUNNING;
        }

        if (desiredMessage != null) {
            job.message = desiredMessage;
        } else if (!stats._isDone()) {
            job.message = "Loading data";
        }
        job.statistics = mapper.valueToTree(stats).toString();

        job.setIsAllDirty();
        return processingJobRepository.saveAndFlush(job);
    }

    private void saveStoppedJobQuietly(long jobId, String jobKey, String message) {
        try {
            saveJobInSeparateTransaction(jobId, getStatisticsForJob(jobKey), ProcessingJob.Status.STOPPED, message);
        } catch (RuntimeException e) {
            // the database may already be closing during shutdown
            log.warn("Could not record stopped status for bulk load job {}: {}", jobId, e.getMessage());
        }
    }

    @PreDestroy
    public void onStop() {
        shuttingDown = true;
        for(ExecutorService s : executorServices.values()){
            s.shutdownNow();
        }
        executorServices.clear();
    }




    @Data
    @Builder
    public static class SubstanceBulkLoadParameters{
        private final Payload payload;
        private final boolean preserveOldEditInfo;


    }

    /**
     * Cancel currently running job.
     * @param processorJobKey
     * @return {@code true} if job cancelled; {@code false} if not cancelled
     * probably because it either didn't exist or isn't currently running.
     * @since 3.0
     */
   //@hasAdminRole
   @canImportData
   public boolean cancel(String processorJobKey){
        ExecutorService service = executorServices.remove(processorJobKey);
        if(service ==null){
            //cant cancel what's not running
             return false;
        }
        service.shutdownNow();
        applyStatisticsChangeForJob(processorJobKey, Statistics.CHANGE.CANCEL);
        return true;
       }
    //@hasAdminRole
    @canImportData
    public PayloadProcessor submit(SubstanceBulkLoadParameters parameters) {
        SubstanceLobSchemaCompatibilityInitializer initializer = lobSchemaCompatibilityInitializerProvider.getIfAvailable();
        if (initializer != null) {
            initializer.ensureCompatibility();
        }
        // first see if this payload has already processed..
        final PayloadProcessor pp = new PayloadProcessor(parameters.getPayload());


        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        pp.jobId = tx.execute(status->{
            ProcessingJob job = new ProcessingJob();
            job.start = TimeUtil.getCurrentTimeMillis();
            job.addKeyword(new Keyword(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY, pp.key));

            job.status = ProcessingJob.Status.PENDING;

            job.message="Preparing payload for processing";
            job.payload = payloadRepository.findById(pp.payloadId).get();

            processingJobRepository.saveAndFlush(job);
            return job.id;
        });
        storeStatisticsForJob(pp.key, new Statistics());


        int loadingThreads = Math.max(1, configuration.getLoadingThreads());
        int queueSize = Math.max(loadingThreads, configuration.getMaxQueueSize());
        final ExecutorService executorService =
                BlockingSubmitExecutor.newFixedThreadPool(loadingThreads, queueSize);

        final PersistRecordWorkerFactory factory = configuration.getPersistRecordWorkerFactory(parameters);

        executorServices.put( pp.key, executorService);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Runnable r= new Runnable() {

            @Override
            public void run() {
                boolean submissionStopped = false;
                TransactionTemplate tx2 = new TransactionTemplate(transactionManager);
                tx2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                ProcessingJob job = tx2.execute(s -> {
                    ProcessingJob innerJob = processingJobRepository.findById(pp.jobId).get();
                    EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(innerJob);
                    return innerJob;
                });
                log.trace("first call to saveJobInSeparateTransaction");
                saveJobInSeparateTransaction(pp.jobId, getStatisticsForJob(pp.key), ProcessingJob.Status.RUNNING, null);
                BulkLoadServiceCallBackImpl callback = new BulkLoadServiceCallBackImpl(pp.key);
                FilteredPrintStream.Filter filterOutJChem = Filters.filterOutClasses(Pattern.compile("chemaxon\\..*|lychi\\..*"));

                //katzelda 6/2019: IDE says we don't ever use the FilterSessions but we do it's just a sideeffect that gets used when we
                //invoke the code inside the try and gets popped when we close so the de-sugared code of the try-with resources does use it
                //so keep it !!
                try (FilteredPrintStream.FilterSession ignoreChemAxonSTDOUT = consoleFilterService.getStdOutOutputFilter().newFilter(filterOutJChem);
                     FilteredPrintStream.FilterSession ignoreChemAxonSTDERR = consoleFilterService.getStdErrOutputFilter().newFilter(filterOutJChem);
                ){
                    Payload tmpPayload = payloadRepository.findById(pp.payloadId).get();
                    try (InputStream in = payloadService.getPayloadAsInputStream(tmpPayload).get()){
                        Estimate es  = configuration.getRecordExtractorFactory().estimateRecordCount(in);
                        log.debug("Counted records");
                        Statistics stat = getStatisticsForJob(pp.key);
                        if (stat == null) {
                            stat = new Statistics();
                        }
                        stat.totalRecords = es;
                        stat.applyChange(Statistics.CHANGE.EXPLICIT_CHANGE);
                        storeStatisticsForJob(pp.key, stat);
                        log.debug(stat.toString());
                    }catch(IOException e){
                        log.error("error retrieving payload", e);
                        saveJobInSeparateTransaction(pp.jobId, getStatisticsForJob(pp.key), ProcessingJob.Status.FAILED, null);
                    }
                    log.trace("saveJobInSeparateTransaction with no status");
                    saveJobInSeparateTransaction(pp.jobId, getStatisticsForJob(pp.key), null, null);

                        try (InputStream in = payloadService.getPayloadAsInputStream(tmpPayload).get();
                             RecordExtractor extractorInstance = configuration.getRecordExtractorFactory().createNewExtractorFor(in)) {
                            Object record;
                            int count = 0;
                            do {
                                try {
                                    record = extractorInstance.getNextRecord();

                                    final PayloadExtractedRecord prg =
                                            new PayloadExtractedRecord(job, record, pp.key);

                                    if (record != null && executorService.isShutdown()) {
                                        // cancelled or server stopping: stop reading instead of rejecting every remaining record
                                        submissionStopped = true;
                                        break;
                                    }
                                    if (record != null) {
                                        //we have to duplicate the newWorkerFor call to avoid the variable mess of effectively final Runnables
                                        Runnable r;
                                        count++;
                                        if (parameters.isPreserveOldEditInfo()) {
                                            r = () -> {
                                                try {
                                                    auditConfig.disableAuditingFor(factory.newWorkerFor(prg, configuration, parameters, callback));
                                                }finally{
                                                    log.trace("saving job after one record because isPreserveOldEditInfo");
                                                    saveJobInSeparateTransaction(pp.jobId, pp.key);
                                                }
                                            };

                                        } else {
                                            r = ()->{
                                                try{
                                                    factory.newWorkerFor(prg, configuration, parameters, callback).run();
                                                }finally{
                                                    log.trace("saving job after one record because NOT isPreserveOldEditInfo");
                                                    saveJobInSeparateTransaction(pp.jobId, pp.key);
                                                }
                                            };
                                        }
                                        executorService.submit(() -> adminService.runAs(auth, r));

                                    }
                                } catch (RejectedExecutionException e) {
                                    // executor was shut down between the check above and submit()
                                    log.warn("Bulk load {} stopped accepting records: {}", pp.key, e.getMessage());
                                    submissionStopped = true;
                                    break;
                                } catch (Exception e) {
                                    Statistics stat = getStatisticsForJob(pp.key);
                                    stat.applyChange(Statistics.CHANGE.ADD_EX_BAD);
                                    storeStatisticsForJob(pp.key, stat);
                                    ExtractFailLogger.info("failed to extract record", e);
                                    log.warn("Error processing record");
                                    // hack to keep iterator going...
                                    record = new Object();
                                }
                            } while (record != null);
                            executorService.shutdown();
                        }catch (IOException e) {
                            e.printStackTrace();
                            job.status =ProcessingJob.Status.FAILED;
                            job.message = e.getMessage();
                            log.warn("IOExcepton processing record");
                            saveJobInSeparateTransaction(pp.jobId, getStatisticsForJob(pp.key), ProcessingJob.Status.FAILED,
                                    e.getMessage());
                            }
                }
                try {
                    executorService.awaitTermination(2, TimeUnit.DAYS);
                    executorServices.remove(pp.key);
                    if (shuttingDown) {
                        log.warn("Bulk load {} stopped because the server is shutting down", pp.key);
                        saveStoppedJobQuietly(pp.jobId, pp.key, "Stopped because the server was shutting down");
                        return;
                    }
                    // records persisted before a cancel are still reindexed so the search index matches the database
                    awaitAsyncIndexingQuiescence();
                    callback.reindexPersistedSubstances();
                    Statistics finalStats = getStatisticsForJob(pp.key);
                    boolean cancelled = submissionStopped || (finalStats != null && finalStats.cancelled);
                    if (cancelled) {
                        log.trace("about to save job as STOPPED (cancelled)");
                        saveJobInSeparateTransaction(pp.jobId, finalStats, ProcessingJob.Status.STOPPED, "Cancelled");
                    } else {
                        log.trace("about to save job as COMPLETE");
                        saveJobInSeparateTransaction(pp.jobId, finalStats, ProcessingJob.Status.COMPLETE, null);
                    }
                } catch (InterruptedException e) {
                    job.status =ProcessingJob.Status.STOPPED;
                    job.message="Interrupted";
                    log.trace("about to save job as Interrupted");
                    saveJobInSeparateTransaction(pp.jobId, getStatisticsForJob(pp.key), ProcessingJob.Status.STOPPED, "Interrupted");
                    e.printStackTrace();
                }
            }
        };

       new Thread(r).start();
        log.trace("after thread start");
        return pp;
    }

    public interface BulkLoadServiceCallback{
        void save(Object o);
        void updateJobIfNecessary(ProcessingJob job);
        void persistedSuccess(Object persistedRecord);
        void persistedFailure();

        void extractionSuccess();
        void extractionFailure();

        void processedSuccess();
        void processedFailure();

    }

    public class BulkLoadServiceCallBackImpl implements BulkLoadServiceCallback{

        private final String jobKey;
        private final Set<UUID> persistedSubstanceIds = ConcurrentHashMap.newKeySet();

        public BulkLoadServiceCallBackImpl(String jobKey) {
            this.jobKey = jobKey;
        }

        @Override
        public void save(Object o) {

        }

        @Override
        public void updateJobIfNecessary(ProcessingJob job) {
        	
        }
        
        @Override
        public void persistedSuccess(Object persistedRecord) {
            String persistedUuid = persistedUuid(persistedRecord);
            if (persistedUuid != null) {
                persistedSubstanceIds.add(UUID.fromString(persistedUuid));
            }
            applyStatisticsChangeForJob(jobKey, Statistics.CHANGE.ADD_PE_GOOD);
        }

        private String persistedUuid(Object persistedRecord) {
            if (persistedRecord instanceof JsonNode json) {
                JsonNode uuidNode = json.get("uuid");
                return uuidNode == null || uuidNode.isNull() ? null : uuidNode.asText();
            }
            if (persistedRecord instanceof com.fasterxml.jackson.databind.JsonNode json) {
                com.fasterxml.jackson.databind.JsonNode uuidNode = json.get("uuid");
                return uuidNode == null || uuidNode.isNull() ? null : uuidNode.asText();
            }
            return null;
        }

        @Override
        public void persistedFailure() {
            applyStatisticsChangeForJob(jobKey, Statistics.CHANGE.ADD_PE_BAD);
        }

        @Override
        public void extractionSuccess() {
            applyStatisticsChangeForJob(jobKey, Statistics.CHANGE.ADD_EX_GOOD);
        }

        @Override
        public void extractionFailure() {
            applyStatisticsChangeForJob(jobKey, Statistics.CHANGE.ADD_EX_BAD);
        }

        @Override
        public void processedSuccess() {
            applyStatisticsChangeForJob(jobKey, Statistics.CHANGE.ADD_PR_GOOD);
        }

        @Override
        public void processedFailure() {
            applyStatisticsChangeForJob(jobKey, Statistics.CHANGE.ADD_PR_BAD);
        }

        void reindexPersistedSubstances() {
            if (persistedSubstanceIds.isEmpty()) {
                return;
            }
            SubstanceRepository substanceRepository =
                    substanceRepositoryProvider == null ? null : substanceRepositoryProvider.getIfAvailable();
            if (substanceRepository == null) {
                log.warn("No SubstanceRepository available; skipping bulk load index reconciliation");
                return;
            }
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.setReadOnly(true);

            int reconciled = 0;
            for (UUID substanceId : persistedSubstanceIds) {
                try {
                    //the event has to be published while the transaction - and therefore the
                    //Hibernate session - is still open. Every ReindexEntityEvent listener is a plain
                    //synchronous @EventListener, so the indexers walk the substance graph inside this
                    //call. Publishing after the transaction closed would hand them a detached entity
                    //and lazy collections such as codes/names/references would throw
                    //LazyInitializationException.
                    Boolean published = tx.execute(status ->
                            substanceRepository.findById(substanceId)
                                    .map(substance -> {
                                        EntityUtils.EntityWrapper<?> wrapper =
                                                EntityUtils.EntityWrapper.of(substance);
                                        applicationEventPublisher.publishEvent(
                                                new ReindexEntityEvent(UUID.randomUUID(),
                                                        wrapper.getKey(), Optional.of(wrapper), true));
                                        return Boolean.TRUE;
                                    })
                                    .orElse(Boolean.FALSE));
                    if (!Boolean.TRUE.equals(published)) {
                        log.warn("Substance {} was reported as persisted but is not in the database; "
                                + "skipping index reconciliation", substanceId);
                        continue;
                    }
                    reconciled++;
                } catch (Exception e) {
                    log.warn("Unable to reconcile index for substance " + substanceId, e);
                }
            }
            log.info("Bulk load index reconciliation complete: {} of {} substances reindexed",
                    reconciled, persistedSubstanceIds.size());
        }
    }

    /**
     * Index create/update events are published asynchronously, so they can still be queued or
     * running after every bulk load worker has finished. Reconciling before they drain would let a
     * late create event append a second document for a substance that was just reconciled.
     * <p>
     * A create event indexes with a separate {@code remove} then {@code add} call, while an update
     * event performs an atomic delete-and-add under a per-key lock. Interleaving those for the same
     * substance is what leaves duplicate documents behind, so the final reconciliation has to wait
     * until no asynchronous indexing work remains.
     */
    void awaitAsyncIndexingQuiescence() {
        ThreadPoolExecutor pool = asyncIndexingPool();
        if (pool == null) {
            //we cannot observe the executor, so fall back to a fixed grace period
            sleepQuietly(INDEX_QUIESCENCE_POLL_MS * INDEX_QUIESCENCE_STABLE_SAMPLES);
            return;
        }
        long deadline = System.currentTimeMillis() + INDEX_QUIESCENCE_TIMEOUT_MS;
        int stableSamples = 0;
        while (System.currentTimeMillis() < deadline) {
            if (pool.getActiveCount() == 0 && pool.getQueue().isEmpty()) {
                if (++stableSamples >= INDEX_QUIESCENCE_STABLE_SAMPLES) {
                    return;
                }
            } else {
                stableSamples = 0;
            }
            if (!sleepQuietly(INDEX_QUIESCENCE_POLL_MS)) {
                return;
            }
        }
        log.warn("Timed out waiting for asynchronous indexing to settle; "
                + "index reconciliation may be incomplete");
    }

    private ThreadPoolExecutor asyncIndexingPool() {
        if (taskExecutor instanceof ThreadPoolTaskExecutor threadPoolTaskExecutor) {
            try {
                return threadPoolTaskExecutor.getThreadPoolExecutor();
            } catch (IllegalStateException e) {
                //executor has not been initialized
                return null;
            }
        }
        return null;
    }

    private boolean sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public Statistics getStatisticsForJob(String jobTerm){
        return jobCacheStatistics.get(jobTerm);
    }
    public Statistics getStatisticsForJob(ProcessingJob pj){
    	
    	String k=pj.getKeyMatching(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY);
        //the Map interface says we should be able to call get(null)
        //but Concurrent hashmap will throw a null pointer when
        //computing the hash value, at least in Java 7...
        if(k ==null){
            return null;
        }
        return jobCacheStatistics.get(k);
    }

    public Statistics storeStatisticsForJob(String jobTerm, Statistics s){

        return jobCacheStatistics.compute(jobTerm, (k, v) ->{
            if(v ==null || s.isNewer(v)){
                return s;
            }
            v.applyChange(s);
            return v;
        });

    }
    private void saveJobInSeparateTransaction(long jobId, String statKey){
        Statistics stat = getStatisticsForJob(statKey);
        if(stat !=null){
            saveJobInSeparateTransaction(jobId, stat, null, null);
        }
    }
    public void applyStatisticsChangeForJob(ProcessingJob job, Statistics.CHANGE change){
        Statistics stat = getStatisticsForJob(job);
        if(stat !=null){
            stat.applyChange(change);
        }
    }

    public Statistics applyStatisticsChangeForJob(String jobTerm, Statistics.CHANGE change){
        Statistics stat = getStatisticsForJob(jobTerm);
        if(stat !=null) {
            stat.applyChange(change);
        }
        return stat;
    }

    /*********************************************
     * Ginas bits for
     * 1. extracting from InputStream
     * 2. transforming to Substance
     * 3. persisting
     *
     * @author peryeata
     *
     */
    @Slf4j
    public static class GinasSubstancePersister extends RecordPersister<JsonNode, JsonNode> {

        @Autowired
        private XRefRepository xRefRepository;

        @Autowired
        private ProcessingRecordRepository processingRecordRepository;

        @Autowired
        private SubstanceEntityService substanceEntityService;

        @PersistenceContext
        private EntityManager entityManager;

//        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void persist(TransformedRecord<JsonNode, JsonNode> prec) throws Exception {
            //System.out.println("Persisting:" + prec.recordToPersist.uuid + "\t" + prec.recordToPersist.getName());

            try{
                boolean worked = false;
                List<String> errors = new ArrayList<>();
                if (prec.recordToPersist != null) {
                    try{
                        GsrsEntityService.CreationResult result = substanceEntityService.createEntity(prec.recordToPersist,true);
                        worked= result.isCreated();

                        Throwable t = result.getThrowable();
                        if(t !=null){
                            t.printStackTrace();
                            errors.add(t.getMessage());
                        }
                        if(!worked && errors.isEmpty()){
                            //must be validation error
                            List<ValidationMessage> messages = result.getValidationResponse().getValidationMessages();

                            errors.add(messages.stream().filter(vm->  vm.getMessageType() == ValidationMessage.MESSAGE_TYPE.ERROR)
                                                    .findFirst().get().getMessage());
                        }
                    }catch(Throwable t){
                        t.printStackTrace();
                        errors.add(t.getMessage());
                    }
                    if (worked) {
                        prec.rec.status = ProcessingRecord.Status.OK;
                        /*
                        we used to save the individual records separately but as of August 2022, we're
                        streamlining the saving process
                         */
                    } else {
                        prec.rec.message =  errors.get(0);
                        prec.rec.status = ProcessingRecord.Status.FAILED;
                    }
                    prec.rec.stop = TimeUtil.getCurrentTimeMillis();
                }
                //copy of rec to get the stats in a detached

                ProcessingRecord savedRecord = saveProcessingRecord(prec.rec);
                prec.rec.id = savedRecord.id;


                if (!worked){
                    
                    throw new IllegalStateException(prec.rec.message);
                }else{
                    log.debug("Saved substance " + (prec.recordToPersist != null ? prec.recordToPersist.get("uuid") : null)
                            + " record " + prec.rec.id);
                }
            }catch(Throwable t){
                log.debug("Fail saved substance " + (prec.recordToPersist != null ? prec.recordToPersist.get("uuid") : null)
                        + " record " + prec.rec.id);
                throw t;
            }
        }

        private ProcessingRecord saveProcessingRecord(ProcessingRecord record) {
            attachManagedJob(record);
            ProcessingRecord recordToSave = record;

            if (!entityManager.contains(record) && record.id != null) {
                recordToSave = processingRecordRepository.findById(record.id)
                        .map(managed -> copyProcessingRecordState(record, managed))
                        .orElse(record);
                attachManagedJob(recordToSave);
            }

            if (entityManager.contains(recordToSave)) {
                entityManager.flush();
                return recordToSave;
            }

            return processingRecordRepository.saveAndFlush(recordToSave);
        }

        private void attachManagedJob(ProcessingRecord record) {
            if (record.job != null && record.job.id != null && !entityManager.contains(record.job)) {
                record.job = entityManager.getReference(ProcessingJob.class, record.job.id);
            }
        }

        private ProcessingRecord copyProcessingRecordState(ProcessingRecord source, ProcessingRecord target) {
            target.start = source.start;
            target.stop = source.stop;
            target.name = source.name;
            target.status = source.status;
            target.message = source.message;
            target.xref = source.xref;
            target.job = source.job;

            if (source.properties != target.properties) {
                target.properties.clear();
                if (source.properties != null) {
                    target.properties.addAll(source.properties);
                }
            }

            return target;
        }


    }


    public static class GinasDumpExtractor extends GinasJSONExtractor {
        BufferedReader buff;


        private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("\t");

        public GinasDumpExtractor(InputStream is) {

            super(is);
            try {
                buff = new BufferedReader(new InputStreamReader(new GZIPInputStream(is), StandardCharsets.UTF_8));
            } catch (Exception e) {

            }

        }

        @Override
        public JsonNode getNextRecord() throws Exception{
            if (buff == null)
                return null;
            String line=null;

            while(true){
                try {
                    line = buff.readLine();
                    if (line == null) {
                        return null;
                    }
                    //trimmed line is separate in case trimming messes up the columns if there are blank cols
                    String trimmedLine = line.trim();
                    if(trimmedLine.isEmpty() || trimmedLine.startsWith("#")){
                        continue;
                    }
                    //use static pattern so we don't recompile on every split call
                    //which is what String.split() does
                    String[] toks = TOKEN_SPLIT_PATTERN.split(line);
                    if(toks ==null || toks.length < 3){
                        continue;
                    }

                    return mapper.readTree(toks[2]);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }

        @Override
        public void close() {
            try {
                if (buff != null)
                    buff.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    public static class GinasJSONExtractor extends RecordExtractor<JsonNode> {


        public GinasJSONExtractor(InputStream is) {
            super(is);
        }

        public GinasJSONExtractor(String s) throws UnsupportedEncodingException {
            this( new ByteArrayInputStream(s.getBytes("utf8")));
        }

        @Override
        public JsonNode getNextRecord() throws Exception{
            if (is == null)
                return null;

            try {
                JsonNode tree = mapper.readTree(is);
                is.close();
                is = null;
                return tree;
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            }
        }

        @Override
        public void close() {
            if(is ==null){
                return;
            }
            try{
                is.close();
            }catch(IOException e){
                e.printStackTrace();
                //ignore exception
            }
            is = null;
        }



    }
    @Slf4j
    public static class GinasSubstanceTransformer extends RecordTransformer<JsonNode, JsonNode> {
        public static GinasSubstanceTransformer INSTANCE = new GinasSubstanceTransformer();
        /**
         * This is the key for the old GSRS 2.x processor we keep it for backwards compatibility.
         */
        private static final String PROCESSING_PLUGIN_KEY = "ix.utils.Util.GinasRecordProcessorPlugin";
        private static final String DOC_TYPE_BATCH_IMPORT = "BATCH_IMPORT";


        /**
         * This method copied from GSRS 2.x Substance class that didn't belong in substance
         * @param p
         */
        private void addImportReference(JsonNode s, ProcessingJob p) {
            Reference r = new Reference();
            r.docType = DOC_TYPE_BATCH_IMPORT;
            r.citation = p.payload.name;
            r.documentDate = TimeUtil.getCurrentDate();
            String processingKey=p.getKeyMatching(PROCESSING_PLUGIN_KEY);
            r.id=processingKey;

            JsonNode references = s.get("references");
            if(references.isMissingNode()){
                ((ObjectNode)s).putArray("references").add(mapper.valueToTree(r));
            }
            if(references.isArray()){
                ((ArrayNode)references).add(mapper.valueToTree(r));
            }

        }
        @Override
        public JsonNode transform(PayloadExtractedRecord<JsonNode> pr, ProcessingRecord rec) {

            try {
                rec.name = getName(pr.theRecord);
            } catch (Exception e) {
                rec.name = "Nameless";
            }

            // System.out.println("############## transforming:" + rec.name);
            rec.job = pr.job;
            rec.start = System.currentTimeMillis();
            rec.status = ProcessingRecord.Status.ADAPTED;
            return pr.theRecord;
        }

        public String getName(JsonNode theRecord) {
            return theRecord.get("name").asText();
        }
    }

}
