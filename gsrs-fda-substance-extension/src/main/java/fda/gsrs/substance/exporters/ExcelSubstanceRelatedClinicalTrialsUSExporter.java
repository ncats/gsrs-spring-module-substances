package fda.gsrs.substance.exporters;

import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialUSDTO;
import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialsUSApi;
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
public class ExcelSubstanceRelatedClinicalTrialsUSExporter implements Exporter<Substance> {

	private OutputStream os;
	private ClinicalTrialsUSApi clinicalTrialsUSApi;
	private ClinicalTrialUSDTOExporter clinicalTrialUSDTOExporter;

	public ExcelSubstanceRelatedClinicalTrialsUSExporter(OutputStream os, ClinicalTrialsUSApi clinicalTrialsUSApi){
		this.os = os; //probably use a buffer instead
		this.clinicalTrialsUSApi = clinicalTrialsUSApi;

		try {
			//Export Application Factory
			OutputFormat format = new OutputFormat("xlsx", "SRS ClinicalTrialUS Data");
			ExporterFactory.Parameters params = new ExportParameters(format, true);
			ClinicalTrialUSDTOExporterFactory factory = new ClinicalTrialUSDTOExporterFactory();
			this.clinicalTrialUSDTOExporter = factory.createNewExporter(os, params);

		} catch (Exception ex) {
			log.error("Exception instantiating ExcelSubstanceRelatedClinicalTrialsUSExporter.", ex);
		}
	}

	private void exportClinicalTrialUS(ClinicalTrialUSDTO clinicalTrialUSDTO){
		try {
			this.clinicalTrialUSDTOExporter.export(clinicalTrialUSDTO);
		}catch (Exception ex) {
			log.error("Exception in method exportClinicalTrialUS.", ex);
		}
	}

	@Override
	public void export(Substance s) throws IOException{

		SearchResult<ClinicalTrialUSDTO> result = getClinicalTrialsUSRelatedToSubstance(s);

		List<ClinicalTrialUSDTO> trialList = result.getContent();

		for (ClinicalTrialUSDTO trial: trialList) {
			exportClinicalTrialUS(trial);
		}
	}

	public SearchResult<ClinicalTrialUSDTO> getClinicalTrialsUSRelatedToSubstance(Substance s) {
		try {
			SearchRequest searchRequest = SearchRequest.builder().q("entity_link_substances:\"" + s.uuid + "\"").top(1000000).simpleSearchOnly(true).build();
			return clinicalTrialsUSApi.search(searchRequest);
		} catch (Exception ex) {
			log.error("Exception in method getClinicalTrialsUSRelatedToSubstance", ex);
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		try {
			if (clinicalTrialUSDTOExporter != null) {
				clinicalTrialUSDTOExporter.close();
			}
		} catch (Exception ex) {
			log.error("Exception closing clinicalTrialUSDTOExporter.", ex);
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
