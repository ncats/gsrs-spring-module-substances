package gsrs.dataexchange;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.dataexchange.extractors.ExplicitMatchableExtractorFactory;
import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.service.HoldingAreaEntityService;
import gsrs.indexer.IndexValueMakerFactory;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.service.GsrsEntityService;
import ix.core.EntityFetcher;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.TextIndexer;
import ix.core.util.EntityUtils;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.JsonSubstanceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

@Slf4j
public class SubstanceHoldingAreaEntityService implements HoldingAreaEntityService<Substance> {

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private IndexValueMakerFactory factory;

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    @Override
    public Substance parse(JsonNode json) {
        return JsonSubstanceFactory.makeSubstance(json);
    }

    @Override
    public ValidationResponse<Substance> validate(Substance substance) {
        try {
            ValidationResponse<Substance> response = substanceEntityService.validateEntity((substance).toFullJsonNode());
            return response;
        } catch (Exception ex) {
            log.error("Error validating substance", ex);
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
    public GsrsEntityService.ProcessResult<Substance> persistEntity(Substance substance) {
        log.trace("saving substance {} version {} total names: {}", substance.getUuid(), substance.version, substance.names.size());
        List<String> initialVersions = Arrays.asList("0", "1");
        try {
            GsrsEntityService.ProcessResult result;
            if (substance.getUuid() == null || initialVersions.contains(substance.version)) {
                GsrsEntityService.CreationResult<Substance> creationResult = substanceEntityService.createEntity(substance.toFullJsonNode());
                result = GsrsEntityService.ProcessResult.ofCreation(creationResult);
            } else {
                GsrsEntityService.UpdateResult<Substance> updateResult = substanceEntityService.updateEntity(substance.toFullJsonNode());
                result = GsrsEntityService.ProcessResult.ofUpdate(updateResult);
            }
            log.trace("result of save {}", result.toString());
            if (result.getValidationResponse() != null) {
                result.getValidationResponse().getValidationMessages().forEach(m ->
                        log.trace("message: {}; type: {}", ((ValidationMessage) m).getMessage(), ((ValidationMessage) m).getMessageType().name())
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
        EntityUtils.EntityWrapper wrapper = EntityUtils.EntityWrapper.of(substance);
        //hackySetKind(wrapper, "sa_substance");
        //log.trace("set kind to {}", wrapper.getKind());
        log.trace("create wrapper with kind {}", wrapper.getKind(), wrapper.getEntityInfo().getIDFieldInfo().get());
        try {
            indexer.add(wrapper);
            log.info("successfully indexed object");
        }  catch (IOException ex) {
            log.error("Error indexing object!", ex);
        }
    }

    private void hackySetKind(EntityUtils.EntityWrapper wrapper, String desiredKind) {

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
}
