package gsrs.dataExchange;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.dataExchange.extractors.ExplicitMatchableExtractorFactory;
import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.service.HoldingAreaEntityService;
import gsrs.module.substance.SubstanceEntityService;
import ix.core.search.text.IndexValueMaker;
import ix.core.validator.ValidationResponse;
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
