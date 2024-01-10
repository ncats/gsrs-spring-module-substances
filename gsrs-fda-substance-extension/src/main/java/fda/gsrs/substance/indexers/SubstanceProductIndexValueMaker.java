package fda.gsrs.substance.indexers;

import gov.hhs.gsrs.products.api.*;

import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchResult;
import gsrs.springUtils.AutowireHelper;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.Code;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SubstanceProductIndexValueMaker implements IndexValueMaker<Substance> {

    @Value("${gsrs.product.ivm.search.max.fetch:20000}")
    private Integer maxFetchSize;

    @Autowired
    public ProductsApi productsApi;

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {

        try {
            String substanceKey = null;
            for (int i = 0; i < substance.codes.size(); i++) {
                Code cd = substance.codes.get(i);
                if (cd != null) {
                    if ((cd.codeSystem != null) && (cd.type != null)) {
                        if ((cd.codeSystem.equalsIgnoreCase("BDNUM")) && (cd.type.equalsIgnoreCase("PRIMARY"))) {
                            substanceKey = cd.code;
                        }
                    }
                }
            }

            SearchRequest searchRequest = SearchRequest.builder().q("entity_link_substances:\"" + substance.uuid + "\"").top(maxFetchSize).simpleSearchOnly(true).build();
            SearchResult<ProductDTO> searchResult = productsApi.search(searchRequest);
            List<ProductDTO> prodList = searchResult.getContent();

            //substances may have more than one product and therefore multiple values
            for (int j = 0; j < prodList.size(); j++) {
                ProductDTO product = prodList.get(j);

                // Get Product Provenances
                product.getProductProvenances().forEach(productProv -> {
                    if (productProv.getProductType() != null) {
                        consumer.accept(IndexableValue.simpleFacetStringValue("Product Type", productProv.getProductType()));
                    }
                });

                // Get Product Manufacture Items
                for (int k = 0; k < product.getProductManufactureItems().size(); k++) {
                    ProductManufactureItemDTO productManuItem = product.getProductManufactureItems().get(k);

                    if (productManuItem.getDosageForm() != null) {
                        consumer.accept(IndexableValue.simpleFacetStringValue("Product Dosage Form", productManuItem.getDosageForm()));
                    }

                    for (int l = 0; l < productManuItem.getProductLots().size(); l++) {
                        ProductLotDTO productLot = productManuItem.getProductLots().get(l);

                        for (int m = 0; m < productLot.getProductIngredients().size(); m++) {
                            ProductIngredientDTO productIngred = productLot.getProductIngredients().get(m);

                            //skip ingredients that aren't this ingredient
                            if (substanceKey != null && !substanceKey.equals(productIngred.getSubstanceKey())) {
                                return;
                            }

                            if (productIngred.getIngredientType() != null) {
                                consumer.accept(IndexableValue.simpleFacetStringValue("Product Ingredient Type", productIngred.getIngredientType()));
                            }
                        } // for loop Product Ingredient
                    } // for loop Product Lot
                }    // for loop Product Manufacture Item
            } // for loop Product List

				/*
				// Get Ingredient List
				product.getProductIngredientAllList().forEach(ingredient -> {
					//skip ingredients that aren't this ingredient
					if(!substance.getUuid().toString().equals(ingredient.getSubstanceUuid())) {
						return;
					}

					if(ingredient.getIngredientType() !=null){
						consumer.accept(IndexableValue.simpleFacetStringValue("Product Ingredient Type", ingredient.getIngredientType()));
					}
					if(ingredient.getDosageFormName() !=null){
						consumer.accept(IndexableValue.simpleFacetStringValue("Product Dosage Form", ingredient.getDosageFormName()));
					}
				});
			}); */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
