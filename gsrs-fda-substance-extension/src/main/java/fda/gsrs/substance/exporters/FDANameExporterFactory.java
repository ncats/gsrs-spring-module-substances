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

/**
 * Created by VenkataSaiRa.Chavali on 3/10/2017.
 */
public class FDANameExporterFactory implements ExporterFactory<Substance> {

    public static final String NAME_PARAMETERS ="addNames";
    public static final String CODE_PARAMETERS ="addPrimaryCodeSystems";

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


    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        ObjectNode nameNode = JsonNodeFactory.instance.objectNode();
        nameNode.put("type", "boolean");
        nameNode.put("title", "Include Names?");
        nameNode.put("comments", "Add Substance names to output?");
        parameters.set(NAME_PARAMETERS, nameNode);

        ObjectNode codeNode = JsonNodeFactory.instance.objectNode();
        codeNode.put("type", "boolean");
        codeNode.put("title", "Include Codes?");
        codeNode.put("comments", "Add Substance codes to output?");
        parameters.set(CODE_PARAMETERS, codeNode);

        ObjectNode approvalIDNameNode = JsonNodeFactory.instance.objectNode();
        approvalIDNameNode.put("type", "string");
        approvalIDNameNode.put("title", "Label for Approval ID in file");
        approvalIDNameNode.put("comments", "Header for Approval ID in file");
        parameters.set("approvalIDName", approvalIDNameNode);
        return generateSchemaNode("SDF File Exporter Parameters", parameters);
    }



}
