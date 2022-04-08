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

		System.out.println("I AM INSIDE SubstanceClinicalUSTrialIndexValueMaker.createIndexableValues 1");
        try{

			System.out.println("I AM INSIDE SubstanceClinicalUSTrialIndexValueMaker.createIndexableValues 2");
        	SearchRequest searchRequest = SearchRequest.builder().q("root_clinicalTrialUSDrug_substanceKey:\"^" + substance.uuid + "$\"").top(1000000).simpleSearchOnly(true).build();
			System.out.println("I AM INSIDE SubstanceClinicalUSTrialIndexValueMaker.createIndexableValues 3");

			SearchResult<ClinicalTrialUSDTO> searchResult = clinicalTrialUSApi.search(searchRequest);
			System.out.println("I AM INSIDE SubstanceClinicalUSTrialIndexValueMaker.createIndexableValues 4");

			List<ClinicalTrialUSDTO> ctusList = searchResult.getContent();
			//substances may have more than one ctus and therefore multiple status values
			ctusList.forEach(ctus -> {
				System.out.println("I AM INSIDE SubstanceClinicalUSTrialIndexValueMaker.createIndexableValues 5");
				if(ctus.getTrialNumber()!=null) {
					consumer.accept(IndexableValue.simpleFacetStringValue("Clinical Trial US Number", ctus.getTrialNumber()));
				}
				if(ctus.getStatus()!=null) {
					consumer.accept(IndexableValue.simpleFacetStringValue("Clinical Trial US Status", ctus.getStatus()));
				}
				System.out.println("I AM INSIDE SubstanceClinicalUSTrialIndexValueMaker.createIndexableValues 6");
			});
			long ctCount = ctusList.size();
			if (ctCount>0) {
				consumer.accept(IndexableValue.simpleFacetLongValue("Clinical Trial US Count", ctCount, countBuckets));
			}
			System.out.println("I AM INSIDE SubstanceClinicalUSTrialIndexValueMaker.createIndexableValues 7");
		// } catch(Exception e){
			// e.printStackTrace();
			// trying this to see if can get errors  that seem to happen on load
			} catch(Throwable t){
			t.printStackTrace();

		}
    }

}
