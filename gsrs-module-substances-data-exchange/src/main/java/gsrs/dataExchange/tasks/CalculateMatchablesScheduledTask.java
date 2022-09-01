package gsrs.dataexchange.tasks;

import gsrs.dataexchange.SubstanceHoldingAreaEntityService;
import gsrs.holdingarea.model.KeyValueMapping;
import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.repository.KeyValueMappingRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.springUtils.AutowireHelper;
import ix.core.utils.executor.ProcessListener;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
public class CalculateMatchablesScheduledTask extends ScheduledTaskInitializer
{
    public static final String DATA_SOURCE_MAIN ="GSRS";
    public static final String SUBSTANCE_CLASS = Substance.class.getName();

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    KeyValueMappingRepository keyValueMappingRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    SubstanceHoldingAreaEntityService substanceHoldingAreaEntityService = new SubstanceHoldingAreaEntityService();

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        l.message("Initializing substance matchable processing");
        l.message("Clearing out old values");
        keyValueMappingRepository.deleteByDataLocation(DATA_SOURCE_MAIN);

        ProcessListener listen = ProcessListener.onCountChange((sofar, total) ->
        {
            if (total != null)
            {
                l.message("Recalculated:" + sofar + " of " + total);
            } else
            {
                l.message("Recalculated:" + sofar);
            }
        });

        listen.newProcess();

        substanceHoldingAreaEntityService= AutowireHelper.getInstance().autowireAndProxy(substanceHoldingAreaEntityService);

        TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
        log.trace("got tx " + tx);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(
                a-> substanceRepository.streamAll().forEach(s -> {
                    log.trace("looking at substance " + s.getUuid());
                    List<MatchableKeyValueTuple> matchables = substanceHoldingAreaEntityService.extractKVM(s);
                    matchables.forEach(kv -> {
                        log.trace("going to store KeyValueMapping with key {} and value '{}' qualifier: {}, instance id: {}; location: {}",
                                kv.getKey(), kv.getValue(), kv.getQualifier(), s.uuid.toString(), DATA_SOURCE_MAIN);
                        KeyValueMapping mapping = new KeyValueMapping();
                        mapping.setKey(kv.getKey());
                        mapping.setValue(kv.getValue());
                        mapping.setQualifier(kv.getQualifier());
                        mapping.setRecordId(s.uuid);
                        mapping.setEntityClass(SUBSTANCE_CLASS);
                        mapping.setDataLocation(DATA_SOURCE_MAIN);
                        keyValueMappingRepository.saveAndFlush(mapping);
                        //index for searching
                        //EntityUtils.EntityWrapper<KeyValueMapping> wrapper = EntityUtils.EntityWrapper.of(mapping);
        /*try {
            indexer.add(wrapper);
        } catch (IOException e) {
            log.error("Error indexing import metadata to index", e);
        }*/
                    });
                }));
    }

    @Override
    public String getDescription() {
        return "Generate 'Matchables' (used in data import) for all substances in the permanent database";
    }
}
