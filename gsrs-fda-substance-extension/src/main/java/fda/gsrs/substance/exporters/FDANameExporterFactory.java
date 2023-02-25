package fda.gsrs.substance.exporters;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

/**
 * Created by VenkataSaiRa.Chavali on 3/10/2017.
 */
public class FDANameExporterFactory implements ExporterFactory<Substance> {

    OutputFormat format = new OutputFormat("names.txt", "Names only, tab-delimited (.txt)");

    private boolean includeBdnum = false;

    @Autowired
    private SubstanceRepository substanceRepository;

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
        return new FDANameExporter(substanceRepository, out, includeBdnum);
    }

    public boolean getIncludeBdnum(boolean includeBdnum) {
        return this.includeBdnum;
    }

    public void setIncludeBdnum(boolean includeBdnum) {
        this.includeBdnum = includeBdnum;
    }

}
