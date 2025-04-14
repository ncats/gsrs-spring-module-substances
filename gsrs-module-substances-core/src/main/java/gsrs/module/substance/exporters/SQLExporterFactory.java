package gsrs.module.substance.exporters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Egor Puzanov.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class SQLExporterFactory implements ExporterFactory<Substance> {

    private OutputFormat format = new OutputFormat("custom.xlsx", "Custom Report (xlsx) File");
    private String stringConverter = null;
    private List<Map<String, Object>> files;

    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }

    public void setStringConverter(String stringConverter) {
        this.stringConverter = stringConverter;
    }

    public void setFiles(Map<Integer, Map<String, Object>> m) {
        this.files = (List<Map<String, Object>>) m.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e->e.getValue())
            .collect(Collectors.toList());
    }

    @Override
    public boolean supports(Parameters params) {
        return params.getFormat().equals(format);
    }

    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return Collections.singleton(format);
    }

    @Override
    public SQLExporter createNewExporter(OutputStream out, Parameters params) throws IOException {
        SQLExporter.Builder builder = new SQLExporter.Builder(out, format.getExtension(), stringConverter);
        for (Map<String, Object> file : files) {
            builder = builder.addFile(file);
        }
        return builder.build();
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        return parameters;
    }
}
