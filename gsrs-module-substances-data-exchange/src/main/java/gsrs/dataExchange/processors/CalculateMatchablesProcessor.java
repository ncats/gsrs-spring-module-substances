package gsrs.dataexchange.processors;

import gsrs.dataexchange.SubstanceHoldingAreaEntityService;
import gsrs.dataexchange.services.ImportMetadataReindexer;
import gsrs.holdingarea.model.ImportMetadata;
import gsrs.holdingarea.model.KeyValueMapping;
import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.repository.ImportMetadataRepository;
import gsrs.holdingarea.repository.KeyValueMappingRepository;
import gsrs.repository.EditRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityProcessor;
import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static gsrs.dataexchange.tasks.CalculateMatchablesScheduledTask.DATA_SOURCE_MAIN;
import static gsrs.dataexchange.tasks.CalculateMatchablesScheduledTask.SUBSTANCE_CLASS;

@Slf4j
public class CalculateMatchablesProcessor implements EntityProcessor<Substance> {
    @Autowired
    private KeyValueMappingRepository keyValueMappingRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private EditRepository editRepository;

    @Autowired
    private ImportMetadataRepository importMetadataRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
        Optional<Integer> version = getVersion(substance);
        Optional<Substance> previousVersion = null;
        if (version.isPresent() && version.get() > 1) {
            log.trace("got version {}", version.get());
            TransactionTemplate txFetch = new TransactionTemplate(platformTransactionManager);
            txFetch.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            previousVersion = txFetch.execute(a -> {
                log.trace("going to look for edits for id {} and version {}.", substance.getUuid(), version.get()-2);
                List<Edit> editList = editRepository.findByRefidAndVersion(substance.getUuid().toString(), Integer.toString(version.get()-2));
                try {
                    log.trace("edit list: {}", (editList==null || editList.isEmpty()) ?"null/empty" : editList.size());
                    if (editList != null && !editList.isEmpty()) {
                        log.trace("editlist has data. using newvalue");
                        return Optional.of(SubstanceBuilder.from(editList.get(0).newValue).build());
                    }
                } catch (IOException e) {
                    log.error("Error deserializing Substance", e);
                    log.debug(editList.get(0).oldValue);
                }
                return Optional.empty();
            });
        } else {
            log.trace("no version");
        }


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

        List<UUID> importMetadataRecordsToReindex = new ArrayList<>();
        List<MatchableKeyValueTuple> matchables = substanceHoldingAreaEntityService.extractKVM(substance);
        log.trace("calculated matchables");
        processMatchables(substance, matchables, importMetadataRecordsToReindex, true);
        if(previousVersion.isPresent()){
            List<MatchableKeyValueTuple> matchablesForPrevious = substanceHoldingAreaEntityService.extractKVM(previousVersion.get());
            processMatchables(previousVersion.get(), matchablesForPrevious, importMetadataRecordsToReindex, false);
            log.trace("calculated matchables for previous (version {}; change reason {})", previousVersion.get().version,
                    previousVersion.get().changeReason);
        } else {
            log.trace("no previous!");
        }

        UUID indexingEventId= UUID.randomUUID();
        log.trace("going to iterate importMetadataRecordsToReindex ({})", importMetadataRecordsToReindex.size());
        importMetadataRecordsToReindex.forEach(r->{
            log.trace("going to index ImportMetadata with ID {}", r);
            if(r.equals(substance.getUuid())) {
                log.trace("object is a Substance!");
            } else {

                ImportMetadata metadata = importMetadataRepository.getOne(r);
                if(metadata!= null) {
                    log.trace("we have a real ImportMetadata object");
                    EntityUtils.EntityWrapper<ImportMetadata> wrappedObject = EntityUtils.EntityWrapper.of(metadata);
                    ImportMetadataReindexer.indexOneItem(indexingEventId, eventPublisher::publishEvent, EntityUtils.Key.of(wrappedObject),
                            EntityUtils.EntityWrapper.of(wrappedObject));
                } else {
                    log.trace("failed to retrieve ImportMetadata");
                }
            }
        });
    }

    private Optional<Integer> getVersion(Substance substance) {
        log.trace("getVersion");
        if (substance.version != null && substance.version.length() > 0) {
            return Optional.of(Integer.parseInt(substance.version));
        }
        return Optional.empty();
    }

    private void processMatchables(Substance substance, List<MatchableKeyValueTuple> matchableKeyValueTuples, List<UUID> recordIdsToProcess,
                                   boolean saveMatchables) {
        log.trace("processMatchables");

        TransactionTemplate txSaveMatchables = new TransactionTemplate(platformTransactionManager);
        txSaveMatchables.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        txSaveMatchables.executeWithoutResult(r -> {
            matchableKeyValueTuples.forEach(kv -> {
                log.trace("going to store KeyValueMapping with key {} and value '{}' qualifier: {}, instance id: {}; location: {}",
                        kv.getKey(), kv.getValue(), kv.getQualifier(), substance.uuid.toString(), DATA_SOURCE_MAIN);
                KeyValueMapping mapping = new KeyValueMapping();
                mapping.setKey(kv.getKey());
                String valueToStore = kv.getValue();
                if( valueToStore !=null && valueToStore.length()> KeyValueMapping.MAX_VALUE_LENGTH) {
                    valueToStore = valueToStore.substring(0, KeyValueMapping.MAX_VALUE_LENGTH-1);
                }
                mapping.setValue(valueToStore);
                mapping.setQualifier(kv.getQualifier());
                mapping.setRecordId(substance.uuid);
                mapping.setEntityClass(SUBSTANCE_CLASS);
                mapping.setDataLocation(DATA_SOURCE_MAIN);
                if(saveMatchables) {
                    keyValueMappingRepository.save(mapping);
                    log.trace("completed save");
                } else {
                    log.trace("skipped save");
                }
                //todo: delete previous values?
            /* the next line of code leads to errors such as
            2023-02-20 18:14:10.564  WARN 15044 --- [nio-8080-exec-1] ix.core.CombinedEntityProcessor          : Object of class [ix.ginas.models.v1.MixtureSubstance] with identifier [c3685a23-87a4-47de-8bb3-ab9a9cfe9606]: optimistic locking failed; nested exception is org.hibernate.StaleObjectStateException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect) : [ix.ginas.models.v1.MixtureSubstance#c3685a23-87a4-47de-8bb3-ab9a9cfe9606]
            org.springframework.orm.ObjectOptimisticLockingFailureException: Object of class [ix.ginas.models.v1.MixtureSubstance] with identifier [c3685a23-87a4-47de-8bb3-ab9a9cfe9606]: optimistic locking failed; nested exception is org.hibernate.StaleObjectStateException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect) : [ix.ginas.models.v1.MixtureSubstance#c3685a23-87a4-47de-8bb3-ab9a9cfe9606]
*/
                List<KeyValueMapping> matches = keyValueMappingRepository.findMappingsByKeyAndValueExcludingRecord(kv.getKey(), kv.getValue(),
                        substance.getUuid());
                matches.forEach(m -> {
                    log.trace("located match {} for k/v: {}/{}", m.getRecordId(), kv.getKey(), kv.getValue());
                    recordIdsToProcess.add(m.getRecordId());
                });
            });
        });

    }


}
