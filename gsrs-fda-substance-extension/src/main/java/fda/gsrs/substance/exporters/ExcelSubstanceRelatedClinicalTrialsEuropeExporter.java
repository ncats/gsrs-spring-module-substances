package fda.gsrs.substance.exporters;

import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialEuropeDTO;
import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialsEuropeApi;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchRequest;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate.SearchResult;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
@Slf4j
public class ExcelSubstanceRelatedClinicalTrialsEuropeExporter implements Exporter<Substance> {

	private OutputStream os;
	private ClinicalTrialsEuropeApi clinicalTrialsEuropeApi;
	private ClinicalTrialEuropeDTOExporter clinicalTrialEuropeDTOExporter;

	public ExcelSubstanceRelatedClinicalTrialsEuropeExporter(OutputStream os, ClinicalTrialsEuropeApi clinicalTrialsEuropeApi){
		this.os = os; //probably use a buffer instead
		this.clinicalTrialsEuropeApi = clinicalTrialsEuropeApi;

		try {
			//Export Application Factory
			OutputFormat format = new OutputFormat("xlsx", "SRS ClinicalTrialEurope Data");
			ExporterFactory.Parameters params = new ExportParameters(format, true);
			ClinicalTrialEuropeDTOExporterFactory factory = new ClinicalTrialEuropeDTOExporterFactory();
			this.clinicalTrialEuropeDTOExporter = factory.createNewExporter(os, params);
		} catch (Exception ex) {
			log.error("Exception instantiating ExcelSubstanceRelatedClinicalTrialsEuropeExporter.", ex);
		}
	}

	private void exportClinicalTrialEurope(ClinicalTrialEuropeDTO clinicalTrialEuropeDTO){
		try {
			this.clinicalTrialEuropeDTOExporter.export(clinicalTrialEuropeDTO);
		}catch (Exception ex) {
			log.error("Exception in method exportClinicalTrialEurope.", ex);
		}
	}

	@Override
	public void export(Substance s) throws IOException{

		SearchResult<ClinicalTrialEuropeDTO> result = getClinicalTrialEuropeRelatedToSubstance(s);

		List<ClinicalTrialEuropeDTO> trialList = result.getContent();

		for (ClinicalTrialEuropeDTO trial: trialList) {
			exportClinicalTrialEurope(trial);
		}
	}

	public SearchResult<ClinicalTrialEuropeDTO> getClinicalTrialEuropeRelatedToSubstance(Substance s) {
		try {
			SearchRequest searchRequest = SearchRequest.builder().q("entity_link_substances:\"" + s.uuid + "\"").top(1000000).simpleSearchOnly(true).build();
			return clinicalTrialsEuropeApi.search(searchRequest);
		} catch (Exception ex) {
			log.error("Exception in method getClinicalTrialEuropeRelatedToSubstance.", ex);
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		try {
			if (clinicalTrialEuropeDTOExporter != null) {
				clinicalTrialEuropeDTOExporter.close();
			}
		} catch (Exception ex) {
			log.error("Exception closing clinicalTrialEuropeDTOExporter.", ex);
			throw ex;
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

		private String username;

		@Override
		public OutputFormat getFormat() {
			return format;
		}

		@Override
		public boolean publicOnly() {
			return publicOnly;
		}

		public void setUsername(String user){
			this.username=user;
		}

		public String getUsername(){
			return this.username;
		}
	}
}
