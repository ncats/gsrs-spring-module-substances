package ix.core.processing;

import gsrs.module.substance.services.GinasSubstanceTransformerFactory;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.module.substance.services.SubstanceBulkLoadServiceConfiguration;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.ProcessingRecord;

import java.util.Objects;

public abstract class PersistRecordWorker implements Runnable {

    private final PayloadExtractedRecord prg;
    private final SubstanceBulkLoadService.BulkLoadServiceCallback callback;
    private final GinasSubstanceTransformerFactory transformerFactory;

    private final SubstanceBulkLoadServiceConfiguration bulkLoadServiceConfiguration;

    public PersistRecordWorker(PayloadExtractedRecord prg,
                               SubstanceBulkLoadServiceConfiguration bulkLoadServiceConfiguration,
                               SubstanceBulkLoadService.BulkLoadServiceCallback callback) {

        this.prg = Objects.requireNonNull(prg);
        this.transformerFactory = Objects.requireNonNull(bulkLoadServiceConfiguration.getRecordTransformFactory());
        this.bulkLoadServiceConfiguration = Objects.requireNonNull(bulkLoadServiceConfiguration);
        this.callback = Objects.requireNonNull(callback);
    }

    @Override
    public void run() {
        TransformedRecord tr = null;
        ProcessingRecord rec = new ProcessingRecord();
        
        try {
            try {
                callback.extractionSuccess();
                }catch(Exception e) {}    

            Object trans = transformerFactory.createTransformerFor().transform(prg, rec);

            if (trans == null) {
                throw new IllegalStateException("Transform error");
            }
          
            tr = AutowireHelper.getInstance().autowireAndProxy(new TransformedRecord(trans, prg.theRecord, rec, callback));
            tr.setConfig(bulkLoadServiceConfiguration);
            try {
                callback.processedSuccess();
                }catch(Exception e) {}
        } catch (Throwable t) {
            callback.processedFailure();
            //t.printStackTrace();

            SubstanceBulkLoadService.getTransformFailureLogger().info(rec.name + "\t" + t.getMessage().replace("\n", "") + "\t"
                    + prg.theRecord.toString().replace("\n", ""));

        }
        if (tr != null) {
            doPersist(tr);
        }
    }

    protected abstract void doPersist(TransformedRecord tr);
}
