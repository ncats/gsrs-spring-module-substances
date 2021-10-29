package fda.gsrs.substance.indexers;

import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate;
import gsrs.springUtils.AutowireHelper;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;

import gov.hhs.gsrs.applications.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SubstanceApplicationIndexValueMaker implements IndexValueMaker<Substance> {

	@Autowired
	public ApplicationsApi applicationsApi;

	@Override
	public Class<Substance> getIndexedEntityClass() {
		return Substance.class;
	}

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {

        try{
        	AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest searchRequest = AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest.builder().q("entity_link_substances:" + substance.uuid).top(Integer.MAX_VALUE).simpleSearchOnly(true).build();
			AbstractLegacySearchGsrsEntityRestTemplate.SearchResult<ApplicationAllDTO> searchResult = applicationsApi.search(searchRequest);
			List<ApplicationAllDTO> appList = searchResult.getContent();

			//substances may have more than one application and therefore multiple status values
			appList.forEach(application -> {
				if (application.getAppStatus() !=null) {
					consumer.accept(IndexableValue.simpleFacetStringValue("Application Status", application.getAppStatus()));
				}
				if (application.getCenter()!=null) {
					consumer.accept(IndexableValue.simpleFacetStringValue("Application Center", application.getCenter()));
				}
				if (application.getAppType() !=null) {
					consumer.accept(IndexableValue.simpleFacetStringValue("Application Type", application.getAppType()));
				}
			});

		} catch(Exception e){
			e.printStackTrace();
		}
    }

}
