package gsrs.module.substance.misc.emasmsfhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

public class EmaSmsFhirExporterFactory implements ExporterFactory {
    // see FDACodeExporterFactory
    // see DefaultSubstanceSpreadsheetExporterFactory

    // To set include e.g. { parameters: {"primaryCodeSystem": "BDNUM"  }, ...}  in your factory configuration
    // the setter will be called automatically by gsrs code.

    public static final String PRIMARY_CODE_SYSTEM_PARAMETERS ="omitPrimaryCodeSystemField";
    public static final String APPROVAL_ID_NAME_PARAMETERS ="approvalIdName";
    public static final String DEFAULT_APPROVAL_ID_NAME ="APPROVAL_ID";

    OutputFormat format = new OutputFormat("emasmsfhir.txt", "Fhir export, (.emasmsfhir.txt)");

    private String primaryCodeSystem;


    @Autowired
    private EmaSmsFhirConfiguration emaSmsFhirConfiguration;

    @Autowired
    private EmaSmsSubstanceDefinitionFhirMapper emaSmsSubstanceDefinitionFhirMapper;

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
        System.out.println("Creating new Exporter using emaSmsSubstanceDefinitionFhirMapper, etc.");
        return new EmaSmsFhirExporter(out, params, this.primaryCodeSystem, this.emaSmsFhirConfiguration, this.emaSmsSubstanceDefinitionFhirMapper);
    }

    public String getPrimaryCodeSystem() {
        return this.primaryCodeSystem;
    }

    public void setPrimaryCodeSystem(String primaryCodeSystem) {
        this.primaryCodeSystem = primaryCodeSystem;
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        if(getPrimaryCodeSystem()!=null) {
            ObjectNode primaryCodeSystemNode = JsonNodeFactory.instance.objectNode();
            primaryCodeSystemNode.put("type", "boolean");
            primaryCodeSystemNode.put("title", "Omit primary code system field ("+ getPrimaryCodeSystem() +")");
            primaryCodeSystemNode.put("comments", "Omit primary code system field ("+ getPrimaryCodeSystem() +")");
            primaryCodeSystemNode.put("default", false);
            parameters.set(PRIMARY_CODE_SYSTEM_PARAMETERS, primaryCodeSystemNode);
        }
        ObjectNode approvalIDNameNode = JsonNodeFactory.instance.objectNode();
        approvalIDNameNode.put("type", "string");
        approvalIDNameNode.put("title", "Label for Approval ID in file");
        approvalIDNameNode.put("comments", "Header for Approval ID in file");
        approvalIDNameNode.put("default", DEFAULT_APPROVAL_ID_NAME);
        parameters.set(APPROVAL_ID_NAME_PARAMETERS, approvalIDNameNode);
        return generateSchemaNode("Code Exporter Parameters", parameters);
    }
}
