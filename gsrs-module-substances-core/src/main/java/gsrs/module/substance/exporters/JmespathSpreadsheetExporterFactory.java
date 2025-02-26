package gsrs.module.substance.exporters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ix.ginas.exporters.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Egor Puzanov.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class JmespathSpreadsheetExporterFactory implements ExporterFactory {

    private OutputFormat format = new OutputFormat("custom.xlsx", "Custom Report (xlsx) File");

    private List<Map<String, String>> columnExpressions = (List<Map<String, String>>) Arrays.asList((Map<String, String>) new HashMap<String, String>(){{put("name", "UUID"); put("expression", "uuid");}});

    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }

    public void setColumnExpressions(Map<Integer, Map<String, String>> m) {
        this.columnExpressions = (List<Map<String, String>>) m.entrySet()
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
    public JmespathSpreadsheetExporter createNewExporter(OutputStream out, Parameters params) throws IOException {
        Spreadsheet spreadsheet = new ExcelSpreadsheet.Builder(out)
                                                      .maxRowsInMemory(100)
                                                      .build();
        JmespathSpreadsheetExporter.Builder builder = new JmespathSpreadsheetExporter.Builder(spreadsheet);
        for (Map<String, String> columnExpression : columnExpressions) {
            String columnName = columnExpression.get("name");
            ColumnValueRecipe<JsonNode> recipe = JmespathColumnValueRecipe.create(
                columnName,
                columnExpression.get("expression"),
                columnExpression.getOrDefault("delimiter", "|"),
                columnExpression.getOrDefault("datetime", null));
            builder = builder.addColumn(columnName, recipe);
        }
        return builder.build();
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        return parameters;
    }
}
