package gsrs.dataexchange.tasks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

import gsrs.GsrsFactoryConfiguration;
import gsrs.dataexchange.SubstanceStagingAreaEntityService;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.springUtils.AutowireHelper;
import gsrs.stagingarea.model.KeyValueMapping;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.repository.KeyValueMappingRepository;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.core.utils.executor.ProcessListener;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CalculateMatchablesScheduledTask extends ScheduledTaskInitializer{
    public static final String DATA_SOURCE_MAIN ="GSRS";
    public static final String SUBSTANCE_CLASS = Substance.class.getName();

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    KeyValueMappingRepository keyValueMappingRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    SubstanceStagingAreaEntityService substanceStagingAreaEntityService = new SubstanceStagingAreaEntityService();

    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;
    
    
    private Integer threadCount = -1;
    
    
    @JsonProperty("threadCount")
    public void setThreadCount(Integer count) {
    	threadCount=count;
    }

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        l.message("Initializing substance matchable processing");
        l.message("Clearing out old values");
        keyValueMappingRepository.deleteByDataLocation(DATA_SOURCE_MAIN);

        ProcessListener listen = ProcessListener.onCountChange((sofar, total) ->{
            if (total != null)
            {
                l.message("Recalculated:" + sofar + " of " + total);
            } else
            {
                l.message("Recalculated:" + sofar);
            }
        });

        listen.newProcess();

        substanceStagingAreaEntityService.setGsrsFactoryConfiguration(this.gsrsFactoryConfiguration);
        try {
            substanceStagingAreaEntityService = AutowireHelper.getInstance().autowireAndProxy(substanceStagingAreaEntityService);
        }catch (Exception ignore){

        }

        List<UUID> allIDs = substanceRepository.getAllIds();
        listen.totalRecordsToProcess(allIDs.size());
        
        
        
        final int parallelism = 4;
        ForkJoinPool forkJoinPool = null;
        try {
        	Runnable r = () ->{
            	allIDs.parallelStream().forEach( uuid -> {
                	try {
                        log.trace("looking at substance " + uuid);
                        EntityUtils.Key substanceKey = EntityUtils.Key.of(Substance.class, uuid);
                        Optional<?> retrieved =EntityFetcher.of(substanceKey).getIfPossible();
                        if(retrieved.isPresent()) {
                            Substance s = (Substance) retrieved.get();
                            List<MatchableKeyValueTuple> matchables = substanceStagingAreaEntityService.extractKVM(s);
                            List<KeyValueMapping> kvmaps = matchables.stream().map(kv -> {
                                log.trace("going to store KeyValueMapping with key {} and value '{}' qualifier: {}, instance id: {}; location: {}",
                                        kv.getKey(), kv.getValue(), kv.getQualifier(), s.uuid.toString(), DATA_SOURCE_MAIN);
                                KeyValueMapping mapping = new KeyValueMapping();
                                mapping.setKey(kv.getKey());
                                mapping.setValue(kv.getValue());
                                mapping.setQualifier(kv.getQualifier());
                                mapping.setRecordId(s.uuid);
                                mapping.setEntityClass(SUBSTANCE_CLASS);
                                mapping.setDataLocation(DATA_SOURCE_MAIN);
                                return mapping;
                            })
                            .collect(Collectors.toList());

                            //mapping.tidy();
                            TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                            log.trace("got tx " + tx);
                            tx.executeWithoutResult(a->{
                            	keyValueMappingRepository.saveAll(kvmaps);
                            	keyValueMappingRepository.flush();
                            });
                            
                            listen.recordProcessed(s);
                        } else {
                            log.warn("error retrieving substance with ID {}", uuid);
                        }
                	} catch (IllegalStateException ignore){
                        log.warn("error processing record {}; continuing to next.", uuid);
                    }
                });
            	
            };
        	
        	if(threadCount!=null && threadCount>0) {
        		forkJoinPool = new ForkJoinPool(threadCount);
        		forkJoinPool.submit(r).get();
        	}else {
        		//will use common fork join pool
        		r.run();
        	}
            
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            if (forkJoinPool != null) {
                forkJoinPool.shutdown();
            }
        }
        
        
    }

    @Override
    public String getDescription() {
        return "Generate 'Matchables' (used in data import) for all substances in the permanent database";
    }
}
