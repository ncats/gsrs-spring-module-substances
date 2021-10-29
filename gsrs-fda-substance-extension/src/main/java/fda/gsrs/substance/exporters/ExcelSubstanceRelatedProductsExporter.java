package fda.gsrs.substance.exporters;

import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchResult;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;

import gov.hhs.gsrs.products.api.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

public class ExcelSubstanceRelatedProductsExporter implements Exporter<Substance> {
	private OutputStream os;
	private ProductsApi productsApi;
	private ProductAllDTOExporter prodExporter;

	public ExcelSubstanceRelatedProductsExporter(OutputStream os, ProductsApi productsApi){
		this.os = os; //probably use a buffer instead
		this.productsApi = productsApi;

		try {
			//Export Application Factory
			OutputFormat format = new OutputFormat("xlsx", "SRS Product Data");
			ExporterFactory.Parameters params = new ExportParameters(format, true);
			ProductAllDTOExporterFactory factory = new ProductAllDTOExporterFactory();
			this.prodExporter = factory.createNewExporter(os, params);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void exportProduct(ProductMainAllDTO app){
		try {
			this.prodExporter.export(app);
		}catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void export(Substance s) throws IOException{

		SearchResult<ProductMainAllDTO> result = getAllProductsRelatedToSubstance(s);
		List<ProductMainAllDTO> appList = result.getContent();

		for (ProductMainAllDTO app: appList) {
			exportProduct(app);
		}
	}

	public SearchResult<ProductMainAllDTO> getAllProductsRelatedToSubstance(Substance s) {
		try {
			SearchRequest searchRequest = SearchRequest.builder().q("entity_link_substances:\"" + s.uuid + "\"").top(Integer.MAX_VALUE).simpleSearchOnly(true).build();
			return productsApi.search(searchRequest);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		try {
			if (prodExporter != null) {
				prodExporter.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static class ExportParameters implements ExporterFactory.Parameters {
		private final OutputFormat format;

		private final boolean publicOnly;

		ExportParameters(OutputFormat format, boolean publicOnly) {
			Objects.requireNonNull(format);
			this.format = format;
			this.publicOnly = publicOnly;
		}

		@Override
		public OutputFormat getFormat() {
			return format;
		}

		@Override
		public boolean publicOnly() {
			return publicOnly;
		}
	}
}
