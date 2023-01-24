package gsrs.dataexchange;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.dataexchange.extractors.ExplicitMatchableExtractorFactory;
import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.service.HoldingAreaEntityService;
import gsrs.indexer.ComponentScanIndexValueMakerFactory;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.service.GsrsEntityService;
import gsrs.startertests.TestIndexValueMakerFactory;
import ix.core.EntityFetcher;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.search.text.IndexValueMaker;
import ix.core.util.EntityUtils;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.JsonSubstanceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class SubstanceHoldingAreaEntityService implements HoldingAreaEntityService<Substance> {

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private ComponentScanIndexValueMakerFactory factory;

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
        }catch (Exception ex) {
            log.error("Error validating substance", ex);
        }
        return null;
    }

    @Override
    public List<MatchableKeyValueTuple> extractKVM(Substance substance) {
        if( substance instanceof ChemicalSubstance) {
            Objects.requireNonNull(structureProcessor, "A structure processor is required to handle a chemical substances!");
            log.trace("chemical substance in extractKVM");
            ChemicalSubstance chemicalSubstance = (ChemicalSubstance) substance;
            Objects.requireNonNull(chemicalSubstance.getStructure(), "A chemical substance must have a structure");
            log.trace("going to instrument structure");
            Structure structure = structureProcessor.instrument(chemicalSubstance.getStructure().toChemical(), true);
            chemicalSubstance.getStructure().updateStructureFields(structure);
        }
        else {
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
    public GsrsEntityService.UpdateResult<Substance> persistEntity(Substance substance) {
        //temporary debug
        log.trace("saving substance {} version {} total names: {}", substance.getUuid(), substance.version, substance.names.size());
        try {
            GsrsEntityService.UpdateResult<Substance>result= substanceEntityService.updateEntity(substance.toFullJsonNode());
            log.trace("result of save {}", result.getStatus());
            result.getValidationResponse().getValidationMessages().forEach(m->
                    log.trace("message: {}; type: {}", m.getMessage(), m.getMessageType().name())
            );
            return result;
        } catch (Exception e) {
            log.error("Error updating substance!", e);
        }
        return null;
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
}
