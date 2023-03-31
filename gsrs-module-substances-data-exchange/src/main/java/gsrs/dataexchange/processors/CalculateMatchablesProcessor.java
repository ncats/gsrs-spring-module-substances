package gsrs.dataexchange.processors;

import gsrs.dataexchange.SubstanceStagingAreaEntityService;
import gsrs.dataexchange.services.ImportMetadataReindexer;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.model.KeyValueMapping;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.repository.ImportMetadataRepository;
import gsrs.stagingarea.repository.KeyValueMappingRepository;
import gsrs.repository.EditRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityProcessor;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Consumer;

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

    SubstanceStagingAreaEntityService substanceStagingAreaEntityService = new SubstanceStagingAreaEntityService();

    @Override
    public void prePersist(Substance obj) {
        //EntityProcessor.super.prePersist(obj);
        log.trace("prePersist");
        recalculateMatchablesFor(obj);
    }

    @Override
    public void preRemove(Substance obj) {
        log.trace("preRemove");
        keyValueMappingRepository.deleteByRecordId(obj.uuid);
    }

    @Override
    public void preUpdate(Substance obj) {
        log.trace("preUpdate");
        //recalculateMatchablesFor(obj);
    }

    @Override
    public void postUpdate(Substance obj) {
        log.trace("postUpdate");
        recalculateMatchablesFor(obj);
    }

    @Override
    public void initialize()  {
        //EntityProcessor.super.initialize();
    }

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    private void recalculateMatchablesFor(Substance substance) {
        log.trace("looking at substance " + substance.getUuid());
        /*Optional<Integer> version = getVersion(substance);
        Optional<Substance> previousVersion = Optional.empty();
        if (version.isPresent() && version.get() > 1) {
            log.trace("got version {}", version.get());
            TransactionTemplate txFetch = new TransactionTemplate(platformTransactionManager);
            txFetch.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            previousVersion = txFetch.execute(a -> {
                log.trace("going to look for edits for id {} and version {}.", substance.getUuid(), version.get()-2);
                List<Edit> editList = editRepository.findByRefidAndVersion(substance.getUuid().toString(), Integer.toString(version.get()-2));
                //sort
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
*/
        substanceStagingAreaEntityService = AutowireHelper.getInstance().autowireAndProxy(substanceStagingAreaEntityService);
        //clear out the old stuff
        TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
        log.trace("got tx " + tx);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(
                a -> keyValueMappingRepository.deleteByRecordId(substance.uuid)
        );
        log.trace("just completed call to deleteByRecordId");

        Set<UUID> importMetadataRecordsToReindex = new HashSet<>();
        List<MatchableKeyValueTuple> matchables = substanceStagingAreaEntityService.extractKVM(substance);
        log.trace("calculated matchables");
        processMatchables(substance, matchables, importMetadataRecordsToReindex::add, true);
  /*      if(previousVersion.isPresent()){
            List<MatchableKeyValueTuple> matchablesForPrevious = substanceStagingAreaEntityService.extractKVM(previousVersion.get());
            processMatchables(previousVersion.get(), matchablesForPrevious, importMetadataRecordsToReindex::add, false);
            log.trace("calculated matchables for previous (version {}; change reason {})", previousVersion.get().version,
                    previousVersion.get().changeReason);
        } else {
            log.trace("no previous!");
        }
*/

        log.trace("going to iterate importMetadataRecordsToReindex ({})", importMetadataRecordsToReindex.size());
        importMetadataRecordsToReindex.forEach(r->{
            log.trace("going to index ImportMetadata with ID {}", r);
            if(r.equals(substance.getUuid())) {
                log.trace("object is a Substance!");
            } else {
                TransactionTemplate txSingleMetadata = new TransactionTemplate(platformTransactionManager);
                txSingleMetadata.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                txSingleMetadata.executeWithoutResult(m->{
                    Optional<ImportMetadata> metadata = importMetadataRepository.findById(r);
                    if(metadata.isPresent()) {
                        log.trace("we have a real ImportMetadata object within transaction, validations: {}, mappings: {}, publisher: {}",
                            metadata.get().getValidations().size(), metadata.get().getKeyValueMappings().size(), (eventPublisher==null));
                        EntityUtils.EntityWrapper<ImportMetadata> wrappedObject = EntityUtils.EntityWrapper.of(metadata.get());
                        UUID indexingEventId= UUID.randomUUID();
                        ImportMetadataReindexer.indexOneItem(indexingEventId, eventPublisher::publishEvent, EntityUtils.Key.of(wrappedObject),
                                wrappedObject);
                    } else {
                        log.trace("failed to retrieve ImportMetadata");
                    }
                });
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

    private void processMatchables(Substance substance, List<MatchableKeyValueTuple> matchableKeyValueTuples, Consumer<UUID> uuidConsumer,
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
                List<KeyValueMapping> matches = keyValueMappingRepository.findMappingsByKeyAndValueExcludingRecord(kv.getKey(), kv.getValue(),
                        substance.getUuid());
                matches.forEach(m -> {
                    log.trace("located match {} for k/v: {}/{}", m.getRecordId(), kv.getKey(), kv.getValue());
                    uuidConsumer.accept(m.getRecordId());
                });
            });
        });

    }


}
