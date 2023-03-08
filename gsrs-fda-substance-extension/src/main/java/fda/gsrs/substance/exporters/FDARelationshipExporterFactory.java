package fda.gsrs.substance.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

public class FDARelationshipExporterFactory implements ExporterFactory<Substance> {
    public static final String AMOUNT_DATA_PARAMETERS ="showAmountDataFields";
    public static final String PRIMARY_CODE_SYSTEM_PARAMETERS ="showPrimaryCodeSystemField";

    private String approvalIDName ="APPROVAL_ID";

    OutputFormat format = new OutputFormat("relationships.txt", "Relationships, tab-delimited (rel.txt)");

    private String primaryCodeSystem = null;

    @Autowired
    private SubstanceRepository substanceRepository;

    @Override
    public boolean supports(Parameters params) {
        return params.getFormat().equals(format);
    }

    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return Collections.singleton(format);
    }

    @Override
    public Exporter<Substance> createNewExporter(OutputStream out, ExporterFactory.Parameters params) throws IOException {
        return new FDARelationshipExporter(substanceRepository, out, params, this.primaryCodeSystem);
    }

    public String getPrimaryCodeSystem(boolean primaryCodeSystem) {
        return this.primaryCodeSystem;
    }

    public void setPrimaryCodeSystem(String primaryCodeSystem) {
        this.primaryCodeSystem = primaryCodeSystem;
    }


    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        ObjectNode amountDataNode = JsonNodeFactory.instance.objectNode();
        amountDataNode.put("type", "boolean");
        amountDataNode.put("title", "Include relationship amount fields?");
        amountDataNode.put("comments", "Include relationship amount fields?");
        parameters.set(AMOUNT_DATA_PARAMETERS, amountDataNode);

        ObjectNode primaryCodeSystemNode = JsonNodeFactory.instance.objectNode();
        primaryCodeSystemNode.put("type", "string");
        primaryCodeSystemNode.put("title", "Include Primary Code System Field");
        primaryCodeSystemNode.put("comments", "Include Primary Code System Field");
        parameters.set(PRIMARY_CODE_SYSTEM_PARAMETERS, primaryCodeSystemNode);

        ObjectNode approvalIDNameNode = JsonNodeFactory.instance.objectNode();
        approvalIDNameNode.put("type", "string");
        approvalIDNameNode.put("title", "Label for Approval ID in file");
        approvalIDNameNode.put("comments", "Header for Approval ID in file");
        parameters.set("approvalIDName", approvalIDNameNode);

        return generateSchemaNode("Relationship Exporter Parameters", parameters);
    }



}
