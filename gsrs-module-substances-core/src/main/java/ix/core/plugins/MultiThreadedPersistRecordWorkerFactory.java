package ix.core.plugins;

import gsrs.AuditConfig;
import gsrs.module.substance.services.GinasSubstanceTransformerFactory;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import ix.core.processing.GinasRecordProcessorPlugin;
import ix.core.processing.TransformedRecord;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A PersistRecordWorkerFactory that can persist many records at the same time from different threads
 * concurrently.
 *
 *
 *
 * Created by katzelda on 4/28/16.
 */
public class MultiThreadedPersistRecordWorkerFactory implements GinasRecordProcessorPlugin.PersistRecordWorkerFactory {
    @Autowired
    private AuditConfig auditConfig;


    @Override
    public GinasRecordProcessorPlugin.PersistRecordWorker newWorkerFor(GinasRecordProcessorPlugin.PayloadExtractedRecord prg,
                                                                       GinasSubstanceTransformerFactory transformerFactory,
                                                                       SubstanceBulkLoadService.SubstanceBulkLoadParameters parameters,
                                                                       SubstanceBulkLoadService.BulkLoadServiceCallback callback) {
        return new MultiThreadedPersistRecordWorker(prg, transformerFactory, parameters.isPreserveOldEditInfo(), callback);
    }


    private class MultiThreadedPersistRecordWorker extends GinasRecordProcessorPlugin.PersistRecordWorker {

        private boolean preserveOldAudit;

        public MultiThreadedPersistRecordWorker(GinasRecordProcessorPlugin.PayloadExtractedRecord prg,
                                                GinasSubstanceTransformerFactory transformerFactory,
                                                boolean preserveOldAudit,
                                                SubstanceBulkLoadService.BulkLoadServiceCallback callback) {
            super(prg,transformerFactory, callback);
            this.preserveOldAudit = preserveOldAudit;
        }

        @Override
        protected void doPersist(TransformedRecord tr) {

            if(preserveOldAudit){
                auditConfig.disableAuditingFor(()->{
                    tr.persists();
                });
            }else{
                tr.persists();
            }

        }
    }
}
