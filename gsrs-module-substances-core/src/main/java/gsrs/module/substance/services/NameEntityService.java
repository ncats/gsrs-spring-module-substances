package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.IdHelpers;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.module.substance.events.NameCreatedEvent;
import gsrs.module.substance.events.NameUpdatedEvent;
import gsrs.module.substance.repository.NameRepository;
import gsrs.service.AbstractGsrsEntityService;
import gsrs.services.GroupService;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidationResponseBuilder;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.utils.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Service
public class NameEntityService extends AbstractGsrsEntityService<Name, UUID> {
    public static final String  CONTEXT = "names";


    public NameEntityService() {
        super(CONTEXT,  IdHelpers.UUID, "gsrs_exchange", "name.created", "name.updated");
    }

    @Autowired
    private NameRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GroupService groupRepository;

    @Autowired
    private GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory;

//    @Autowired
//    private CvSearchService searchService;


    @Override
    public Class<Name> getEntityClass() {
        return Name.class;
    }

    @Override
    public UUID parseIdFromString(String idAsString) {
        return UUID.fromString(idAsString);
    }

    @Override
    protected <T> ValidatorCallback createCallbackFor(T object, ValidationResponse<T> response, ValidatorConfig.METHOD_TYPE type) {
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewDefaultStrategy();
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
                    messages.stream().forEach(strategy::processMessage);
                    resp.setValid(false);
                    if (strategy.handleMessages((Substance) object, messages)) {
                        resp.setValid(true);
                    }
                    strategy.addProblems((Substance) object, messages);


                    if (GinasProcessingMessage.ALL_VALID(messages)) {
                        resp.addValidationMessage(GinasProcessingMessage.SUCCESS_MESSAGE("Substance is valid"));
                    }
                }
            }
        };
        if(type == ValidatorConfig.METHOD_TYPE.BATCH){
            builder.allowPossibleDuplicates(true);
        }

        return builder;
    }

    @Override
    protected Name fromNewJson(JsonNode json) throws IOException {
        return objectMapper.convertValue(json, Name.class);

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
    protected Name update(Name substance) {
//        controlledVocabulary.

        //first bump version?
        substance.forceUpdate();

        return repository.save(getEntityManager().merge(substance));
    }

    @Override
    protected AbstractEntityUpdatedEvent<Name> newUpdateEvent(Name updatedEntity) {
        return new NameUpdatedEvent(updatedEntity);
    }

    @Override
    protected AbstractEntityCreatedEvent<Name> newCreationEvent(Name createdEntity) {
        return new NameCreatedEvent(createdEntity);
    }

    @Override
    public UUID getIdFrom(Name entity) {
        return entity.getUuid();
    }

    @Override
    protected List<Name> fromNewJsonList(JsonNode list) throws IOException {
        List<Name> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromNewJson(n));
        }
        return substances;
    }



    @Override
    protected Name fromUpdatedJson(JsonNode json) throws IOException {
        //TODO should we make any edits to remove fields?
        return objectMapper.convertValue(json, Name.class);
    }


    @Override
    protected List<Name> fromUpdatedJsonList(JsonNode list) throws IOException {
        List<Name> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromUpdatedJson(n));
        }
        return substances;
    }


    @Override
    protected JsonNode toJson(Name controlledVocabulary) throws IOException {
        return objectMapper.valueToTree(controlledVocabulary);
    }

    @Override
    protected Name create(Name substance) {
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
    public Optional<Name> get(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Name> flexLookup(String someKindOfId) {
        if(someKindOfId==null){
            return Optional.empty();
        }
        if(Util.isUUID(someKindOfId)){
            return get(UUID.fromString(someKindOfId));
        }
        //TODO the Play version doesn't seem to resolve any names for the name factory?

        //old versions of GSRS only used the first 8 chars of the uuid
        if (someKindOfId.length() == 8) { // might be uuid
            List<Name> list = repository.findByUuidStartingWith(someKindOfId);
            if(!list.isEmpty()){
                return Optional.of(list.get(0));
            }
        }

        List<Name> summaries = repository.findByNameIgnoreCase(someKindOfId);
        if(summaries !=null && !summaries.isEmpty()){

            //get the first?
            return repository.findById(summaries.get(0).getUuid());
        }

        return Optional.empty();

    }

    @Override
    protected Optional<UUID> flexLookupIdOnly(String someKindOfId) {
        //easiest way to avoid deduping data is to just do a full flex lookup and then return id
        Optional<Name> found = flexLookup(someKindOfId);
        if(found.isPresent()){
            return Optional.of(found.get().uuid);
        }
        return Optional.empty();
    }




}
