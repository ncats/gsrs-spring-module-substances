package ix.core.plugins;

import gsrs.AuditConfig;
import gsrs.module.substance.services.GinasSubstanceTransformerFactory;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import ix.core.processing.GinasRecordProcessorPlugin;
import ix.core.processing.TransformedRecord;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A PersistRecordWorkerFactory that can persist only a single record at the same time even if in a multi-thread
 * enviornment.  Some database implementations can't save transactions across multiple threads so
 * this implementation should be used for them.
 *
 *
 *
 * Created by katzelda on 4/28/16.
 */
public class SingleThreadedPersistRecordWorkerFactory implements GinasRecordProcessorPlugin.PersistRecordWorkerFactory
{
    @Autowired
    private AuditConfig auditConfig;

    private static final Object lock = new Object();

    @Override
    public GinasRecordProcessorPlugin.PersistRecordWorker newWorkerFor(GinasRecordProcessorPlugin.PayloadExtractedRecord prg,
                                                                       GinasSubstanceTransformerFactory transformerFactory,
                                                                       SubstanceBulkLoadService.SubstanceBulkLoadParameters parameters,
                                                                       SubstanceBulkLoadService.BulkLoadServiceCallback callback) {
        return new SingleThreadedPersistRecordWorker(prg, transformerFactory, callback, parameters.isPreserveOldEditInfo());
    }

    public class SingleThreadedPersistRecordWorker extends GinasRecordProcessorPlugin.PersistRecordWorker {


        private boolean preserveOldAudit;

        public SingleThreadedPersistRecordWorker(GinasRecordProcessorPlugin.PayloadExtractedRecord prg,
                                                 GinasSubstanceTransformerFactory transformerFactory,
                                                 SubstanceBulkLoadService.BulkLoadServiceCallback callback,
                                                 boolean preserveOldAudit) {
            super(prg, transformerFactory, callback);
            this.preserveOldAudit = preserveOldAudit;
        }

        @Override
        protected void doPersist(TransformedRecord tr) {
            synchronized (lock) {
                if(preserveOldAudit){
                    auditConfig.disableAuditingFor(()->{
                        tr.persists();
                    });
                }else {
                    tr.persists();
                }
            }
        }
    }
}
