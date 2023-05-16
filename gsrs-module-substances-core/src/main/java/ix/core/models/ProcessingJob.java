package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.model.GsrsApiAction;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.EntityMapperOptions;
import ix.core.FieldResourceReference;
import ix.core.processing.RecordExtractor;
import ix.core.processing.RecordExtractorFactory;
import ix.core.processing.RecordPersister;
import ix.core.processing.RecordPersisterFactory;
import ix.core.stats.Statistics;
import org.springframework.data.annotation.CreatedBy;

import javax.persistence.*;
import java.io.IOException;
import java.util.*;

@Entity
@Table(name="ix_core_procjob")
@Indexable(indexed = false)
@EntityMapperOptions(selfRelViews = BeanViews.Compact.class, idProviderRef = "loaderLabel")
@SequenceGenerator(name = "LONG_SEQ_ID", sequenceName = "ix_core_procjob_seq", allocationSize = 1)
public class ProcessingJob extends LongBaseModel {


	private static final String EXTRACTOR_KEYWORD = "EXTRACTOR";
	private static final String TRANSFORM_KEYWORD = "TRANSFORM";
	private static final String PERSISTER_KEYWORD = "PERSISTER";
	
    public enum Status {
        COMPLETE, RUNNING, NOT_RUN, FAILED, PENDING, STOPPED, UNKNOWN;

        public boolean isComplete(){
            return this == COMPLETE;
        }

        public boolean isInFinalState(){
            return this == COMPLETE || this == FAILED || this == STOPPED;
        }
    }



    @ManyToMany(cascade=CascadeType.ALL)
    @JoinTable(name="ix_core_procjob_key")
    //, inverseJoinColumns = {
    //            @JoinColumn(name="ix_core_procjob_id")
    public List<Keyword> keys = new ArrayList<Keyword>();

    @Transient
    @JsonIgnore
    private Map<String, Keyword> keywordLabelMap;

    @Transient
    @JsonIgnore
    private Map<String, Keyword> keywordTermMap;

    @Indexable(facet=true, name="Job Status")
    public Status status = Status.PENDING;
    
    @Column(name="job_start")
    public Long start;
    
    @Column(name="job_stop")
    public Long stop;

    @Lob
    @Basic(fetch=FetchType.EAGER)
    public String message;
    
    @Lob
    @Basic(fetch=FetchType.EAGER)
    @JsonView(BeanViews.Private.class)
    public String statistics;

    @OneToOne(cascade=CascadeType.ALL)
    @JsonView(BeanViews.Full.class)
    @CreatedBy
    public Principal owner;
    
    @OneToOne(cascade=CascadeType.ALL)
    @JsonView(BeanViews.Full.class)
    public Payload payload;
    
    @Version
    public Long version;
    
    
    public Date lastUpdate; // here
    @Transient
    @JsonIgnore
    private static ObjectMapper om = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ProcessingJob () {
    }

    /**
     * This is the id we use for the rest api.
     * @return
     */
    public String loaderLabel(){
        for(Keyword key : keys){

            if(ProcessingJobUtils.LEGACY_PLUGIN_LABEL_KEY.equals(key.label)){
                return key.term;
            }
        }
        return null;
    }


    /*
    @JsonIgnore
    @GsrsApiAction(value= "oldValue", serializeUrlOnly = true, isRaw = true)
    public FieldResourceReference<JsonNode> getOldValueReference() {
        if(oldValue ==null){
            return null;
        }
        return FieldResourceReference.forRawFieldAsJson("oldValue", oldValue);
    }
     */
    @JsonIgnore
    @GsrsApiAction(value= "_payload", serializeUrlOnly = true, view = BeanViews.Compact.class)
    public FieldResourceReference<String> getJsonPayload () {
        if(payload ==null){
            return null;
        }
        return FieldResourceReference.forField("/payload", ()->"ignored");

    }
    @JsonIgnore
    @GsrsApiAction(value= "_owner", serializeUrlOnly = true, view = BeanViews.Compact.class)
    public FieldResourceReference<String> getJsonOwner () {
        if(owner ==null){
            return null;
        }
        return FieldResourceReference.forField("/owner", ()->"ignored");

    }

    
    @JsonView(BeanViews.Compact.class)
    @JsonProperty("statistics")
    public Map getStatisticsForAPI () {
        try {
			return om.readValue(om.valueToTree(getStatistics())+"",Map.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
        return null;
    }
    
    public String getKeyMatching(String label){
        populateKeywordMapsIfNeeded();
        Keyword keyword = keywordLabelMap.get(label);
    	return keyword ==null ? null : keyword.getValue();
    }
    public boolean hasKey(String term){
        populateKeywordMapsIfNeeded();
        return keywordTermMap.containsKey(term);

    }


    public void addKeyword(Keyword keyword){
        Objects.requireNonNull(keyword);
        populateKeywordMapsIfNeeded();

        keys.add(keyword);
        keywordLabelMap.put(keyword.label, keyword);
        keywordTermMap.put(keyword.term, keyword);

    }

    private void populateKeywordMapsIfNeeded() {
        if(keywordLabelMap !=null) {
            return;
        }
        HashMap<String,Keyword> tKeywordLabelMap = new HashMap<>();
        HashMap<String,Keyword> tkeywordTermMap = new HashMap<>();

        
        for (Keyword k : keys) {
        	tKeywordLabelMap.put(k.label, k);
            tkeywordTermMap.put(k.term, k);
        }
        
        this.keywordLabelMap=tKeywordLabelMap;
        this.keywordTermMap=tkeywordTermMap;
        

    }

    //    @JsonView(BeanViews.Compact.class)
//    @JsonProperty("_statistics")
    public Statistics getStatistics(){
        //we persist only at the end so lets  check the in memory map first and only fetch from db if it's not in memory.
    	//TODO move this to controller
        SubstanceBulkLoadService loadService = StaticContextAccessor.getBean(SubstanceBulkLoadService.class);
    	Statistics stats= loadService.getStatisticsForJob(this);
    	if(stats !=null){
    	    return stats;
        }
        if(this.statistics!=null){
            try {
                return om.readValue(statistics, Statistics.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    @JsonIgnore
    public Date _getStartAsDate(){
    	return new Date(start);
    }
    
    @JsonIgnore
    public Date _getStoppedAsDate(){
    	if(stop==null)return null;
    	return new Date(stop);
    }
    
    @JsonIgnore
    public Long _getDurationAsMs(){
    	if(stop!=null){
    		return stop-start;
    	}
    	return TimeUtil.getCurrentTimeMillis()-start;
    }
    
    public String getName(){
    	if(payload!=null){
    		return "Import batch file \"" + payload.name + "\"";
    	}else{
    		return "Unnamed Batch";
    	}
    }

    @JsonIgnore
	public RecordPersister getPersister() {
    	RecordPersister rec = RecordPersister
				.getInstanceOfPersister(this
						.getKeyMatching(PERSISTER_KEYWORD));
		return rec;
	}

    @JsonIgnore
	public RecordExtractor getExtractor() {
		RecordExtractor rec = RecordExtractor
				.getInstanceOfExtractor(this
						.getKeyMatching(EXTRACTOR_KEYWORD));
		return rec;
	}


    @JsonIgnore
	public void setExtractor(RecordExtractorFactory extractor) {
		this.addKeyword(new Keyword(EXTRACTOR_KEYWORD, extractor.getExtractorName()));
	}
    
    @JsonIgnore
	public void setPersister(RecordPersisterFactory persister) {
		this.addKeyword(new Keyword(PERSISTER_KEYWORD, persister.getPersisterName()));
	}
    
    @PreUpdate
    @PrePersist
    private void updateTime(){
    	lastUpdate=new Date();
    }
}
