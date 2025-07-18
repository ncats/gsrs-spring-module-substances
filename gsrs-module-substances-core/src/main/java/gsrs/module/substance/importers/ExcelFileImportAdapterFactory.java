package gsrs.module.substance.importers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterStatistics;
import ix.ginas.importers.ExcelSpreadsheetReader;
import ix.ginas.importers.InputFieldStatistics;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.*;

@Slf4j
public class ExcelFileImportAdapterFactory extends DelimTextImportAdapterFactory {

    private List<Class> entityServices;

    private List<String> fieldNames;

    private String dataSheetName;

    private Integer fieldRow = 0;

    private List<String> extensions = Arrays.asList("xlsx");

    private String description = "Importer for Excel Spreadsheets";

    @Override
    public String getAdapterName() {
        return "Excel Spreadsheet Adapter";
    }

    @Override
    public String getAdapterKey() {
        return "ExcelSpreadsheet";
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return this.extensions;
    }

    @Override
    public void setSupportedFileExtensions(List<String> extensions) {
        this.extensions = extensions;
    }


    @SneakyThrows
    @Override
    public ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        log.trace("starting in createAdapter. adapterSettings: " + adapterSettings.toPrettyString());
        List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions = getMappingActions(adapterSettings);
        Map<String, Object> initializationParameters;
        if (adapterSettings.hasNonNull("parameters")) {
            log.trace("adapterSettings has parameters");
            ObjectMapper mapper = new ObjectMapper();
            initializationParameters = mapper.convertValue(adapterSettings.get("parameters"), new TypeReference<Map<String, Object>>() {
            });
        } else {
            log.trace("adapterSettings has NO parameters");
            initializationParameters = new HashMap<>();
            if (this.fieldNames != null) {
                initializationParameters.put("fieldNames", this.fieldNames);
            }
            initializationParameters.put("fieldRow", this.fieldRow);
            initializationParameters.put("dataSheetName", this.dataSheetName);
            initializationParameters.put("substanceClassName", this.substanceClassName);
            log.trace("created map");
        }
        log.trace("initializationParameters: " + initializationParameters);
        return new ExcelFileImportAdapter(actions, initializationParameters);
    }

    @Override
    public ImportAdapterStatistics predictSettings(InputStream is, ObjectNode settings) {
        log.trace("in predictSettings");
        String sheetName = settings.get("dataSheetName") != null ? settings.get("dataSheetName").textValue() : null;
        log.trace("using sheet name {}", sheetName);
        Set<String> fields;
        if (registry == null || registry.isEmpty()) {
            log.trace("predictSettings will call initialize");
            initialize();
        }
            /*
            intended logic 28 April 2022:
            steps:
            1: calculate summary statistics based on sd file input
            2: look through all steps in registry for any step marked as 'prediction steps' (todo: find better term) (aka default steps)
                    e.g., peak loading for NSRS.  this becomes a PROPERTY but we don't show them that configuration thing to select from because that would be a
                        distraction. more obscure case: an SD file property called 'melting point' that gets mapped to a property
             */
        ExcelSpreadsheetReader reader = new ExcelSpreadsheetReader(is);
        Map<String, InputFieldStatistics> stats =
                reader.getFileStatistics(sheetName, null, 10, 0);
        ImportAdapterStatistics statistics =
                new ImportAdapterStatistics();
        fields = stats.keySet();
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.putPOJO(FIELD_LIST, fields);
        node.put("fileName", getFileName());
        statistics.setAdapterSchema(node);
        statistics.setAdapterSettings(createDefaultFileImport(stats));
        this.statistics = statistics;
        return statistics;
    }

}
