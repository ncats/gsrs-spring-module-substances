package gsrs.module.substance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.sneak.Sneak;
import gsrs.EntityPersistAdapter;
import gsrs.controller.IdHelpers;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.json.JsonEntityUtil;
import gsrs.module.substance.events.SubstanceCreatedEvent;
import gsrs.module.substance.events.SubstanceUpdatedEvent;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.services.SubstanceBulkLoadServiceConfiguration;
import gsrs.service.AbstractGsrsEntityService;
import gsrs.validator.ValidatorConfig;
import ix.core.EntityFetcher;
import ix.core.models.ForceUpdatableModel;
import ix.core.util.EntityUtils;
import ix.core.util.LogUtil;
import ix.core.validator.*;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.JsonSubstanceFactory;
import ix.ginas.utils.validation.strategy.BatchProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.utils.Util;
import ix.utils.pojopatch.PojoDiff;
import ix.utils.pojopatch.PojoPatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Service
@Slf4j
public class SubstanceEntityServiceImpl extends AbstractGsrsEntityService<Substance, UUID> implements SubstanceEntityService {
    public static final String  CONTEXT = "substances";


    public SubstanceEntityServiceImpl() {
        super(CONTEXT,  IdHelpers.UUID, "gsrs_exchange", "substance.created", "substance.updated");
    }

    @Autowired
    private SubstanceRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory;

    @Autowired
    private SubstanceBulkLoadServiceConfiguration bulkLoadServiceConfiguration;

    @Autowired
    private EntityPersistAdapter entityPersistAdapter;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    @Override
    public UUID parseIdFromString(String idAsString) {
        return UUID.fromString(idAsString);
    }



    protected GsrsProcessingStrategy createProcessingStrategyFor(ValidatorConfig.METHOD_TYPE type){
        if(type == ValidatorConfig.METHOD_TYPE.BATCH){
            return new BatchProcessingStrategy(gsrsProcessingStrategyFactory.createNewStrategy(bulkLoadServiceConfiguration.getBatchProcessingStrategy()));
        }
        return gsrsProcessingStrategyFactory.createNewDefaultStrategy();
    }
    @Override
    protected <T> ValidatorCallback createCallbackFor(T object, ValidationResponse<T> response, ValidatorConfig.METHOD_TYPE type) {

        GsrsProcessingStrategy strategy = createProcessingStrategyFor(type);
        ValidationResponseBuilder<T> builder = new ValidationResponseBuilder<T>(object, response, strategy){
            @Override
            public void complete() {
                if(object instanceof Substance) {
                    ValidationResponse<T> resp = buildResponse();

                    List<GinasProcessingMessage> messages = resp.getValidationMessages()
                            .stream()
                            .filter(m -> m instanceof GinasProcessingMessage)
                            .map(m -> (GinasProcessingMessage) m)
                            .collect(Collectors.toList());
                    //processMessage, handleMessages, addProblems?
                    //Why all 3? because right now each of these methods might set or change fields in validation response.
                    messages.stream().forEach(strategy::processMessage);
                    resp.setValid(false);
                    if (strategy.handleMessages((Substance) object, messages)) {
                        resp.setValid(true);
                    }
                    strategy.addProblems((Substance) object, messages);

                    strategy.setIfValid(resp, messages);
                }
            }
        };
        return builder;
    }

    @Override
    protected Substance fromNewJson(JsonNode json) throws IOException {
        return JsonSubstanceFactory.makeSubstance(json);

    }

    @Override
    public Page page(Pageable pageable) {

        return repository.findAll(pageable);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    protected Substance update(Substance substance) {
//        controlledVocabulary.

//        JsonSubstanceFactory.fixOwners(substance, true);
        //first bump version?
        substance.forceUpdate();

        //postUpdate/etc only gets called on flush, apparently?
        return repository.saveAndFlush(getEntityManager().merge(substance));
    }

    @Override
    protected AbstractEntityUpdatedEvent<Substance> newUpdateEvent(Substance updatedEntity) {
        return new SubstanceUpdatedEvent(updatedEntity);
    }

    @Override
    protected AbstractEntityCreatedEvent<Substance> newCreationEvent(Substance createdEntity) {
        return new SubstanceCreatedEvent(createdEntity);
    }

    @Override
    public UUID getIdFrom(Substance entity) {
        return entity.getUuid();
    }

    @Override
    protected List<Substance> fromNewJsonList(JsonNode list) throws IOException {
        List<Substance> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromNewJson(n));
        }
        return substances;
    }

    @Override
    protected Substance fromUpdatedJson(JsonNode json) throws IOException {
        //TODO should we make any edits to remove fields?
        return JsonSubstanceFactory.makeSubstance(json);
    }


    @Override
    protected List<Substance> fromUpdatedJsonList(JsonNode list) throws IOException {
        List<Substance> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromUpdatedJson(n));
        }
        return substances;
    }


    @Override
    protected JsonNode toJson(Substance substance) throws IOException {
        return objectMapper.valueToTree(substance);
    }

    @Override
    protected Substance create(Substance substance) {

        try {
            return repository.saveAndFlush(substance);
        }catch(Throwable t){
            t.printStackTrace();
            throw t;
        }
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    @Transactional
    public Optional<Substance> get(UUID id) {
        return fullFetch(repository.findById(id),false);
    }

    /**
     * Fully fetch the given Substance this might be a computationally intense operation
     * and the passed in Substance is not guaranteed to be the same
     * reference of the Substance returned.
     * @param opt the Optional wrapped Substance to fully fetch.
     * @param useEF use entity fetcher to try to more efficiently fetch the Substance data.
     * @return an Optional wrapped fully fetched substance which may be empty if it could not be fetched.
     * An empty Optional passed in will always return an empty Optional.
     */
    private Optional<Substance> fullFetch(Optional<Substance> opt, boolean useEF){
        if(opt.isPresent()){
            if(useEF) {
                EntityUtils.Key k = opt.get().fetchKey();
                Optional<Substance> fetched= EntityFetcher.of(k).getIfPossible().map(o->(Substance)o);
                if(fetched.isPresent()){
                    return fetched;
                }

            }
            //if entity fetcher didn't find it fallback to full jsonnode but note
            //this might require an open transaction
            opt.get().toFullJsonNode();
        }

        return opt;
    }

    @Override
    @Transactional
    public Optional<Substance> flexLookup(String someKindOfId) {
        if(someKindOfId==null){
            return Optional.empty();
        }
        if(Util.isUUID(someKindOfId)){
            return get(UUID.fromString(someKindOfId));
        }
        //old versions of GSRS only used the first 8 chars of the uuid
        if (someKindOfId.length() == 8) { // might be uuid
            List<Substance> list = repository.findByUuidStartingWith(someKindOfId);
            if(!list.isEmpty()){
                return fullFetch(Optional.of(list.get(0)),true);
            }
        }

        Substance result = repository.findByApprovalID(someKindOfId);
        if(result !=null){
            return fullFetch(Optional.of(result),true);
        }
        List<SubstanceRepository.SubstanceSummary> summaries = repository.findByNames_NameIgnoreCase(someKindOfId);
        if(summaries !=null && !summaries.isEmpty()){

            //get the first?
            return fullFetch(repository.findById(summaries.get(0).getUuid()),true);
        }
        summaries = repository.findByCodes_CodeIgnoreCase(someKindOfId);
        if(summaries !=null && !summaries.isEmpty()){

            //get the first?
            return fullFetch(repository.findById(summaries.get(0).getUuid()),true);
        }
        return Optional.empty();
    }
    

    @Override
    protected Optional<UUID> flexLookupIdOnly(String someKindOfId) {
        //easiest way to avoid deduping data is to just do a full flex lookup and then return id
        Optional<Substance> found = flexLookup(someKindOfId);
        if(found.isPresent()){
            return Optional.of(found.get().uuid);
        }
        return Optional.empty();
    }


    @Override
    public UpdateResult<Substance> updateEntityWithoutValidation(JsonNode updatedEntityJson) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(this.getTransactionManager());

        return transactionTemplate.execute( status-> {
            try {
                Substance updatedEntity = JsonEntityUtil.fixOwners(fromUpdatedJson(updatedEntityJson), true);
                EntityUtils.Key oKey = EntityUtils.EntityWrapper.of(updatedEntity).getKey();
                EntityManager entityManager = oKey.getEntityManager();

                UpdateResult.UpdateResultBuilder<Substance> builder = UpdateResult.<Substance>builder();
                EntityUtils.EntityWrapper<Substance> savedVersion = entityPersistAdapter.change(oKey, oldEntity -> {
                        EntityUtils.EntityWrapper<Substance> og = EntityUtils.EntityWrapper.of(oldEntity);
                        String oldJson = og.toFullJson();

                        EntityUtils.EntityWrapper<Substance> oWrap = EntityUtils.EntityWrapper.of(oldEntity);
                        EntityUtils.EntityWrapper<Substance> nWrap = EntityUtils.EntityWrapper.of(updatedEntity);

                        boolean usePojoPatch = false;
                        //only use POJO patch if the entities are the same type
                        if (oWrap.getEntityClass().equals(nWrap.getEntityClass())) {
                            usePojoPatch = true;
                        }
                        if (usePojoPatch) {
                            PojoPatch<Substance> patch = PojoDiff.getDiff(oldEntity, updatedEntity);
                            LogUtil.debug(() -> "changes = " + patch.getChanges());
                            final List<Object> removed = new ArrayList<Object>();

                            //Apply the changes, grabbing every change along the way
                            Stack changeStack = patch.apply(oldEntity, c -> {
                                if ("remove".equals(c.getOp())) {
                                    removed.add(c.getOldValue());
                                }
                                LogUtil.trace(() -> c.getOp() + "\t" + c.getOldValue() + "\t" + c.getNewValue());
                            });
                            if (changeStack.isEmpty()) {
                                throw new IllegalStateException("No change detected");
                            } else {
                                LogUtil.debug(() -> "Found:" + changeStack.size() + " changes");
                            }
                            oldEntity = fixUpdatedIfNeeded(JsonEntityUtil.fixOwners(oldEntity, true));
                            //This is the last line of defense for making sure that the patch worked
                            //Should throw an exception here if there's a major problem
                            //This is inefficient, but forces confirmation that the object is fully realized
                            String serialized = EntityUtils.EntityWrapper.of(oldEntity).toJsonDiffJson();


                            while (!changeStack.isEmpty()) {
                                Object v = changeStack.pop();
                                EntityUtils.EntityWrapper<Object> ewchanged = EntityUtils.EntityWrapper.of(v);
                                if (!ewchanged.isIgnoredModel() && ewchanged.isEntity()) {
                                    Object o = ewchanged.getValue();
                                    if (o instanceof ForceUpdatableModel) {
                                        //Maybe don't do twice? IDK.
                                        ((ForceUpdatableModel) o).forceUpdate();
                                    }

                                    entityManager.merge(o);
                                }
                            }

                            //explicitly delete deleted things
                            //This should ONLY delete objects which "belong"
                            //to something. That is, have a @SingleParent annotation
                            //inside

                            removed.stream()
                                    .filter(Objects::nonNull)

                                    .map(o -> EntityUtils.EntityWrapper.of(o))
                                    .filter(ew -> ew.isExplicitDeletable())
                                    .forEach(ew -> {
                                        Object o = ew.getValue();
                                        log.warn("deleting:" + o);
                                        //hibernate can only remove entities from this transaction
                                        //this logic will merge "detached" entities from outside this transaction before removing anything

                                        entityManager.remove(entityManager.contains(o) ? o : entityManager.merge(o));

                                    });

                            try {
                                Substance saved = transactionalUpdate(oldEntity, oldJson);
//                			System.out.println("updated entity = " + saved);
                                String internalJSON = EntityUtils.EntityWrapper.of(saved).toInternalJson();
//                			System.out.println("updated entity full eager fetch = " + internalJSON.hashCode());
                                builder.updatedEntity(saved);

                                builder.status(UpdateResult.STATUS.UPDATED);

                                return Optional.of(saved);
                            } catch (Throwable t) {
                                t.printStackTrace();

                                builder.status(UpdateResult.STATUS.ERROR);
                                builder.throwable(t);
                                return Optional.empty();
                            }
                        } else {
                            //NON POJOPATCH: delete and save for updates

                            Substance oldValue = (Substance) oWrap.getValue();
                            entityManager.remove(oldValue);

                            // Now need to take care of bad update pieces:
                            //	1. Version not incremented correctly (post update hooks not called)
                            //  2. Some metadata / audit data may be problematic
                            //  3. The update hooks are called explicitly now
                            //     ... and that's a weird thing to do, because the persist hooks
                            //     will get called too. Does someone really expect things to
                            //     get called twice?
                            // TODO: the above pieces are from the old codebase, but the new one
                            // has to have these evaluated too. Need unit tests.

                            entityManager.flush();
                            // if we clear here, it will cause issues for
                            // some detached entities later, but not clearing causes other issues

                                entityManager.clear();

                        Substance newValue = (Substance)nWrap.getValue();
                            entityManager.persist(newValue);
                            entityManager.flush();


//                	    T saved=newValue;
                            Substance saved = transactionalUpdate(newValue, oldJson);
                            builder.updatedEntity(saved);
                            builder.status(UpdateResult.STATUS.UPDATED);

                            return Optional.of(saved); //Delete & Create
                        }
                    });
                if(savedVersion ==null){
                    status.setRollbackOnly();
                }else {
                    //IDK?
//                    if(forceMoreSave[0]) {
//                        EntityUtils.EntityWrapper<T> savedVersion2 = entityPersistAdapter.performChangeOn(savedVersion, sec -> {
//
//                        });
//                    }
                    //only publish events if we save!
                    AbstractEntityUpdatedEvent<Substance> event = newUpdateEvent(savedVersion.getValue());
                    if(event !=null) {
                        applicationEventPublisher.publishEvent(event);
                        //todo: if RabbitMq is desired, make the following work
                        /*if (gsrsRabbitMqConfiguration.isEnabled() && exchangeName != null) {
                            rabbitTemplate.convertAndSend(exchangeName, substanceUpdatedKey, event);
                        }*/
                    }
                }

                UpdateResult<Substance> updateResult= builder.build();
                if(updateResult.getThrowable() !=null){
                    Sneak.sneakyThrow( updateResult.getThrowable());
                }
                return updateResult;
            }catch(IOException e){
                status.setRollbackOnly();
                throw new UncheckedIOException(e);
            }
        });
    }

	@Override
	public List<UUID> getIDs() {
		List<UUID> IDs = repository.getAllIds();		
		return IDs;
	}

}
