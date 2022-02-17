package ix.core.processing;

import ix.core.models.ProcessingJob;
import ix.core.models.ProcessingJobUtils;

import java.io.Serializable;

public class PayloadExtractedRecord<K> implements Serializable {
    public final ProcessingJob job;
    public final K theRecord;
    public final String jobKey;

    public PayloadExtractedRecord(ProcessingJob job, K rec) {
        this.job = job;
        this.theRecord = rec;
        String k = job.getKeyMatching(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY);
        jobKey = k;
    }
}
