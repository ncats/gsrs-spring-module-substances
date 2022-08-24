package gsrs.dataexchange;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.dataexchange.extractors.ExplicitMatchableExtractorFactory;
import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.service.HoldingAreaEntityService;
import gsrs.module.substance.SubstanceEntityService;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.search.text.IndexValueMaker;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.JsonSubstanceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SubstanceHoldingAreaEntityService implements HoldingAreaEntityService<Substance> {

    @Autowired
    protected SubstanceEntityService substanceEntityService;

    @Autowired
    private StructureProcessor structureProcessor;

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
            ValidationResponse response = substanceEntityService.validateEntity((substance).toFullJsonNode());
            return response;
        }catch (Exception ex) {
            log.error("Error validating substance", ex);
        }
        return null;
    }

    @Override
    public List<MatchableKeyValueTuple> extractKVM(Substance substance) {
        if( substance instanceof ChemicalSubstance) {
            log.trace("chemical substance in extractKVM");
            ChemicalSubstance chemicalSubstance = (ChemicalSubstance) substance;
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
        return null;
    }
}
