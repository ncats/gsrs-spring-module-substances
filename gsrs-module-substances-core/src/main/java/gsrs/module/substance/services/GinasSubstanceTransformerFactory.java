package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import ix.core.processing.RecordTransformFactory;
import ix.core.processing.RecordTransformer;

public class GinasSubstanceTransformerFactory implements RecordTransformFactory<JsonNode, JsonNode> {

    public static GinasSubstanceTransformerFactory INSTANCE = new GinasSubstanceTransformerFactory();

    private GinasSubstanceTransformerFactory(){
        //can not instantiate
    }
    @Override
    public RecordTransformer<JsonNode, JsonNode> createTransformerFor() {
        return SubstanceBulkLoadService.GinasSubstanceTransformer.INSTANCE;
    }
}
