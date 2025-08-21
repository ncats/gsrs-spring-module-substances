package fda.gsrs.substance.indexers;

import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialsEuropeApi;
import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialEuropeDTO;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchResult;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
@Slf4j
public class SubstanceClinicalEuropeTrialIndexValueMaker implements IndexValueMaker<Substance> {

	@Value("${gsrs.clinicaltrial.ivm.search.max.fetch:20000}")
	private Integer maxFetchSize;

	@Autowired
	public ClinicalTrialsEuropeApi clinicalTrialEuropeApi;

	@Override
	public Class<Substance> getIndexedEntityClass() {
		return Substance.class;
	}
	
	@Override
	public Set<String> getTags(){
		return Util.toSet("external","clinicalEuropeTrial");		
	} 
	
	@Override	
	public Set<String> getFieldNames(){
		return Util.toSet("Clinical Trial Europe Status","Clinical Trial Europe Count");
	}

	private static final long[] countBuckets = new long[]{1,2,3,4,5,10,15,20,25,30,40,50,75,100,250,500,1000,2000,3000,5000};

	@Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        try{
        	SearchRequest searchRequest = SearchRequest.builder().q("entity_link_substances:\"" + substance.uuid + "\"").top(maxFetchSize).simpleSearchOnly(true).build();
			SearchResult<ClinicalTrialEuropeDTO> searchResult = clinicalTrialEuropeApi.search(searchRequest);

			List<ClinicalTrialEuropeDTO> cteuList = searchResult.getContent();

			if(cteuList==null) return;

			cteuList.forEach(cteu -> {
				if(cteu.getTrialStatus()!=null) {
					consumer.accept(IndexableValue.simpleFacetStringValue("Clinical Trial Europe Status", cteu.getTrialStatus()));
				}
			});
			long ctCount = cteuList.size();
			if (ctCount>0) {
				consumer.accept(IndexableValue.simpleFacetLongValue("Clinical Trial Europe Count", ctCount, countBuckets));
			}
		} catch(Exception e){
			log.warn("Exception occurred when cross indexing a substance with Clinical Trials Europe.", e);
		}
	}
}
