package fda.gsrs.substance.exporters;

import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialsUSApi;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

public class ExcelSubstanceRelatedClinicalTrialsUSExporterFactory implements ExporterFactory{

    @Autowired
    public ClinicalTrialsUSApi clinicalTrialsUSApi;

    OutputFormat format = new OutputFormat("ctxlsx", "Spreadsheet File");
    String source = null;
    
    @Override
    public boolean supports(Parameters params) {
        return params.getFormat().equals(format);
    }

    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return Collections.singleton(format);
    }

    @Override
    public Exporter<Substance> createNewExporter(OutputStream out, Parameters params) throws IOException {

        if (clinicalTrialsUSApi == null) {
            AutowireHelper.getInstance().autowire(this);
        }

    	return new ExcelSubstanceRelatedClinicalTrialsUSExporter(out, clinicalTrialsUSApi);
    }

}
