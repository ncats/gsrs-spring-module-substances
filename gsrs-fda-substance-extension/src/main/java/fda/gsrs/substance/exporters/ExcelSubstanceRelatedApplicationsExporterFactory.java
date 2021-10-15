package fda.gsrs.substance.exporters;

import gsrs.springUtils.AutowireHelper;
import org.springframework.beans.factory.annotation.Autowired;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.exporters.Spreadsheet;
import ix.ginas.exporters.SpreadsheetFormat;
import ix.ginas.models.v1.Substance;

import gov.hhs.gsrs.applications.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

public class ExcelSubstanceRelatedApplicationsExporterFactory implements ExporterFactory{

    @Autowired
    public ApplicationsApi applicationsApi;

    OutputFormat format = new OutputFormat("appxlsx", "Spreadsheet File");
    String source = null;
    
    @Override
    public boolean supports(ExcelSubstanceRelatedApplicationsExporterFactory.Parameters params) {
        return params.getFormat().equals(format);
    }

    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return Collections.singleton(format);
    }

    @Override
    public Exporter<Substance> createNewExporter(OutputStream out, ExporterFactory.Parameters params) throws IOException {

        if (applicationsApi == null) {
            AutowireHelper.getInstance().autowire(this);
        }

    	return new ExcelSubstanceRelatedApplicationsExporter(out, applicationsApi);
    }

}
