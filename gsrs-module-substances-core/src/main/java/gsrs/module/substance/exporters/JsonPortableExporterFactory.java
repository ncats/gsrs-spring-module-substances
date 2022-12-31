package gsrs.module.substance.exporters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by epuzanov on 8/30/21.
 */
@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonPortableExporterFactory implements ExporterFactory{

    private OutputFormat format = new OutputFormat("gsrsp", "Json Portable Export (gsrsp) File");
    private List<String> fieldsToRemove  = Arrays.asList("_name","_nameHTML","_formulaHTML","_approvalIDDisplay","_isClassification","_self","self","approvalID","approved","approvedBy","changeReason","created","createdBy","lastEdited","lastEditedBy","deprecated","uuid","refuuid","originatorUuid","linkingID","id","documentDate","status","version");
    private String originBase = null;

    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }

    public void setFieldsToRemove(Map<String, String> fieldsToRemove) {
        this.fieldsToRemove = (List<String>) fieldsToRemove.values();
    }

    public void setOriginBase(String originBase) {
        this.originBase = originBase;
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
        JsonNode detailedParameters = params.detailedParameters();
        return new JsonPortableExporter(new GZIPOutputStream(out), params, fieldsToRemove, originBase);
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        return generateSchemaNode("GSRS Portable File Exporter Parameters", parameters);
    }
}
