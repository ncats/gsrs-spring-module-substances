package fda.gsrs.substance.exporters;

import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

/**
 * Created by VenkataSaiRa.Chavali on 3/10/2017.
 */
public class FDACodeExporterFactory implements ExporterFactory<Substance>{

    OutputFormat format = new OutputFormat("codes.txt", "Names Report (txt) File");

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
        // if(params.shouldCompress()) {
        return new FDACodeExporter(out, !params.publicOnly());
//        }
//        return new JsonExporter(out);
    }
}
