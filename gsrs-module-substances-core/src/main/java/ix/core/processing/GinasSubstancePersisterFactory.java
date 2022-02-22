package ix.core.processing;

import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.ProcessingJob;
import ix.ginas.models.v1.Substance;

public class GinasSubstancePersisterFactory implements RecordPersisterFactory<Substance, Substance>{
    @Override
    public String getPersisterName() {
        return SubstanceBulkLoadService.GinasSubstancePersister.class.getName();
    }

    @Override
    public RecordPersister<Substance,Substance> createPersisterFor(ProcessingJob job) {
        return AutowireHelper.getInstance().autowireAndProxy(new SubstanceBulkLoadService.GinasSubstancePersister());
    }
}
