package gsrs.module.substance.exporters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ix.ginas.exporters.*;
import ix.ginas.models.v1.Substance;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Egor Puzanov.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class XslExporterFactory implements ExporterFactory<Substance> {

    private OutputFormat format = new OutputFormat("gsrsp", "Json Portable Export (gsrsp) File");
    private String templateFile = "";
    private String header = "";
    private String footer = "";
    private String delimiter = "";
    private boolean shouldCompress = false;

    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setShouldCompress(boolean shouldCompress) {
        this.shouldCompress = shouldCompress;
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
    public Exporter<Substance> createNewExporter(OutputStream out, Parameters params) throws IOException {
        if(shouldCompress) {
            try {
                return new XslExporter(new GZIPOutputStream(out), templateFile, header, footer, delimiter);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        try {
            return new XslExporter(out, templateFile, header, footer, delimiter);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        return parameters;
    }
}
