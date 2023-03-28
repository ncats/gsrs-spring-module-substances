package gsrs.data_exchange.tasks;

import gsrs.data_exchange.SubstanceStagingAreaEntityService;
import gsrs.stagingarea.model.KeyValueMapping;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.repository.KeyValueMappingRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.core.utils.executor.ProcessListener;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

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
    SubstanceStagingAreaEntityService substanceStagingAreaEntityService = new SubstanceStagingAreaEntityService();

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

        substanceStagingAreaEntityService= AutowireHelper.getInstance().autowireAndProxy(substanceStagingAreaEntityService);

        TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
        log.trace("got tx " + tx);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        List<UUID>allIDs = substanceRepository.getAllIds();
        listen.totalRecordsToProcess(allIDs.size());
        tx.executeWithoutResult(
                a-> allIDs.stream().parallel().forEach( uuid -> {
                    log.trace("looking at substance " +uuid);
                    EntityUtils.Key substanceKey =  EntityUtils.Key.of(Substance.class, uuid);
                    Substance s = (Substance) EntityFetcher.of(substanceKey).getIfPossible().get();
                    List<MatchableKeyValueTuple> matchables = substanceStagingAreaEntityService.extractKVM(s);
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
                        //mapping.tidy();
                        keyValueMappingRepository.saveAndFlush(mapping);
                    });
                    listen.recordProcessed(s);
                }));
    }

    @Override
    public String getDescription() {
        return "Generate 'Matchables' (used in data import) for all substances in the permanent database";
    }
}
