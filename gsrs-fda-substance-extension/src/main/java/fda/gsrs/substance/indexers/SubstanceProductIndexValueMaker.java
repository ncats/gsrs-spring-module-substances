package fda.gsrs.substance.indexers;

import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchResult;
import gsrs.springUtils.AutowireHelper;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;

import gov.hhs.gsrs.products.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SubstanceProductIndexValueMaker implements IndexValueMaker<Substance> {

	@Autowired
	public ProductsApi productsApi;

	@Override
	public Class<Substance> getIndexedEntityClass() {
		return Substance.class;
	}

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {

        try{
        	SearchRequest searchRequest = SearchRequest.builder().q("entity_link_substances:\"" + substance.uuid + "\"").top(1000000).simpleSearchOnly(true).build();
			SearchResult<ProductMainAllDTO> searchResult = productsApi.search(searchRequest);
			List<ProductMainAllDTO> prodList = searchResult.getContent();

			//substances may have more than one application and therefore multiple status values
			prodList.forEach(product -> {
				if(product.getProductType() !=null){
					consumer.accept(IndexableValue.simpleFacetStringValue("Product Type", product.getProductType()));
				}

				// Get Ingredient List
				product.getProductIngredientAllList().forEach(ingredient -> {
					if(ingredient.getIngredientType() !=null){
						consumer.accept(IndexableValue.simpleFacetStringValue("Product Ingredient Type", ingredient.getIngredientType()));
					}
					if(ingredient.getDosageFormName() !=null){
						consumer.accept(IndexableValue.simpleFacetStringValue("Product Dosage Form", ingredient.getDosageFormName()));
					}
				});
			});

		} catch(Exception e){
			e.printStackTrace();
		}
    }

}
