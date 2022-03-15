package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import ix.ginas.models.v1.Substance;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SDFImportAdaptorFactory implements AbstractImportSupportingGsrsEntityController.ImportAdapterFactory<Substance> {
    public final static String SIMPLE_REF ="REF_1";
    public final static String SIMPLE_REFERENCE_ACTION = "public_reference";
    public final static String ACTION_NAME ="actionName";
    public final static String ACTION_PARAMETERS ="actionParameters";
    public final static String CATALOG_REFERENCE = "CATALOG";
    public final static String REFERENCE_INSTRUCTION ="INSERT REFERENCE CITATION HERE";
    public final static String REFERENCE_ID_INSTRUCTION ="INSERT REFERENCE ID HERE";

    @Override
    public String getAdapterName() {
        return "SDF Adapter";
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return Arrays.asList("sdf", "sd");
    }

    @Override
    public AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        AbstractImportSupportingGsrsEntityController.ImportAdapter sDFImportAdapter = new SDFImportAdapter();
        return sDFImportAdapter;
    }

    @Override
    public AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics predictSettings(InputStream is) {
        return null;
    }

    public JsonNode createDefaultSdfFileImport(List<String> fieldNames) {
        ObjectNode topLevelReturn =JsonNodeFactory.instance.objectNode();
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        ObjectNode structureNode = JsonNodeFactory.instance.objectNode();
        structureNode.put(ACTION_NAME, "structure_and_moieties");
        structureNode.set(ACTION_PARAMETERS, createDefaultReferenceReferenceNode());
        result.add(structureNode);
        fieldNames.forEach(f -> {
            ObjectNode actionNode= JsonNodeFactory.instance.objectNode();
            if(f.toUpperCase(Locale.ROOT).endsWith("NAME")) {
                actionNode.put(ACTION_NAME, "common_name");
                ArrayNode parameters = JsonNodeFactory.instance.arrayNode();
                parameters.add(String.format("{{%s}}", f));
                parameters.add(createDefaultReferenceReferenceNode());
                actionNode.set(ACTION_PARAMETERS, parameters);
            } else {
                actionNode.put(ACTION_NAME, "code_import");
                ArrayNode parameters = JsonNodeFactory.instance.arrayNode();
                parameters.add(String.format("{{%s}}", f));
                parameters.add(f);
                actionNode.set(ACTION_PARAMETERS, parameters);
            }
            result.add(actionNode);
        });
        result.add(createDefaultReferenceNode());
        topLevelReturn.set("actions", result);
        return topLevelReturn;
    }

    private JsonNode createDefaultReferenceReferenceNode() {
        ArrayNode parameters = JsonNodeFactory.instance.arrayNode();
        parameters.add(SIMPLE_REF);
        return parameters;
    }

    private JsonNode createDefaultReferenceNode() {
        ObjectNode referenceNode = JsonNodeFactory.instance.objectNode();
        referenceNode.put(ACTION_NAME, SIMPLE_REFERENCE_ACTION);

        ArrayNode parameters = JsonNodeFactory.instance.arrayNode();
        parameters.add(CATALOG_REFERENCE);
        parameters.add(REFERENCE_INSTRUCTION);
        parameters.add(REFERENCE_ID_INSTRUCTION);
        parameters.add(SIMPLE_REF);
        referenceNode.set(ACTION_PARAMETERS, parameters);

        return referenceNode;
    }
}
