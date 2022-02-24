package ix.core.processing;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.ProcessingJob;

public class GinasSubstancePersisterFactory implements RecordPersisterFactory<JsonNode, JsonNode>{
    @Override
    public String getPersisterName() {
        return SubstanceBulkLoadService.GinasSubstancePersister.class.getName();
    }

    @Override
    public RecordPersister<JsonNode,JsonNode> createPersisterFor(ProcessingJob job) {
        return AutowireHelper.getInstance().autowireAndProxy(new SubstanceBulkLoadService.GinasSubstancePersister());
    }
}
