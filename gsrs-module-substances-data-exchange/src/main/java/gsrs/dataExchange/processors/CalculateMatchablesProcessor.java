package gsrs.dataexchange.processors;

import gsrs.dataexchange.SubstanceHoldingAreaEntityService;
import gsrs.holdingarea.model.KeyValueMapping;
import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.repository.KeyValueMappingRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static gsrs.dataexchange.tasks.CalculateMatchablesScheduledTask.DATA_SOURCE_MAIN;
import static gsrs.dataexchange.tasks.CalculateMatchablesScheduledTask.SUBSTANCE_CLASS;

@Slf4j
public class CalculateMatchablesProcessor implements EntityProcessor<Substance> {
    @Autowired
    private KeyValueMappingRepository keyValueMappingRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    SubstanceHoldingAreaEntityService substanceHoldingAreaEntityService = new SubstanceHoldingAreaEntityService();

    @Override
    public void prePersist(Substance obj) throws FailProcessingException {
        //EntityProcessor.super.prePersist(obj);
        log.trace("prePersist");
        recalculateMatchablesFor(obj);
    }

    @Override
    public void preRemove(Substance obj) throws FailProcessingException {
        log.trace("preRemove");
        keyValueMappingRepository.deleteByRecordId(obj.uuid);
    }

    @Override
    public void preUpdate(Substance obj) throws FailProcessingException {
        log.trace("preUpdate");
        //recalculateMatchablesFor(obj);
    }

    @Override
    public void postUpdate(Substance obj) throws FailProcessingException {
        log.trace("postUpdate");
        recalculateMatchablesFor(obj);
    }

    @Override
    public void initialize() throws FailProcessingException {
        //EntityProcessor.super.initialize();
    }

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    private void recalculateMatchablesFor(Substance substance) {
        log.trace("looking at substance " + substance.getUuid());
        substanceHoldingAreaEntityService = AutowireHelper.getInstance().autowireAndProxy(substanceHoldingAreaEntityService);
        //clear out the old stuff
        TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
        log.trace("got tx " + tx);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(
                a -> {
                    keyValueMappingRepository.deleteByRecordId(substance.uuid);
                }
        );
        log.trace("just completed call to deleteByRecordId");

        List<MatchableKeyValueTuple> matchables = substanceHoldingAreaEntityService.extractKVM(substance);
        log.trace("calculated matchables");
        matchables.forEach(kv -> {
            log.trace("going to store KeyValueMapping with key {} and value '{}' qualifier: {}, instance id: {}; location: {}",
                    kv.getKey(), kv.getValue(), kv.getQualifier(), substance.uuid.toString(), DATA_SOURCE_MAIN);
            KeyValueMapping mapping = new KeyValueMapping();
            mapping.setKey(kv.getKey());
            mapping.setValue(kv.getValue());
            mapping.setQualifier(kv.getQualifier());
            mapping.setRecordId(substance.uuid);
            mapping.setEntityClass(SUBSTANCE_CLASS);
            mapping.setDataLocation(DATA_SOURCE_MAIN);
            keyValueMappingRepository.save(mapping);
            log.trace("completed save");
        });
    }
}
