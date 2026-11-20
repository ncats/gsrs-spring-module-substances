package ix.core.stats;


import com.fasterxml.jackson.annotation.JsonProperty;
import gov.nih.ncats.common.util.TimeUtil;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Statistics implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = -5730152930484560692L;
    public AtomicInteger recordsExtractedSuccess= new AtomicInteger();
    public AtomicInteger recordsProcessedSuccess=new AtomicInteger();
    public AtomicInteger recordsPersistedSuccess=new AtomicInteger();

    public AtomicInteger recordsExtractedFailed=new AtomicInteger();
    public AtomicInteger recordsProcessedFailed=new AtomicInteger();
    public AtomicInteger recordsPersistedFailed=new AtomicInteger();

    @JsonProperty("start")
    private volatile long start=TimeUtil.getCurrentTimeMillis();
    private volatile Long stop;

    public enum CHANGE{	ADD_EX_GOOD,
        ADD_EX_BAD,
        ADD_PR_GOOD,
        ADD_PR_BAD,
        ADD_PE_GOOD,
        ADD_PE_BAD,
        /**
         * Take the current counts and consider the job done with the current numbers.
         */
        MARK_EXTRACTION_DONE,
        EXPLICIT_CHANGE,
        /**
         * Cancel the current job, this is similar to {@link #MARK_EXTRACTION_DONE}
         * except the final counts also consider the original estimate of records remaining
         * and presumed never to complete.
         * @since 3.0
         */
        CANCEL
    };

    public Estimate totalRecords=null;

    public volatile boolean cancelled= false;

    private CHANGE lastChangeAction=null;
    private AtomicLong lastChangeVersion = new AtomicLong();


    public String toString(){
        String msg = "Extracted: " + recordsExtractedSuccess.get() + " (" + recordsExtractedFailed.get() + " failed)\n";
        msg += "Processed: " + recordsProcessedSuccess.get() + " (" + recordsProcessedFailed.get() + " failed)\n";
        msg += "Persisted: " + recordsPersistedSuccess.get() + " (" + recordsPersistedFailed.get() + " failed)\n";
        if(totalRecords!=null)
            msg += "Total rec: " + totalRecords.getCount() + " (" + totalRecords.getType().toString() + ")\n";
        return msg;			
    }


    public int totalFailedAndPersisted() {
        return recordsExtractedFailed.get()+recordsProcessedFailed.get()+recordsPersistedFailed.get()+recordsPersistedSuccess.get();
    }

    private void markExtractionDone(){
        totalRecords= new Estimate(recordsExtractedSuccess.get()+recordsExtractedFailed.get(), Estimate.TYPE.EXACT);
    }

    private void markAsCancelled() {
        cancelled=true;
        if(totalRecords ==null){
            markExtractionDone();
            return;
        }
        //consider the rest as
        //			int x = totalFailedAndPersisted();
        //			recordsExtractedFailed.set((int)(totalRecords.getCount() -x));
    }

    public boolean _isDone(){
        if(cancelled){
            return true;
        }
        if(totalRecords==null)return false;
        if(totalFailedAndPersisted()>=totalRecords.getCount() && totalRecords.getType()==Estimate.TYPE.EXACT){
            return true;
        }
        return false;
    }

    public double getAverageTimeToPersist(){
        return this.getAverageTimeToPersistMS(TimeUtil.getCurrentTimeMillis());
    }

    public void markProcessingStarted() {
        start = TimeUtil.getCurrentTimeMillis();
        stop = null;
    }

    public Long getStop() {
        return stop;
    }

    public void setStop(Long stop) {
        this.stop = stop;
    }

    public long getEstimatedTimeLeft(){
        return getEstimatedTimeLeft(TimeUtil.getCurrentTimeMillis());
    }

    public long getEstimatedTimeLeft(long currentTimeMillis){
        if (stop != null) {
            return 0;
        }
        if(totalRecords == null
                || totalRecords.getCount() < 0
                || (totalRecords.getType() != Estimate.TYPE.EXACT
                && totalRecords.getType() != Estimate.TYPE.APPROXIMATE)){
            return -1;
        }

        long totalDone= totalFailedAndPersisted();
        long totalRemaining = Math.max(totalRecords.getCount() - totalDone, 0);
        if(totalRemaining == 0){
            return 0;
        }
        if(totalDone == 0){
            return -1;
        }

        long timeSoFar = Math.max(currentTimeMillis - start, 0);
        double estimate = (double) totalRemaining * timeSoFar / totalDone;
        return estimate >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.ceil(estimate);
    }

    public double getAverageTimeToPersistMS(long end){
        long totalPersist=recordsPersistedSuccess.get();
        if(totalPersist == 0){
            return 0;
        }
        Long stoppedAt = stop;
        long effectiveEnd = stoppedAt == null ? end : Math.min(end, stoppedAt);
        return Math.max(effectiveEnd - start, 0) / (totalPersist + 0.0);
    }

    public void applyChange(CHANGE c){
        if(c!=null){
            this.lastChangeAction=c;

            //DO NOT REORDER CASE STATEMENTS
            //the order of the case statements
            //is in the same order as the declarations in the enum
            //this should compile to a jump table instead of a table switch
            //which is an O(1) switch instead of a O(n)
            //so it should be faster.
            //if we ever add new values to the enum
            //make sure they get put here in the same order
            //enum values that shouldn't be processed
            //should either be the first or last values
            //so we have a continuous block
            switch(c){

            case ADD_EX_GOOD:
                recordsExtractedSuccess.getAndIncrement();
                break;
            case ADD_EX_BAD:
                recordsExtractedFailed.getAndIncrement();
                break;
            case ADD_PR_GOOD:
                recordsProcessedSuccess.getAndIncrement();
                break;
            case ADD_PR_BAD:
                recordsProcessedFailed.getAndIncrement();
                break;
            case ADD_PE_GOOD:
                recordsPersistedSuccess.getAndIncrement();
                break;
            case ADD_PE_BAD:
                recordsPersistedFailed.getAndIncrement();
                break;
            case MARK_EXTRACTION_DONE:
                this.markExtractionDone();
                break;
            case CANCEL:
                this.markAsCancelled();
                break;
            default:
                break;
            }
        }
        lastChangeVersion.getAndIncrement();
    }




    public void applyChange(Statistics s) {
        this.applyChange(s.lastChangeAction);
    }


    public boolean isNewer(Statistics st) {

        return this.lastChangeVersion.get() > st.lastChangeVersion.get();
    }

}
