package ix.core.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.module.substance.services.SubstanceBulkLoadServiceConfiguration;
import ix.core.models.ProcessingRecord;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

public class TransformedRecord<K, V> implements Serializable {
    public final V recordToPersist;
    public final ProcessingRecord rec;
    final K theRecord;

    private static ObjectMapper MAPPER = new ObjectMapper();
    private SubstanceBulkLoadService.BulkLoadServiceCallback bulkLoadServiceCallBack;

    private SubstanceBulkLoadServiceConfiguration config;

    public SubstanceBulkLoadServiceConfiguration getConfig() {
        return config;
    }

    public void setConfig(SubstanceBulkLoadServiceConfiguration config) {
        this.config = config;
    }

    public TransformedRecord(V persistRecord, K record,
                             ProcessingRecord rec,
                             SubstanceBulkLoadService.BulkLoadServiceCallback bulkLoadServiceCallBack) {
        this.recordToPersist = persistRecord;
        this.rec = rec;
        this.theRecord = record;
        this.bulkLoadServiceCallBack = bulkLoadServiceCallBack;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persists() {



        boolean persisted=false;
        try {
            if (config.isActuallyPersist()) {
                config.getRecordPersisterFactory().createPersisterFor(rec.job).persist(this);
                persisted=true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            bulkLoadServiceCallBack.persistedFailure();
            SubstanceBulkLoadService.getPersistFailureLogger().info(rec.name + "\t" + rec.message + "\t"
                    + MAPPER.valueToTree(theRecord).toString().replace("\n", ""));
        }
        if(persisted) {
            try {
            bulkLoadServiceCallBack.persistedSuccess();
            }catch(Exception e) {
                e.printStackTrace();
            }
        }

    }
}
