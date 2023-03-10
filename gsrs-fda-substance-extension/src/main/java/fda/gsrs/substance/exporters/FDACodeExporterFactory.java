package fda.gsrs.substance.exporters;

import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

public class FDACodeExporterFactory implements ExporterFactory<Substance>{

    // To set include e.g. { parameters: {"primaryCodeSystem": "BDNUM"  }, ...}  in your factory configuration
    // the setter will be called automatically by gsrs code.
    private String primaryCodeSystem;

    OutputFormat format = new OutputFormat("codes.txt", "Codes only, tab-delimited (.txt)");

    @Override
    public boolean supports(ExporterFactory.Parameters params) {
        return params.getFormat().equals(format);
    }

    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return Collections.singleton(format);
    }

    @Override
    public Exporter<Substance> createNewExporter(OutputStream out, ExporterFactory.Parameters params) throws IOException {
        return new FDACodeExporter(out, this.primaryCodeSystem);
    }

    public void setPrimaryCodeSystem(String primaryCodeSystem) {
        this.primaryCodeSystem = primaryCodeSystem;
    }

    public String getPrimaryCodeSystem() {
        return this.primaryCodeSystem;
    }
}
