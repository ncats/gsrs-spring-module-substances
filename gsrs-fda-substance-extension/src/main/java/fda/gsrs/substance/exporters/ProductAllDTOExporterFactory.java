package fda.gsrs.substance.exporters;

import gsrs.module.substance.SubstanceEntityService;
import gsrs.springUtils.AutowireHelper;
import org.springframework.beans.factory.annotation.Autowired;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.exporters.Spreadsheet;
import ix.ginas.exporters.SpreadsheetFormat;
import gsrs.module.substance.controllers.SubstanceController;
import ix.ginas.models.v1.Substance;

import gov.hhs.gsrs.products.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public class ProductAllDTOExporterFactory implements ExporterFactory {

	@Autowired
	public SubstanceEntityService substanceEntityService;

	private static final Set<OutputFormat> FORMATS;

	static {
		Set<OutputFormat> set = new LinkedHashSet<>();
		set.add(SpreadsheetFormat.TSV);
		set.add(SpreadsheetFormat.CSV);
		set.add(SpreadsheetFormat.XLSX);

		FORMATS = Collections.unmodifiableSet(set);
	}

	@Override
	public Set<OutputFormat> getSupportedFormats() {
		return FORMATS;
	}

	@Override
	public boolean supports(Parameters params) {
		return params.getFormat() instanceof SpreadsheetFormat;
	}

	@Override
	public ProductAllDTOExporter createNewExporter(OutputStream out, Parameters params) throws IOException {

		if (substanceEntityService == null) {
			AutowireHelper.getInstance().autowire(this);
		}

		SpreadsheetFormat format = SpreadsheetFormat.XLSX;
		Spreadsheet spreadsheet = format.createSpreadsheet(out);

		ProductAllDTOExporter.Builder builder = new ProductAllDTOExporter.Builder(spreadsheet);
		configure(builder, params);
		
		return builder.build(substanceEntityService);
	}

	protected void configure(ProductAllDTOExporter.Builder builder, Parameters params) {
		builder.includePublicDataOnly(params.publicOnly());
	}

}
