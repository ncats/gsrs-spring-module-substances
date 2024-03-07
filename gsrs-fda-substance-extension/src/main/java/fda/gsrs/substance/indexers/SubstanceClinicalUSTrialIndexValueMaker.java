package fda.gsrs.substance.indexers;

import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialsUSApi;
import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialUSDTO;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchResult;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class SubstanceClinicalUSTrialIndexValueMaker implements IndexValueMaker<Substance> {

	@Value("${gsrs.clinicaltrial.ivm.search.max.fetch:20000}")
	private Integer maxFetchSize;

	@Autowired
	public ClinicalTrialsUSApi clinicalTrialsUSApi;

	@Override
	public Class<Substance> getIndexedEntityClass() {
		return Substance.class;
	}
	
	@Override
	public boolean isExternal() {
		return true;
	} 

	private static final long[] countBuckets = new long[]{1,2,3,4,5,10,15,20,25,30,40,50,75,100,250,500,1000,2000,3000,5000};

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        try{
        	SearchRequest searchRequest = SearchRequest.builder().q("entity_link_substances:\"" + substance.uuid + "\"").top(maxFetchSize).simpleSearchOnly(true).build();

			SearchResult<ClinicalTrialUSDTO> searchResult = clinicalTrialsUSApi.search(searchRequest);

			List<ClinicalTrialUSDTO> ctusList = searchResult.getContent();

			if(ctusList==null) return;

			ctusList.forEach(ctus -> {
				if(ctus.getStatus()!=null) {
					consumer.accept(IndexableValue.simpleFacetStringValue("Clinical Trial US Status", ctus.getStatus()));
				}
			});
			long ctCount = ctusList.size();
			if (ctCount>0) {
				consumer.accept(IndexableValue.simpleFacetLongValue("Clinical Trial US Count", ctCount, countBuckets));
			}
		} catch(Exception e){
			log.warn("Exception occurred when cross indexing a substance with Clinical Trials US.", e);
		}
    }
}
