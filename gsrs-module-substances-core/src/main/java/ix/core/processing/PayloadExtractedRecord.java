package ix.core.processing;

import ix.core.EntityFetcher;
import ix.core.models.ProcessingJob;
import ix.core.models.ProcessingJobUtils;
import ix.core.util.EntityUtils.EntityWrapper;

import java.io.Serializable;

public class PayloadExtractedRecord<T> implements Serializable {
    public final ProcessingJob job;
    public final T theRecord;
    public final String jobKey;

    public PayloadExtractedRecord(ProcessingJob job, T rec) {
        
        this.theRecord = rec;
        String theJobKey=null;
        // Try to use the job directly, but if it's not fully loaded and there needs
        // to be another retrieval from the database, do it with an entityfetcher 
        try {
        	theJobKey = job.getKeyMatching(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY);
        }catch(Exception e) {
        	job=EntityFetcher.ofPojo(job).getIfPossible().orElse(null);
        	theJobKey = job.getKeyMatching(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY);
        }
        this.job=job;
        this.jobKey=theJobKey;
    }
}
