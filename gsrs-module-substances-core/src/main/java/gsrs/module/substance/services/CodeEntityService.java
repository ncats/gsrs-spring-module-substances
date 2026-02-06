package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.DefaultDataSourceConfig;
import gsrs.controller.IdHelpers;
import gsrs.events.AbstractEntityCreatedEvent;
import gsrs.events.AbstractEntityUpdatedEvent;
import gsrs.module.substance.events.CodeCreatedEvent;
import gsrs.module.substance.events.CodeUpdatedEvent;
import gsrs.module.substance.repository.CodeRepository;
import gsrs.service.AbstractGsrsEntityService;
import gsrs.services.GroupService;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Group;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidationResponseBuilder;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Code;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Service
@Slf4j
public class CodeEntityService extends AbstractGsrsEntityService<Code, UUID> {
    public static final String  CONTEXT = "codes";

    private static final String SYSTEM_GENERATED_CODE = "System Generated Code";

    private static final String SYSTEM = "SYSTEM";


    public CodeEntityService() {
        super(CONTEXT,  IdHelpers.UUID, "gsrs_exchange", "code.created", "code.updated");
    }

    @Autowired
    private CodeRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GroupService groupService;

    @Autowired
    private GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory;

    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
    private EntityManager entityManager;

//    @Autowired
//    private CvSearchService searchService;


    @Override
    public Class<Code> getEntityClass() {
        return Code.class;
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
    protected Code fromNewJson(JsonNode json) throws IOException {
        return objectMapper.convertValue(json, Code.class);

    }

    @Override
    public Page page(Pageable pageable) {

        return repository.findAll(pageable);
    }

    @Override
    public void delete(UUID id) {
        log.error("unsupported operation");
        //the base controller will handle the request and send a message... This exception is here as a safety measure
        throw new RuntimeException("Please update the Substance when deleting a Code to ensure correct processing");
    }

    @Override
    @Transactional
    protected Code update(Code substance) {
        log.error("unsupported operation");
        //the base controller will handle the request and send a message... This exception is here as a safety measure
        throw new RuntimeException("Please update the Substance when modifying a Code to ensure correct processing");
    }

    @Override
    protected AbstractEntityUpdatedEvent<Code> newUpdateEvent(Code updatedEntity) {
        return new CodeUpdatedEvent(updatedEntity);
    }

    @Override
    protected AbstractEntityCreatedEvent<Code> newCreationEvent(Code createdEntity) {
        return new CodeCreatedEvent(createdEntity);
    }

    @Override
    public UUID getIdFrom(Code entity) {
        return entity.getUuid();
    }

    @Override
    protected List<Code> fromNewJsonList(JsonNode list) throws IOException {
        List<Code> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromNewJson(n));
        }
        return substances;
    }



    @Override
    protected Code fromUpdatedJson(JsonNode json) throws IOException {
        //TODO should we make any edits to remove fields?
        return objectMapper.convertValue(json, Code.class);
    }


    @Override
    protected List<Code> fromUpdatedJsonList(JsonNode list) throws IOException {
        List<Code> substances = new ArrayList<>(list.size());
        for(JsonNode n : list){
            substances.add(fromUpdatedJson(n));
        }
        return substances;
    }


    @Override
    protected JsonNode toJson(Code controlledVocabulary) throws IOException {
        return objectMapper.valueToTree(controlledVocabulary);
    }

    @Override
    protected Code create(Code substance) {
        //the base controller will handle the request and send a message... This exception is here as a safety measure
        throw new RuntimeException("Please update the Substance when creating a Code to ensure correct processing");
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Optional<Code> get(UUID id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Code> flexLookup(String someKindOfId) {
        if(someKindOfId==null){
            return Optional.empty();
        }
        if(Util.isUUID(someKindOfId)){
            return get(UUID.fromString(someKindOfId));
        }
        //TODO the Play version doesn't seem to resolve any Codes for the Code factory?

        //old versions of GSRS only used the first 8 chars of the uuid
        if (someKindOfId.length() == 8) { // might be uuid
            List<Code> list = repository.findByUuidStartingWith(someKindOfId);
            if(!list.isEmpty()){
                return Optional.of(list.get(0));
            }
        }

        List<Code> summaries = repository.findByCodeIgnoreCase(someKindOfId);
        if(summaries !=null && !summaries.isEmpty()){

            //get the first?
            return repository.findById(summaries.get(0).getUuid());
        }

        return Optional.empty();

    }

    @Override
    protected Optional<UUID> flexLookupIdOnly(String someKindOfId) {
        //easiest way to avoid deduping data is to just do a full flex lookup and then return id
        Optional<Code> found = flexLookup(someKindOfId);
        if(found.isPresent()){
            return Optional.of(found.get().uuid);
        }
        return Optional.empty();
    }

    @Transactional
    public Code createNewCode(Substance substance, String codeSystem, String code){
        Code newCode = new Code(codeSystem, code);
        substance.addCode(newCode);
        return newCode;
    }
    @Transactional
    public Code createNewSystemCode(Substance substance, String codeSystem, Function<Code, String> idGenerator,
                                    String... groups){
        Code c = new Code();
        c.codeSystem=codeSystem;
        c.code=idGenerator.apply(c);
        c.type="PRIMARY";

        Reference r = new Reference();
        r.docType=SYSTEM;
        r.citation=SYSTEM_GENERATED_CODE;
        if(groups !=null) {
            for (String groupName : groups) {
                if (groupName == null) {
                    continue;
                }
                Group g = groupService.registerIfAbsent(groupName);
                r.addRestrictGroup(g);
                c.addRestrictGroup(g);
            }
        }

        substance.addCode(c);
        c.addReference(r, substance);

        //Is this a good idea?
        c.clearDirtyFields();
        r.clearDirtyFields();
        substance.clearDirtyFields();

        return c;
    }

    @Override
    public List<UUID> getIDs() {
        return repository.getAllIDs();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
