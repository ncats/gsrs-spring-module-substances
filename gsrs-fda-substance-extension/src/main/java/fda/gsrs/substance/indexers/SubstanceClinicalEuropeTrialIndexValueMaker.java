package fda.gsrs.substance.indexers;

import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialsEuropeApi;
import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialEuropeDTO;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchResult;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.function.Consumer;

public class SubstanceClinicalEuropeTrialIndexValueMaker implements IndexValueMaker<Substance> {

	@Autowired
	public ClinicalTrialsEuropeApi clinicalTrialEuropeApi;

	@Override
	public Class<Substance> getIndexedEntityClass() {
		return Substance.class;
	}

	private static final long[] countBuckets = new long[]{1,2,3,4,5,10,15,20,25,30,40,50,75,100,250,500,1000,2000,3000,5000};
    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        try{
        	SearchRequest searchRequest = SearchRequest.builder().q("entity_link_substances:\"" + substance.uuid + "\"").top(1000000).simpleSearchOnly(true).build();
			SearchResult<ClinicalTrialEuropeDTO> searchResult = clinicalTrialEuropeApi.search(searchRequest);

			List<ClinicalTrialEuropeDTO> cteuList = searchResult.getContent();

			if(cteuList==null) return;

			cteuList.forEach(cteu -> {
				if(cteu.getTrialStatus()!=null) {
					consumer.accept(IndexableValue.simpleFacetStringValue("Clinical Trial EU Status", cteu.getTrialStatus()));
				}
			});
			long ctCount = cteuList.size();
			if (ctCount>0) {
				consumer.accept(IndexableValue.simpleFacetLongValue("Clinical Trial Europe Count", ctCount, countBuckets));
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
