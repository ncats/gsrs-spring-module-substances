package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.IdHelpers;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.module.substance.events.ReferenceCreatedEvent;
import gsrs.module.substance.events.ReferenceUpdatedEvent;
import gsrs.module.substance.repository.ReferenceRepository;
import gsrs.service.AbstractGsrsEntityService;
import gsrs.services.GroupService;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidationResponseBuilder;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ReferenceEntityService extends AbstractGsrsEntityService<Reference, UUID> {
    public static final String  CONTEXT = "references";


    @Override
    public boolean isReadOnly() {
        return true;
    }

    public ReferenceEntityService() {
        super(CONTEXT,  IdHelpers.UUID, "gsrs_exchange", "reference.created", "reference.updated");
    }

    @Autowired
    private ReferenceRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GroupService groupRepository;

    @Autowired
    private GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory;

//    @Autowired
//    private CvSearchService searchService;


    @Override
    public Class<Reference> getEntityClass() {
        return Reference.class;
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
                        resp.addValidationMessage(GinasProcessingMessage.SUCCESS_MESSAGE("ReferenceEntityServiceSuccess","Substance is valid"));
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
    protected Reference fromNewJson(JsonNode json) throws IOException {
        return objectMapper.convertValue(json, Reference.class);

    }

    @Override
    public Page page(Pageable pageable) {

        return repository.findAll(pageable);
    }

    @Override
    public void delete(UUID id) {
        log.error("unsupported operation");
        //the base controller will handle the request and send a message... This exception is here as a safety measure
        throw new RuntimeException("Please update the Substance when deleting a Reference to ensure correct processing");
    }

    @Override
    @Transactional
    protected Reference update(Reference substance) {
        //the base controller will handle the request and send a message... This exception is here as a safety measure
        throw new RuntimeException("Please update the Substance when updating a Reference to ensure correct processing");
    }

    @Override
    protected AbstractEntityUpdatedEvent<Reference> newUpdateEvent(Reference updatedEntity) {
        return new ReferenceUpdatedEvent(updatedEntity);
    }

    @Override
    protected AbstractEntityCreatedEvent<Reference> newCreationEvent(Reference createdEntity) {
        return new ReferenceCreatedEvent(createdEntity);
    }

    @Override
    public UUID getIdFrom(Reference entity) {
        return entity.getUuid();
    }

    @Override
    protected List<Reference> fromNewJsonList(JsonNode list) throws IOException {
        List<Reference> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromNewJson(n));
        }
        return substances;
    }



    @Override
    protected Reference fromUpdatedJson(JsonNode json) throws IOException {
        //TODO should we make any edits to remove fields?
        return objectMapper.convertValue(json, Reference.class);
    }


    @Override
    protected List<Reference> fromUpdatedJsonList(JsonNode list) throws IOException {
        List<Reference> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromUpdatedJson(n));
        }
        return substances;
    }


    @Override
    protected JsonNode toJson(Reference controlledVocabulary) throws IOException {
        return objectMapper.valueToTree(controlledVocabulary);
    }

    @Override
    protected Reference create(Reference substance) {
        throw new RuntimeException("Please update the Substance when creating a Reference to ensure correct processing");
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Optional<Reference> get(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Reference> flexLookup(String someKindOfId) {
        if(someKindOfId==null){
            return Optional.empty();
        }
        if(Util.isUUID(someKindOfId)){
            return get(UUID.fromString(someKindOfId));
        }
        //TODO the Play version doesn't seem to resolve any names for the name factory?

        //old versions of GSRS only used the first 8 chars of the uuid
        if (someKindOfId.length() == 8) { // might be uuid
            List<Reference> list = repository.findByUuidStartingWith(someKindOfId);
            if(!list.isEmpty()){
                return Optional.of(list.get(0));
            }
        }
        return Optional.empty();

    }

    @Override
    protected Optional<UUID> flexLookupIdOnly(String someKindOfId) {
        //easiest way to avoid deduping data is to just do a full flex lookup and then return id
        Optional<Reference> found = flexLookup(someKindOfId);
        if(found.isPresent()){
            return Optional.of(found.get().uuid);
        }
        return Optional.empty();
    }

	@Override
	public List<UUID> getIDs() {
		return repository.getAllIDs();
	}
}
