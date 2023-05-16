package gsrs.dataexchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.common.util.CachedSupplierGroup;
import gsrs.dataexchange.extractors.ExplicitMatchableExtractorFactory;
import gsrs.events.ReindexEntityEvent;
import gsrs.module.substance.standardizer.SubstanceSynchronizer;
import gsrs.springUtils.AutowireHelper;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.service.StagingAreaEntityService;
import gsrs.indexer.IndexValueMakerFactory;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.service.GsrsEntityService;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.GsrsValidatorFactory;
import gsrs.validator.ValidatorConfig;
import ix.core.EntityFetcher;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.TextIndexer;
import ix.core.util.EntityUtils;
import ix.core.validator.*;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.JsonSubstanceFactory;
import ix.ginas.utils.validation.ValidatorFactory;
import ix.ginas.utils.validation.validators.DefinitionalDependencyValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class SubstanceStagingAreaEntityService implements StagingAreaEntityService<Substance> {

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private IndexValueMakerFactory factory;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    SubstanceSynchronizer substanceSynchronizer;

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    @Override
    public Substance parse(JsonNode json) {
        return JsonSubstanceFactory.makeSubstance(json);
    }

    @Autowired
    private GsrsValidatorFactory validatorFactoryService;

    private CachedSupplier<ValidatorFactory> validatorFactory;
    CachedSupplierGroup ENTITY_SERVICE_INITIALIZATION_GROUP = new CachedSupplierGroup();

    private boolean initializedValidator = false;
    @PostConstruct
    private void initValidator(){
        //need this in a post construct so the validator factory service is injected
        //This is added to the initization Group so that we can reset this in tests

        //This cache might be unncessary as of now the call to newFactory isn't cached
        //by tests where the return value could change over time?  but keep it here anyway for now...
        validatorFactory = ENTITY_SERVICE_INITIALIZATION_GROUP.add(()->validatorFactoryService.newFactory("substances"));
    }

    private final ValidatorConfig.METHOD_TYPE validationMethodType = ValidatorConfig.METHOD_TYPE.BATCH;

    @Override
    public ValidationResponse<Substance> validate(Substance substance) {
        try {
            if(!initializedValidator) {
                initValidator();
                initializedValidator=true;
            }
            //need to remove the UUID or errors occur on the deserialization process within validation
            UUID originalUuid=null;
            boolean ignoreMessageAboutUuid=false;
            if( substance.uuid!=null){
                originalUuid=substance.uuid;
                ignoreMessageAboutUuid=true;
            }
            substance.uuid=null;
            Validator<Substance> validator = validatorFactory.getSync().createValidatorFor(substance, null,
                    validationMethodType, ValidatorCategory.CATEGORY_ALL());

            ValidationResponse<Substance> response = new ValidationResponse<>(substance);
            ValidatorCallback callback = createCallbackFor(substance, response, validationMethodType);
            validator.validate(substance, null, callback);
            callback.complete();
            //ValidationResponse<Substance> response = substanceEntityService.validateEntity((substance).toFullJsonNode(), ValidatorCategory.);
            //treat one validator separately
            DefinitionalDependencyValidator definitionalDependencyValidator = new DefinitionalDependencyValidator();
            AutowireHelper.getInstance().autowire(definitionalDependencyValidator);
            ValidationResponse<Substance> response2 = definitionalDependencyValidator.validate(substance, null);
            substance.uuid=originalUuid;
            log.trace("adding messages from DefinitionalDependencyValidator");
            boolean finalIgnoreMessageAboutUuid = ignoreMessageAboutUuid;
            response.getValidationMessages().forEach(m->{
                if(!(finalIgnoreMessageAboutUuid && m.getMessage().startsWith("Substance has no UUID, will generate uuid"))){
                    response2.addValidationMessage(m);
                }
            });
            response2.setNewObject(response.getNewObject());
            return response2;
        } catch (Exception ex) {
            log.warn("Error validating substance", ex);
        }
        return null;
    }

    @Override
    public List<MatchableKeyValueTuple> extractKVM(Substance substance) {
        if (substance instanceof ChemicalSubstance) {
            Objects.requireNonNull(structureProcessor, "A structure processor is required to handle a chemical substances!");
            log.trace("chemical substance in extractKVM");
            ChemicalSubstance chemicalSubstance = (ChemicalSubstance) substance;
            Objects.requireNonNull(chemicalSubstance.getStructure(), "A chemical substance must have a structure");
            log.trace("going to instrument structure");
            Structure structure = structureProcessor.instrument(chemicalSubstance.getStructure().toChemical(), true);
            chemicalSubstance.getStructure().updateStructureFields(structure);
        } else {
            log.trace("other type of substance");
        }
        List<MatchableKeyValueTuple> allMatchables = new ArrayList<>();
        ExplicitMatchableExtractorFactory factory = new ExplicitMatchableExtractorFactory();
        factory.createExtractorFor(Substance.class).extract(substance, allMatchables::add);
        return allMatchables;
    }

    @Override
    public IndexValueMaker<Substance> createIVM(Substance substance) {
        //todo: check implementation
        return factory.createIndexValueMakerFor(substance);
    }

    @Override
    public GsrsEntityService.ProcessResult<Substance> persistEntity(Substance substance, boolean isNew) {
        log.trace("saving substance {} version {} total names: {}", substance.getUuid(), substance.version, substance.names.size());
        try {
            GsrsEntityService.ProcessResult<Substance> result;
            if (isNew) {
                GsrsEntityService.CreationResult<Substance> creationResult = substanceEntityService.createEntity(substance.toFullJsonNode(),
                        true);
                result = GsrsEntityService.ProcessResult.ofCreation(creationResult);
            } else {
                GsrsEntityService.UpdateResult<Substance> updateResult = substanceEntityService.updateEntity(substance.toFullJsonNode());
                result = GsrsEntityService.ProcessResult.ofUpdate(updateResult);
            }
            log.trace("result of save {}", result.toString());
            if (result.getValidationResponse() != null) {
                result.getValidationResponse().getValidationMessages().forEach(m ->
                        log.trace("message: {}; type: {}",  m.getMessage(), m.getMessageType().name())
                );
            }

            return result;
        } catch (Exception e) {
            log.error("Error updating substance!", e);
            log.trace("building return object");
            GsrsEntityService.ProcessResult.ProcessResultBuilder<Substance> builder = GsrsEntityService.ProcessResult.builder();
            return builder
                    .entity(substance)
                    .saved(false)
                    .throwable(e)
                    .build();
        }
    }

    @Override
    public Substance retrieveEntity(String entityId) {
        Substance substance;
        try {
            substance = (Substance) EntityFetcher.of(EntityUtils.Key.of(Substance.class, UUID.fromString(entityId))).call();
            log.trace("retrieved substance {} version {} total names: {}", substance.getUuid(), substance.version, substance.names.size());
        } catch (Exception e) {
            log.error("Error retrieving substance", e);
            throw new RuntimeException(e);
        }
        return substance;
    }

    @Override
    public void IndexEntity(TextIndexer indexer, Object object) {
        Substance substance= (Substance) object;
        log.trace("substance uuid: {}", substance.getUuid());
        if( substance.getUuid()==null) {
            log.trace("assigned id {}", substance.getOrGenerateUUID());
        }
        EntityUtils.EntityWrapper<Substance> wrapper = EntityUtils.EntityWrapper.of(substance);
        log.trace("create wrapper with kind {} - id field {}", wrapper.getKind(), wrapper.getEntityInfo().getIDFieldInfo().get());
        UUID reindexUuid = UUID.randomUUID();
        ReindexEntityEvent event = new ReindexEntityEvent(reindexUuid, wrapper.getKey(), Optional.of(wrapper),true);
        applicationEventPublisher.publishEvent(event);
        log.info("submitted object for indexing");
    }

    @Override
    public void synchronizeEntity(Substance substance, Consumer<String> recorder, JsonNode options) {
        log.trace("Starting synchronizeEntity");
        String uuidCodeSystem = (options.isObject() && options.hasNonNull("refUuidCodeSystem") )?
                ((ObjectNode)options).get("refUuidCodeSystem").textValue()  : "UUID Code";
        String approvalIdCodeSystem = (options.isObject() && options.hasNonNull("refApprovalIdCodeSystem") )?
                ((ObjectNode)options).get("refApprovalIdCodeSystem").textValue()  : "FDA UNII";
        log.trace("using uuidCodeSystem: {} and approvalIdCodeSystem: {}", uuidCodeSystem, approvalIdCodeSystem);
        substanceSynchronizer.fixSubstanceReferences(substance, recorder, uuidCodeSystem, approvalIdCodeSystem);
        log.trace("finishing synchronizeEntity");
    }


    /*private void hackySetKind(EntityUtils.EntityWrapper wrapper, String desiredKind) {

        try {
            Field ieField = EntityUtils.EntityWrapper.class.getField("ei");
            ieField.setAccessible(true);
            EntityUtils.EntityInfo ei = (EntityUtils.EntityInfo) ieField.get(wrapper);
            Field kindField = ei.getClass().getField("kind");
            kindField.setAccessible(true);
            kindField.set(ei, desiredKind);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.error("Error accessing fields", e);
        }
    }
*/
    protected <T> ValidatorCallback createCallbackFor(T object, ValidationResponse<T> response, DefaultValidatorConfig.METHOD_TYPE type) {
        return new ValidatorCallback() {
            @Override
            public void addMessage(ValidationMessage message) {
                response.addValidationMessage(message);
            }

            @Override
            public void setInvalid() {
                response.setValid(false);
            }

            @Override
            public void setValid() {
                response.setValid(true);
            }

            @Override
            public void haltProcessing() {

            }

            @Override
            public void addMessage(ValidationMessage message, Runnable appyAction) {
                response.addValidationMessage(message);
                if( appyAction!=null) {
                    appyAction.run();
                } else {
                    log.warn("in addMessage, appyAction is null");
                }
            }

            @Override
            public void complete() {
                if(response.hasError()){
                    response.setValid(false);
                }
            }
        };
    }
}
