package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.AuditConfig;
import gsrs.DefaultDataSourceConfig;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.repository.ProcessingJobRepository;
import gsrs.module.substance.repository.ProcessingRecordRepository;
import gsrs.module.substance.repository.XRefRepository;
import gsrs.repository.PayloadRepository;
import gsrs.security.AdminService;
import gsrs.security.hasAdminRole;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import ix.core.models.*;
import ix.core.processing.*;
import ix.core.stats.Estimate;
import ix.core.stats.Statistics;
import ix.core.util.EntityUtils;
import ix.core.util.FilteredPrintStream;
import ix.core.util.Filters;
import ix.core.validator.ValidationMessage;
import ix.ginas.models.v1.Reference;
import ix.ginas.utils.JsonSubstanceFactory;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

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

    private final static int NUMBER_OF_LOADING_THREADS=1;

    public static Logger getPersistFailureLogger(){
        return PersistFailLogger;
    }

    public static Logger getTransformFailureLogger(){
        return TransformFailLogger;
    }

    private final Object jobLock = new Object();

    private static final String KEY_PROCESS_QUEUE_SIZE = "PROCESS_QUEUE_SIZE";
    //Hack variable for resisting buildup
    //of extracted records not yet transformed
    private static Map<String,Long> queueStatistics = new ConcurrentHashMap<String,Long>();
    private static Map<String,Statistics> jobCacheStatistics = new ConcurrentHashMap<>();

    private static ObjectMapper om = new ObjectMapper();

    private static int MAX_EXTRACTION_QUEUE = 100;

    private SubstanceBulkLoadServiceConfiguration configuration;

    private Map<String, ExecutorService> executorServices = new ConcurrentHashMap<>();

    private ProcessingJobRepository processingJobRepository;

    private PlatformTransactionManager transactionManager;

    private ConsoleFilterService consoleFilterService;

    private TaskExecutor taskExecutor;

    private PayloadService payloadService;

    private AdminService adminService;

    private AuditConfig auditConfig;

    private PayloadRepository payloadRepository;


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
            TaskExecutor taskExecutor
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
    }

    public Statistics getStatisticsFor(String jobId){
        return getStatisticsForJob(jobId);
    }

    private ProcessingJob saveJobInSeparateTransaction(long jobId, Statistics stats){
        synchronized (jobLock) {
            if(stats==null ) {
                log.info("skipping save because stats is null");
                return null;
            }
            if(!stats._isDone()) {
                log.info("skipping save of job in process");
                return null;
            }
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return tx.execute(status -> saveJobInCurrentTransaction(jobId, stats));
        }
    }

    private ProcessingJob saveJobInCurrentTransaction(long jobId, Statistics stats) {
        ProcessingJob job = processingJobRepository.findById(jobId).get();
        if (!stats._isDone()) {

            job.message = "Loading data";
            job.status = ProcessingJob.Status.RUNNING;
        } else {
            job.status = ProcessingJob.Status.COMPLETE;
        }
        job.statistics = om.valueToTree(stats).toString();

        job.setIsAllDirty();
        return processingJobRepository.saveAndFlush(job);
    }

    @PreDestroy
    public void onStop() {

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
    @hasAdminRole
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
    @hasAdminRole
    public PayloadProcessor submit(SubstanceBulkLoadParameters parameters) {
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


        final ExecutorService executorService = BlockingSubmitExecutor.newFixedThreadPool(NUMBER_OF_LOADING_THREADS, MAX_EXTRACTION_QUEUE);

        final PersistRecordWorkerFactory factory = configuration.getPersistRecordWorkerFactory(parameters);

        executorServices.put( pp.key, executorService);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Runnable r= new Runnable() {

            @Override
            public void run() {
                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                tx.executeWithoutResult(ignore-> {
                    TransactionTemplate tx2 = new TransactionTemplate(transactionManager);
                    tx2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    ProcessingJob job = tx2.execute(s -> {
                        ProcessingJob innerJob=processingJobRepository.findById(pp.jobId).get();
                        EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(innerJob);
                        log.trace("JSON of Job retrieved: {}", wrapper.toInternalJson());
                        return innerJob;
                    });

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
                        e.printStackTrace();
                        //error figuring out estimate?
                    }

                    saveJobInSeparateTransaction(pp.jobId, getStatisticsForJob(pp.key));

                    BulkLoadServiceCallback callback = new BulkLoadServiceCallBackImpl(job);
                   

                        try (InputStream in = payloadService.getPayloadAsInputStream(tmpPayload).get();
                             RecordExtractor extractorInstance = configuration.getRecordExtractorFactory().createNewExtractorFor(in)) {
                            Object record;
                            int count = 0;
                            do {
                                try {
                                    record = extractorInstance.getNextRecord();

                                    final PayloadExtractedRecord prg = new PayloadExtractedRecord(job, record);

                                    if (record != null) {
                                        //we have to duplicate the newWorkerFor call to avoid the variable mess of effectively final Runnables
                                        Runnable r;
                                        count++;
                                        if (parameters.isPreserveOldEditInfo()) {
                                            r = () -> {
                                                try {
                                                    auditConfig.disableAuditingFor(factory.newWorkerFor(prg, configuration, parameters, callback));
                                                }finally{
                                                    saveJobInSeparateTransaction(pp.jobId, pp.key);
                                                }
                                            };

                                        } else {
                                            r = ()->{
                                                try{
                                                    factory.newWorkerFor(prg, configuration, parameters, callback).run();
                                                }finally{
                                                    saveJobInSeparateTransaction(pp.jobId, pp.key);
                                                }
                                            };
                                        }
                                        executorService.submit(() -> adminService.runAs(auth, r));

                                    }
                                } catch (Exception e) {
                                    Statistics stat = getStatisticsForJob(pp.key);
                                    stat.applyChange(Statistics.CHANGE.ADD_EX_BAD);
                                    storeStatisticsForJob(pp.key, stat);
                                    ExtractFailLogger.info("failed to extract record", e);
                                    // hack to keep iterator going...
                                    record = new Object();
                                }
                            } while (record != null);
                            executorService.shutdown();
                        }catch (IOException e) {
                            e.printStackTrace();
                            job.status =ProcessingJob.Status.FAILED;
                            job.message = e.getMessage();
                            saveJobInSeparateTransaction(pp.jobId, getStatisticsForJob(pp.key));
                            }
                }
                try {
                    executorService.awaitTermination(2, TimeUnit.DAYS);
                    executorServices.remove(pp.key);
                } catch (InterruptedException e) {
                    job.status =ProcessingJob.Status.STOPPED;
                    job.message="Interrupted";
                    saveJobInSeparateTransaction(pp.jobId, getStatisticsForJob(pp.key));
                    e.printStackTrace();
                }
                });

            }
        };

       new Thread(r).start();


        return pp;
    }

    public interface BulkLoadServiceCallback{
        void save(Object o);
        void updateJobIfNecessary(ProcessingJob job);
        void persistedSuccess();
        void persistedFailure();

        void extractionSuccess();
        void extractionFailure();

        void processedSuccess();
        void processedFailure();

    }

    public class BulkLoadServiceCallBackImpl implements BulkLoadServiceCallback{

        private final ProcessingJob job;

        public BulkLoadServiceCallBackImpl(ProcessingJob job) {
            this.job = job;
        }

        @Override
        public void save(Object o) {

        }

        @Override
        public void updateJobIfNecessary(ProcessingJob job) {

        }

        @Override
        public void persistedSuccess() {
            applyStatisticsChangeForJob(job, Statistics.CHANGE.ADD_PE_GOOD);
        }

        @Override
        public void persistedFailure() {
            applyStatisticsChangeForJob(job, Statistics.CHANGE.ADD_PE_BAD);
        }

        @Override
        public void extractionSuccess() {
            applyStatisticsChangeForJob(job, Statistics.CHANGE.ADD_EX_GOOD);
        }

        @Override
        public void extractionFailure() {
            applyStatisticsChangeForJob(job, Statistics.CHANGE.ADD_EX_BAD);
        }

        @Override
        public void processedSuccess() {
            applyStatisticsChangeForJob(job, Statistics.CHANGE.ADD_PR_GOOD);
        }

        @Override
        public void processedFailure() {
            applyStatisticsChangeForJob(job, Statistics.CHANGE.ADD_PR_BAD);
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
            saveJobInSeparateTransaction(jobId, stat);
        }
    }
    public void applyStatisticsChangeForJob(ProcessingJob job, Statistics.CHANGE change){
        Statistics stat = getStatisticsForJob(job);
        if(stat !=null){
            stat.applyChange(change);
        }
        saveJobInSeparateTransaction(job.id, stat);
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

                processingRecordRepository.saveAndFlush(entityManager.contains(prec.rec)? prec.rec : entityManager.merge(prec.rec));


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
            ObjectMapper mapper = new ObjectMapper();
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
                    if(toks ==null || toks.length <2){
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
                ObjectMapper mapper = new ObjectMapper();
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

        private ObjectMapper mapper = new ObjectMapper();

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
