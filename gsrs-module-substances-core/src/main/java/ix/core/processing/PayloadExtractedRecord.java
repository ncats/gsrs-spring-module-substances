package ix.core.processing;

import ix.core.models.ProcessingJob;
import ix.core.models.ProcessingJobUtils;

import java.io.Serializable;

public class PayloadExtractedRecord<T> implements Serializable {
    public final ProcessingJob job;
    public final T theRecord;
    public final String jobKey;

    public PayloadExtractedRecord(ProcessingJob job, T rec) {
        this(job, rec, job.getKeyMatching(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY));
    }

    public PayloadExtractedRecord(ProcessingJob job, T rec, String jobKey) {
        this.theRecord = rec;
        this.job=job;
        this.jobKey=jobKey;
    }
}
