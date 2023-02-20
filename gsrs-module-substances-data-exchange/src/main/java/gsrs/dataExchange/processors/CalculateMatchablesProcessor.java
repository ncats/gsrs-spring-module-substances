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

    SubstanceHoldingAreaEntityService substanceHoldingAreaEntityService = new SubstanceHoldingAreaEntityService();

    @Autowired
    private EditRepository editRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ImportMetadataRepository importMetadataRepository;

    @Override
    public void postPersist(Substance obj) {
        //EntityProcessor.super.prePersist(obj);
        log.trace("postPersist");
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
    public void initialize() {
        //EntityProcessor.super.initialize();
    }

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    private void recalculateMatchablesFor(Substance substance) {
        log.trace("recalculateMatchablesFor looking at substance " + substance.getUuid());
        Optional<Integer> version = getVersion(substance);
        Substance previousVersion = null;
        if( version.isPresent() && version.get()>1) {
            if( false) {
                List<Edit> editList = editRepository.findByRefidAndVersion(substance.getUuid().toString(), Integer.toString(version.get() - 1));
                try {
                    if (editList != null && !editList.isEmpty()) {
                        previousVersion = SubstanceBuilder.from(editList.get(0).oldValue).build();
                    }
                } catch (IOException e) {
                    log.error("Error deserializing Substance", e);
                    log.debug(editList.get(0).oldValue);
                }
            }
        }

        substanceHoldingAreaEntityService = AutowireHelper.getInstance().autowireAndProxy(substanceHoldingAreaEntityService);
        //clear out the old stuff
        TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
        log.trace("got tx " + tx);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(
                a -> keyValueMappingRepository.deleteByRecordId(substance.uuid)
        );
        log.trace("just completed call to deleteByRecordId");

        List<MatchableKeyValueTuple> matchables = substanceHoldingAreaEntityService.extractKVM(substance);

        List<UUID> importMetadataRecordsToRindex = new ArrayList<>();
        log.trace("calculated matchables");
        processMatchables(substance, matchables, importMetadataRecordsToRindex);
        if(previousVersion !=null){
            List<MatchableKeyValueTuple> matchablesForPrevious = substanceHoldingAreaEntityService.extractKVM(previousVersion);
            processMatchables(previousVersion, matchablesForPrevious, importMetadataRecordsToRindex);
        }
        UUID indexingEventId= UUID.randomUUID();
        importMetadataRecordsToRindex.forEach(r->{
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

    private void processMatchables(Substance substance, List<MatchableKeyValueTuple> matchableKeyValueTuples, List<UUID> recordIdsToProcess){
        log.trace("processMatchables");
        matchableKeyValueTuples.forEach(kv-> {
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
            //todo: delete previous values?
            /* the next line of code leads to errors such as
            2023-02-20 18:14:10.564  WARN 15044 --- [nio-8080-exec-1] ix.core.CombinedEntityProcessor          : Object of class [ix.ginas.models.v1.MixtureSubstance] with identifier [c3685a23-87a4-47de-8bb3-ab9a9cfe9606]: optimistic locking failed; nested exception is org.hibernate.StaleObjectStateException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect) : [ix.ginas.models.v1.MixtureSubstance#c3685a23-87a4-47de-8bb3-ab9a9cfe9606]
            org.springframework.orm.ObjectOptimisticLockingFailureException: Object of class [ix.ginas.models.v1.MixtureSubstance] with identifier [c3685a23-87a4-47de-8bb3-ab9a9cfe9606]: optimistic locking failed; nested exception is org.hibernate.StaleObjectStateException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect) : [ix.ginas.models.v1.MixtureSubstance#c3685a23-87a4-47de-8bb3-ab9a9cfe9606]

            List<KeyValueMapping> matches = keyValueMappingRepository.findMappingsByKeyAndValueExcludingRecord(kv.getKey(), kv.getValue(),
                    substance.getUuid());
            matches.forEach(m -> {
                log.trace("located match {} for k/v: {}/{}", m.getRecordId(), kv.getKey(), kv.getValue());
                recordIdsToProcess.add(m.getRecordId());
            });*/
        });
     }

    private Optional<Integer> getVersion(Substance substance){
        log.trace("getVersion");
        if( substance.version !=null && substance.version.length()>0) {
            return Optional.of(Integer.parseInt(substance.version));
        }
        return Optional.empty();
    }
}
