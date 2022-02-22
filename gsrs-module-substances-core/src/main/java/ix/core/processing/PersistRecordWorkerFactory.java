package ix.core.processing;

import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.module.substance.services.SubstanceBulkLoadServiceConfiguration;

public interface PersistRecordWorkerFactory {
    PersistRecordWorker newWorkerFor(PayloadExtractedRecord prg,
                                     SubstanceBulkLoadServiceConfiguration configuration,
                                     SubstanceBulkLoadService.SubstanceBulkLoadParameters parameters,
                                     SubstanceBulkLoadService.BulkLoadServiceCallback callback);
}
