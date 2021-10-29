package fda.gsrs.substance.exporters;

import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchResult;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;

import gov.hhs.gsrs.applications.api.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

public class ExcelSubstanceRelatedApplicationsExporter implements Exporter<Substance> {
	private OutputStream os;
	private ApplicationsApi applicationsApi;
	private ApplicationAllDTOExporter appExporter;

	public ExcelSubstanceRelatedApplicationsExporter(OutputStream os, ApplicationsApi applicationsApi){
		this.os = os; //probably use a buffer instead
		this.applicationsApi = applicationsApi;

		try {
			//Export Application Factory
			OutputFormat format = new OutputFormat("xlsx", "SRS Application Data");
			ExporterFactory.Parameters params = new ExportParameters(format, true);
			ApplicationAllDTOExporterFactory factory = new ApplicationAllDTOExporterFactory();
			this.appExporter = factory.createNewExporter(os, params);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void exportApp(ApplicationAllDTO app){
		try {
			this.appExporter.export(app);
		}catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void export(Substance s) throws IOException{

		SearchResult<ApplicationAllDTO> result = getAllApplicationsRelatedToSubstance(s);
		List<ApplicationAllDTO> appList = result.getContent();

		for (ApplicationAllDTO app: appList) {
			exportApp(app);
		}
	}

	public SearchResult<ApplicationAllDTO> getAllApplicationsRelatedToSubstance(Substance s) {
		try {
			SearchRequest searchRequest = SearchRequest.builder().q("entity_link_substances:\"" + s.uuid + "\"").top(Integer.MAX_VALUE).simpleSearchOnly(true).build();
			return applicationsApi.search(searchRequest);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		if (appExporter != null) {
			appExporter.close();
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
