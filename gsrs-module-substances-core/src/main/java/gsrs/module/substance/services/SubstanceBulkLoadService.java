package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.AuditConfig;
import gsrs.DefaultDataSourceConfig;
import gsrs.module.substance.repository.ProcessingJobRepository;
import gsrs.module.substance.repository.ProcessingRecordRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.repository.XRefRepository;
import gsrs.repository.PayloadRepository;
import gsrs.security.AdminService;
import gsrs.security.hasAdminRole;
import gsrs.service.PayloadService;
import gsrs.validator.GsrsValidatorFactory;
import gsrs.validator.ValidatorConfig;
import ix.core.models.*;
import ix.core.processing.*;
import ix.core.stats.Estimate;
import ix.core.stats.Statistics;
import ix.core.util.EntityUtils;
import ix.core.util.FilteredPrintStream;
import ix.core.util.Filters;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidatorCategory;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.JsonSubstanceFactory;
import ix.ginas.utils.validation.ValidatorFactory;
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


    public static Logger getPersistFailureLogger(){
        return PersistFailLogger;
    }

    public static Logger getTransformFailureLogger(){
        return TransformFailLogger;
    }
    /**
     * Lock object to synchronize persistance calls
     * so only 1 object is persisted at a time.
     * Not using this causes problems with MySQL.
     */
    private final Object persistanceLock = new Object();

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

    private GsrsValidatorFactory validatorFactoryService;

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
            GsrsValidatorFactory validatorFactoryService,
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
        this.validatorFactoryService = validatorFactoryService;
        this.payloadRepository = payloadRepository;
        this.taskExecutor = taskExecutor;
    }

    public Statistics getStatisticsFor(String jobId){
        return getStatisticsForJob(jobId);
    }
    private ProcessingJob saveJobInSeparateTransaction(long jobId, Statistics stats){
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status->{
            ProcessingJob job = processingJobRepository.findById(jobId).get();
            if(!stats._isDone()){
                job.status = ProcessingJob.Status.RUNNING;

                job.message = "Loading data";
                job.status = ProcessingJob.Status.RUNNING;
            }else{
                job.status = ProcessingJob.Status.COMPLETE;
            }
            job.statistics = om.valueToTree(stats).toString();
            job.setIsAllDirty();
            return  processingJobRepository.saveAndFlush(job);
        });
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

    private void ensureAllAttached(Object o){
        EntityUtils.EntityWrapper.of(o).traverse().execute( (p,v)->{
            if(v !=null) {
                Object actual = v.getRawValue();
                if(actual !=null) {
                    if (!entityManager.contains(actual)) {
                        entityManager.merge(actual);
                    }
                }
            }
        });
    }
    @hasAdminRole
    public GinasRecordProcessorPlugin.PayloadProcessor submit(SubstanceBulkLoadParameters parameters) {
        // first see if this payload has already processed..


        final GinasRecordProcessorPlugin.PayloadProcessor pp = new GinasRecordProcessorPlugin.PayloadProcessor(parameters.getPayload());



        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        pp.jobId = tx.execute(status->{
            ProcessingJob job = new ProcessingJob();
            job.start = TimeUtil.getCurrentTimeMillis();
            job.addKeyword(new Keyword(GinasRecordProcessorPlugin.class.getName(), pp.key));

            job.status = ProcessingJob.Status.PENDING;

            job.message="Preparing payload for processing";
            job.payload = payloadRepository.findById(pp.payloadId).get();

            processingJobRepository.saveAndFlush(job);
            return job.id;
        });
        //owner now set automatically in created by?
//        job.owner= ((UserProfile) GsrsSecurityUtils.getCurrentUser()).user.;
//        saveJobInSeparateTransaction(job);
        storeStatisticsForJob(pp.key, new Statistics());


        final ExecutorService executorService = BlockingSubmitExecutor.newFixedThreadPool(3, MAX_EXTRACTION_QUEUE);

        final GinasRecordProcessorPlugin.PersistRecordWorkerFactory factory = configuration.getPersistRecordWorkerFactory(parameters);

        executorServices.put( pp.key, executorService);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Runnable r= new Runnable() {

            @Override
            public void run() {
                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                tx.executeWithoutResult(ignore->{
                ProcessingJob job = processingJobRepository.findById(pp.jobId).get();
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

                                    final GinasRecordProcessorPlugin.PayloadExtractedRecord prg = new GinasRecordProcessorPlugin.PayloadExtractedRecord(job, record);

                                    if (record != null) {
                                        //we have to duplicate the newWorkerFor call to avoid the variable mess of effectively final Runnables
                                        Runnable r;
                                        count++;
                                        if (parameters.isPreserveOldEditInfo()) {
                                            r = () -> auditConfig.disableAuditingFor(factory.newWorkerFor(prg, configuration, parameters, callback));

                                        } else {
                                            r = factory.newWorkerFor(prg, configuration, parameters, callback);
                                        }
                                        executorService.submit(() -> adminService.runAs(auth, r));

                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
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
                    Statistics stat = getStatisticsForJob(pp.key);
                    stat.applyChange(Statistics.CHANGE.MARK_EXTRACTION_DONE);
                    job.status =ProcessingJob.Status.COMPLETE;
                    job.message="";
                    saveJobInSeparateTransaction(pp.jobId, stat);
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
        String k=pj.getKeyMatching(GinasRecordProcessorPlugin.class.getName());
        //the Map interface says we should be able to call get(null)
        //but Concurrenthashmap will throw a null pointer when
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
    public Statistics applyStatisticsChangeForJob(ProcessingJob job, Statistics.CHANGE change){
        Statistics stat = getStatisticsForJob(job);
        if(stat !=null){
            stat.applyChange(change);
        }
        return stat;
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
    public static class GinasSubstancePersister extends RecordPersister<Substance, Substance> {
        private static ObjectMapper MAPPER = new ObjectMapper();
        @Autowired
        private XRefRepository xRefRepository;

        @Autowired
        private ProcessingRecordRepository processingRecordRepository;

        @Autowired
        private SubstanceRepository substanceRepository;

        @PersistenceContext
        private EntityManager entityManager;

//        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void persist(TransformedRecord<Substance, Substance> prec) throws Exception {
            //System.out.println("Persisting:" + prec.recordToPersist.uuid + "\t" + prec.recordToPersist.getName());

            try{
                boolean worked = false;
                List<String> errors = new ArrayList<String>();
                if (prec.recordToPersist != null) {
                    try{
                        substanceRepository.saveAndFlush(prec.recordToPersist);
                        worked= true;
                    }catch(Throwable t){
                        t.printStackTrace();
                        errors.add(t.getMessage());
                    }
                    if (worked) {
                        prec.rec.status = ProcessingRecord.Status.OK;
                        prec.rec.xref = new XRef(prec.recordToPersist);
                        //NOTE: the JPA mappings aren't set for cascade correctly? have to manually save both
                        xRefRepository.saveAndFlush(prec.rec.xref);
                    } else {
                        prec.rec.message = errors.get(0);
                        prec.rec.status = ProcessingRecord.Status.FAILED;
                    }
                    prec.rec.stop = System.currentTimeMillis();
                }
                //copy of rec to get the stats in a detached

                processingRecordRepository.saveAndFlush(entityManager.contains(prec.rec)? prec.rec : entityManager.merge(prec.rec));


                if (!worked){
                    throw new IllegalStateException(prec.rec.message);
                }else{
                    log.debug("Saved substance " + (prec.recordToPersist != null ? prec.recordToPersist.getUuid() : null)
                            + " record " + prec.rec.id);
                }
            }catch(Throwable t){
                log.debug("Fail saved substance " + (prec.recordToPersist != null ? prec.recordToPersist.getUuid() : null)
                        + " record " + prec.rec.id);
                throw t;
            }
        }


    }



    public static class GinasSubstanceTransformer extends GinasAbstractSubstanceTransformer<JsonNode> {


        public GinasSubstanceTransformer(ValidatorFactory validatorFactory) {
            super(validatorFactory);
        }

        @Override
        public String getName(JsonNode theRecord) {
            return theRecord.get("name").asText();
        }

        @Override
        public Substance transformSubstance(JsonNode rec) throws Throwable {
            return JsonSubstanceFactory.makeSubstance(rec);
        }
    }
//    /**
//     * This Extractor is for explicitly testing that failed validation
//     * records do fail.
//     *
//     * @author peryeata
//     *
//     */
//    public static class GinasAlwaysFailTestDumpExtractor extends GinasDumpExtractor {
//        public GinasAlwaysFailTestDumpExtractor(InputStream is) {
//            super(is);
//        }
//
//        @Override
//        public RecordTransformer getTransformer() {
//            return new RecordTransformer<JsonNode, Substance>(){
//                @Override
//                public Substance transform(PayloadExtractedRecord<JsonNode> pr, ProcessingRecord rec) {
//                    throw new IllegalStateException("Intentionally failed validation");
//                }
//
//            };
//        }
//
//    }

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
                    // Logger.debug("extracting:"+ toks[1]);
//				ByteArrayInputStream bis = new ByteArrayInputStream(toks[2].getBytes(StandardCharsets.UTF_8));
//
//				return mapper.readTree(bis);
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
    public abstract static class GinasAbstractSubstanceTransformer<K> extends RecordTransformer<K, Substance> {

        private static final String DOC_TYPE_BATCH_IMPORT = "BATCH_IMPORT";
        private final ValidatorFactory validatorFactory;

        public GinasAbstractSubstanceTransformer(ValidatorFactory validatorFactory) {
            this.validatorFactory = validatorFactory;
        }

        /**
         * This method copied from GSRS 2.x Substance class that didn't belong in substance
         * @param p
         */
        private void addImportReference(Substance s, ProcessingJob p) {
            Reference r = new Reference();
            r.docType = DOC_TYPE_BATCH_IMPORT;
            r.citation = p.payload.name;
            r.documentDate = TimeUtil.getCurrentDate();
            String processingKey=p.getKeyMatching(GinasRecordProcessorPlugin.class.getName());
            r.id=processingKey;
            s.addReference(r);
        }
        @Override
        public Substance transform(GinasRecordProcessorPlugin.PayloadExtractedRecord<K> pr, ProcessingRecord rec) {

            try {
                rec.name = getName(pr.theRecord);
            } catch (Exception e) {
                rec.name = "Nameless";
            }

            // System.out.println("############## transforming:" + rec.name);
            rec.job = pr.job;
            rec.start = System.currentTimeMillis();
            Substance sub = null;
            try {
                sub = transformSubstance(pr.theRecord);
                addImportReference(sub, rec.job);
                ValidationResponse resp = validatorFactory.createValidatorFor(sub, null, ValidatorConfig.METHOD_TYPE.BATCH, ValidatorCategory.CATEGORY_ALL())
                        .validate(sub, null);
                if(resp.hasError()){
                    throw new IllegalArgumentException("validation error: " + resp.getValidationMessages());
                }
                rec.status = ProcessingRecord.Status.ADAPTED;
            } catch (Throwable t) {
                rec.stop = System.currentTimeMillis();
                rec.status = ProcessingRecord.Status.FAILED;
                rec.message = t.getMessage();
                log.error(t.getMessage());
                t.printStackTrace();
                throw new IllegalStateException(t);
            }
            return sub;
        }

        public abstract String getName(K theRecord);

        public abstract Substance transformSubstance(K rec) throws Throwable;
    }

}
