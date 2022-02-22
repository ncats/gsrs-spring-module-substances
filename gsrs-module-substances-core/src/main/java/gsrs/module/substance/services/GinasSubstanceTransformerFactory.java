package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import ix.core.processing.RecordTransformFactory;
import ix.core.processing.RecordTransformer;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.ValidatorFactory;

public class GinasSubstanceTransformerFactory implements RecordTransformFactory<JsonNode, Substance> {
    private final ValidatorFactory validatorFactory;

    public GinasSubstanceTransformerFactory(ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    @Override
    public RecordTransformer<JsonNode, Substance> createTransformerFor() {
        return new SubstanceBulkLoadService.GinasSubstanceTransformer(validatorFactory);
    }
}
