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

	@Autowired
	public ClinicalTrialUSApi clinicalTrialUSApi;

	@Override
	public Class<Substance> getIndexedEntityClass() {
		return Substance.class;
	}

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {

		System.out.println("I AM IN SIDE SubstanceClinicalUSTrialIndexValueMaker.createIndexableValues 1");
        try{

        	SearchRequest searchRequest = SearchRequest.builder().q("entity_link_substances:\"" + substance.uuid + "\"").top(1000000).simpleSearchOnly(true).build();
			SearchResult<ClinicalTrialUSDTO> searchResult = clinicalTrialUSApi.search(searchRequest);
			List<ClinicalTrialUSDTO> ctusList = searchResult.getContent();
			//substances may have more than one ctus and therefore multiple status values
			ctusList.forEach(ctus -> {
				System.out.println("I AM IN SIDE SubstanceClinicalUSTrialIndexValueMaker.createIndexableValues 2");

				consumer.accept(IndexableValue.simpleFacetStringValue("Clinical Trial US Number", ctus.getTrialNumber()));
				consumer.accept(IndexableValue.simpleFacetStringValue("Clinical Trial US Status", ctus.getStatus()));
			});
		} catch(Exception e){
			e.printStackTrace();
		}
    }

}
