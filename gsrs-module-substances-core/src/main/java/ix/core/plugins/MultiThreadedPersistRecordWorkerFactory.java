package ix.core.plugins;

import gsrs.AuditConfig;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.module.substance.services.SubstanceBulkLoadServiceConfiguration;
import ix.core.processing.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A PersistRecordWorkerFactory that can persist many records at the same time from different threads
 * concurrently.
 *
 *
 *
 * Created by katzelda on 4/28/16.
 */
public class MultiThreadedPersistRecordWorkerFactory implements PersistRecordWorkerFactory {
    @Autowired
    private AuditConfig auditConfig;


    @Override
    public PersistRecordWorker newWorkerFor(PayloadExtractedRecord prg,
                                            SubstanceBulkLoadServiceConfiguration bulkLoadServiceConfiguration,
                                            SubstanceBulkLoadService.SubstanceBulkLoadParameters parameters,
                                            SubstanceBulkLoadService.BulkLoadServiceCallback callback) {
        return new MultiThreadedPersistRecordWorker(prg, bulkLoadServiceConfiguration, parameters.isPreserveOldEditInfo(), callback);
    }


    private class MultiThreadedPersistRecordWorker extends PersistRecordWorker {

        private boolean preserveOldAudit;

        public MultiThreadedPersistRecordWorker(PayloadExtractedRecord prg,
                                                SubstanceBulkLoadServiceConfiguration bulkLoadServiceConfiguration,
                                                boolean preserveOldAudit,
                                                SubstanceBulkLoadService.BulkLoadServiceCallback callback) {
            super(prg,bulkLoadServiceConfiguration, callback);
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
