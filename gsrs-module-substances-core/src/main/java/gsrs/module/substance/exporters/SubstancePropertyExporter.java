package gsrs.module.substance.exporters;

import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class SubstancePropertyExporter implements Exporter<Substance> {

    private final BufferedWriter out;
    private ExporterFactory.Parameters exportParameters;

    private final String FIELD_DELIMITER = "\t";

    private boolean onlyDefining = false;

    public boolean isOnlyDefining() {
        return onlyDefining;
    }

    public void setOnlyDefining(boolean onlyDefining) {
        this.onlyDefining = onlyDefining;
    }

    public SubstancePropertyExporter(OutputStream outputStream, ExporterFactory.Parameters parameters) {
        this.out = new BufferedWriter(new OutputStreamWriter(outputStream));
        this.exportParameters = parameters;
    }

    @Override
    public void export(Substance obj) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(obj);
        List<Property> exportables = obj.properties.stream()
                .filter(p-> p.isDefining() || !onlyDefining)
                .collect(Collectors.toList());
        StringBuilder outputBuilder = new StringBuilder();
        StringBuilder lineStart = new StringBuilder();
        outputBuilder.append(obj.getUuid());
        lineStart.append(obj.getUuid());
        outputBuilder.append(FIELD_DELIMITER);
        lineStart.append(FIELD_DELIMITER);
        outputBuilder.append(obj.getName());
        lineStart.append(obj.getName());
        outputBuilder.append(FIELD_DELIMITER);
        lineStart.append(FIELD_DELIMITER);
        outputBuilder.append(exportables == null ? "0" : exportables.size());
        lineStart.append(exportables == null ? "0" : exportables.size());
        outputBuilder.append(FIELD_DELIMITER);
        lineStart.append(FIELD_DELIMITER);
        AtomicInteger line = new AtomicInteger(0);

        exportables.forEach(p->{
            if( line.incrementAndGet() > 1) {
                outputBuilder.append(lineStart);
            }
            outputBuilder.append(p.getName());
            outputBuilder.append(FIELD_DELIMITER);
            outputBuilder.append(p.getPropertyType());
            outputBuilder.append(FIELD_DELIMITER);
            outputBuilder.append(p.isDefining());
            outputBuilder.append(FIELD_DELIMITER);
            if( p.getValue() != null) {
                if( p.getValue().average != null ) {
                    outputBuilder.append(p.getValue().average);
                    outputBuilder.append(FIELD_DELIMITER);
                    outputBuilder.append(p.getValue().units);
                } else if( p.getValue().low != null) {
                    outputBuilder.append(p.getValue().low );
                    outputBuilder.append( " - ");
                    if( p.getValue().high != null ){
                        outputBuilder.append(p.getValue().high);
                    }
                    outputBuilder.append(p.getValue().units);
                } else if( p.getValue().nonNumericValue != null) {
                    outputBuilder.append(p.getValue().nonNumericValue);
                }
            }
            outputBuilder.append("\n");
        });
        out.write(outputBuilder.toString());
        out.newLine();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
