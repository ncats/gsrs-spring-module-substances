package ix.core.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.services.GinasSubstanceTransformerFactory;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Payload;
import ix.core.models.ProcessingJob;
import ix.core.models.ProcessingRecord;
import ix.core.stats.Statistics;
import ix.utils.Util;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;


public class GinasRecordProcessorPlugin{
    private static final int AKKA_TIMEOUT = 60000;
    
//    
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
    
        

    private Set<ExecutorService> executorServices = new HashSet<>();


    static final Random rand = new Random();
        
    private static GinasRecordProcessorPlugin _instance;
        
    public static GinasRecordProcessorPlugin getInstance(){
        return _instance;
    }

    static String randomKey(int size) {
        byte[] b = new byte[size];
        rand.nextBytes(b);
        return Util.toHex(b);
    }

    public static class PayloadProcessor implements Serializable {
        public final Payload payload;
        public final String id;
        public final String key;
        public Long jobId;
                
        public PayloadProcessor(Payload payload) {
            Objects.requireNonNull(payload);
            this.payload = payload;
            this.key = randomKey(10);
            this.id = payload.id + ":" + this.key;
                        
        }
    }

    
        
    public static class PayloadExtractedRecord<K> implements Serializable {
        public final ProcessingJob job;
        public final K theRecord;
        public final String jobKey;
                
        public PayloadExtractedRecord(ProcessingJob job, K rec) {
            this.job = job;
            this.theRecord = rec;
            String k=job.getKeyMatching(GinasRecordProcessorPlugin.class.getName());
            jobKey=k;
        }
    }


    //    public static void updateJobIfNecessary(ProcessingJob job2) {
//    	ProcessingJob job;
//    	try{
//    		job = ProcessingJobFactory.getJob(job2.id);
//    	}catch(Exception e){
//    		Logger.debug("Error refreshing job from database, using local copy");
//            job = job2;
//    	}
//
//    	Statistics stat = getStatisticsForJob(job);
//        if (stat != null && stat._isDone()) {
//            updateJob(job,stat);
//        }
//    }
//    public static void updateJob(ProcessingJob job,Statistics stat) {
//
//        job.stop = TimeUtil.getCurrentTimeMillis();
//        job.status = ProcessingJob.Status.COMPLETE;
//        job.statistics = om.valueToTree(stat).toString();
//        job.message="Job complete";
//
//        job.update();
//    }

        




//    public GinasRecordProcessorPlugin(Application app) {
//        this.app = app;
//    }
//
//    @Override
//    public void onStart() {
//
//        //GSRS 2.x sets this to 2 in conf
//        MAX_EXTRACTION_QUEUE=app.configuration()
//                .getInt("ix.ginas.maxrecordqueue", MAX_EXTRACTION_QUEUE);
//
//
//
//
//        ctx = app.plugin(IxContext.class);
//        if (ctx == null)
//            throw new IllegalStateException("IxContext plugin is not loaded!");
//
//
//
//
//
//
//        long start= System.currentTimeMillis();
//
//        _instance=this;
//    }

//    @Override
//    public void onStop() {
//        //TODO shutdownNow() which will kill currently running threads??
//        for(ExecutorService s : executorServices){
//            s.shutdownNow();
//        }
//        executorServices.clear();
//      //  executorService.shutdown();
//        Logger.info("Plugin " + getClass().getName() + " stopped!");
//    }

    public boolean enabled() {
        return true;
    }


//    public PayloadProcessor submit(final Payload payload, Class<?> extractor, Class<?> persister) {
//        // first see if this payload has already processed..
//
//
//        final PayloadProcessor pp = new PayloadProcessor(payload);
//
//
//        final ProcessingJob job = new ProcessingJob();
//        job.start = TimeUtil.getCurrentTimeMillis();
//        job.addKeyword(new Keyword(GinasRecordProcessorPlugin.class.getName(), pp.key));
//        job.setExtractor(extractor);
//        job.setPersister(persister);
//
//        job.status = ProcessingJob.Status.PENDING;
//        job.payload = pp.payload;
//        job.message="Preparing payload for processing";
//        job.owner=UserFetcher.getActingUser();
//        job.save();
//        storeStatisticsForJob(pp.key, new Statistics());
//        pp.jobId=job.id;
//
//        final ExecutorService executorService = BlockingSubmitExecutor.newFixedThreadPool(3, MAX_EXTRACTION_QUEUE);
//
//        final PersistRecordWorkerFactory factory = getPersistRecordWorkerFactory();
//
//        executorServices.add(executorService);
//        Runnable r= new Runnable() {
//            @Override
//            public void run() {
//                FilteredPrintStream.Filter filterOutJChem = Filters.filterOutClasses(Pattern.compile("chemaxon\\..*|lychi\\..*"));
//
//                //katzelda 6/2019: IDE says we don't ever use the FilterSessions but we do it's just a sideeffect that gets used when we
//                //invoke the code inside the try and gets popped when we close so the de-sugared code of the try-with resources does use it
//                //so keep it !!
//                try (FilteredPrintStream.FilterSession ignoreChemAxonSTDOUT = ConsoleFilterPlugin.getStdOutOutputFilter().newFilter(filterOutJChem);
//                     FilteredPrintStream.FilterSession ignoreChemAxonSTDERR = ConsoleFilterPlugin.getStdErrOutputFilter().newFilter(filterOutJChem);
//
//                     RecordExtractor extractorInstance = job.getExtractor().makeNewExtractor(payload)) {
//
//                    Estimate es = extractorInstance.estimateRecordCount(pp.payload);
//                    {
//                        Logger.debug("Counted records");
//                        Statistics stat = getStatisticsForJob(pp.key);
//                        if (stat == null) {
//                            stat = new Statistics();
//                        }
//                        stat.totalRecords = es;
//                        stat.applyChange(Statistics.CHANGE.EXPLICIT_CHANGE);
//                        storeStatisticsForJob(pp.key, stat);
//                        Logger.debug(stat.toString());
//                    }
//                    job.status = ProcessingJob.Status.RUNNING;
//                    job.payload = pp.payload;
//                    job.message = "Loading data";
//                    job.save();
//
//                    Object record;
//                    do {
//                        try {
//                            record = extractorInstance.getNextRecord();
//                            final PayloadExtractedRecord prg = new PayloadExtractedRecord(job, record);
//
//                            if (record != null) {
//
//                                executorService.submit(factory.newWorkerFor(prg));
//
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            Statistics stat = getStatisticsForJob(pp.key);
//                            stat.applyChange(Statistics.CHANGE.ADD_EX_BAD);
//                            storeStatisticsForJob(pp.key, stat);
//                            Global.ExtractFailLogger
//                                    .info("failed to extract record", e);
//                            // hack to keep iterator going...
//                            record = new Object();
//                        }
//                    } while (record != null);
//                    executorService.shutdown();
//
//                }
//                try {
//                    executorService.awaitTermination(2, TimeUnit.DAYS);
//                    Statistics stat = getStatisticsForJob(pp.key);
//                    stat.applyChange(Statistics.CHANGE.MARK_EXTRACTION_DONE);
//                    executorServices.remove(executorService);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//
//            }
//        };
//
//        ForkJoinPool.commonPool().submit(r);
//
//
//        return pp;
//    }




    public static abstract class PersistRecordWorker implements Runnable{

        private final PayloadExtractedRecord prg;
        private final SubstanceBulkLoadService.BulkLoadServiceCallback callback;
        private final GinasSubstanceTransformerFactory transformerFactory;

        public PersistRecordWorker(PayloadExtractedRecord prg,
                                   GinasSubstanceTransformerFactory transformerFactory,
                                   SubstanceBulkLoadService.BulkLoadServiceCallback callback){

            this.prg = Objects.requireNonNull(prg);
            this.transformerFactory = Objects.requireNonNull(transformerFactory);

            this.callback = Objects.requireNonNull(callback);
        }
        @Override
        public void run() {
        	TransformedRecord tr=null;
        	ProcessingRecord rec = new ProcessingRecord();
            try{
	            callback.extractionSuccess();
	            
	            Object trans = transformerFactory.createTransformerFor().transform(prg, rec);
	
	            if (trans == null) {
	                throw new IllegalStateException("Transform error");
	            }
                callback.processedSuccess();
	            tr= AutowireHelper.getInstance().autowireAndProxy(new TransformedRecord(trans, prg.theRecord, rec, callback));
	           
        	}catch(Throwable t){
                callback.processedFailure();
        		//t.printStackTrace();
        		
        		SubstanceBulkLoadService.getTransformFailureLogger().info(rec.name + "\t" + t.getMessage().replace("\n", "") + "\t"
						+ prg.theRecord.toString().replace("\n", ""));

        	}
        	if(tr!=null){
        		doPersist(tr);
        	}
        }

        protected abstract void doPersist(TransformedRecord tr);
    }
    
    
    public interface PersistRecordWorkerFactory{
        PersistRecordWorker newWorkerFor(PayloadExtractedRecord prg,
                                         GinasSubstanceTransformerFactory transformerFactory,
                                         SubstanceBulkLoadService.SubstanceBulkLoadParameters parameters,
                                         SubstanceBulkLoadService.BulkLoadServiceCallback callback);
    }
    


}
