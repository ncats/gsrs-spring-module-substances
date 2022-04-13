package fda.gsrs.substance.indexers;

import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialUSApi;
import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialUSDTO;
import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialUSDrugDTO;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchResult;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SubstanceClinicalUSTrialIndexValueMaker implements IndexValueMaker<Substance> {
	// sponsor, conditions, status, study type.
	//
	@Autowired
	public ClinicalTrialUSApi clinicalTrialUSApi;

	@Override
	public Class<Substance> getIndexedEntityClass() {
		return Substance.class;
	}

	private static long[] countBuckets = new long[]{1,2,3,4,5,10,15,20,25,30,40,50,75,100,250,500,1000,2000,3000,5000};

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        try{
        	SearchRequest searchRequest = SearchRequest.builder().q("root_clinicalTrialUSDrug_substanceKey:\"^" + substance.uuid + "$\"").top(1000000).simpleSearchOnly(true).build();

			SearchResult<ClinicalTrialUSDTO> searchResult = clinicalTrialUSApi.search(searchRequest);

			List<ClinicalTrialUSDTO> ctusList = searchResult.getContent();
			//substances may have more than one ctus and therefore multiple status values
			ctusList.forEach(ctus -> {
				if(ctus.getTrialNumber()!=null) {
					consumer.accept(IndexableValue.simpleFacetStringValue("Clinical Trial US Number", ctus.getTrialNumber()));
				}
				if(ctus.getStatus()!=null) {
					consumer.accept(IndexableValue.simpleFacetStringValue("Clinical Trial US Status", ctus.getStatus()));
				}
			});
			long ctCount = ctusList.size();
			if (ctCount>0) {
				consumer.accept(IndexableValue.simpleFacetLongValue("Clinical Trial US Count", ctCount, countBuckets));
			}
		} catch(Exception e){
			e.printStackTrace();
		}
    }
}
