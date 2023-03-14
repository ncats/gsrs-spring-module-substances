package gsrs.module.substance.importers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterStatistics;
import gsrs.module.substance.importers.importActionFactories.*;
import ix.ginas.importers.InputFieldStatistics;
import ix.ginas.importers.TextFileReader;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.util.*;

@Slf4j
public class DelimTextImportAdapterFactory extends SubstanceImportAdapterFactoryBase {

    private String lineValueDelimiter;

    private boolean removeQuotes = false;

    private String inputFileName;

    protected Class stagingAreaService;

    protected List<Class> entityServices;

    private List<String> fields;

    private int linesToSkip;

    private List<String> extensions = Arrays.asList("csv", "txt", "tsv");

    public final static String FIELD_LIST = "fields";

    public String substanceClassName;

    private String description = "Importer for various types of text/delimited files";

    @Override
    public String getAdapterName() {
        return "Delimited Text Adapter";
    }

    @Override
    public String getAdapterKey() {
        return "DelimitedText";
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
        Map<String, Object> initializationParameters = null;
        if (adapterSettings.hasNonNull("parameters")) {
            log.trace("adapterSettings has parameters");
            ObjectMapper mapper = new ObjectMapper();
            initializationParameters = mapper.convertValue(adapterSettings.get("parameters"), new TypeReference<Map<String, Object>>() {
            });
        } else {
            log.trace("adapterSettings has NO parameters");
            initializationParameters = new HashMap<>();
            if (this.lineValueDelimiter != null) {
                initializationParameters.put("lineValueDelimiter", this.lineValueDelimiter);
            }
            initializationParameters.put("linesToSkip", this.linesToSkip);
            initializationParameters.put("removeQuotes", this.removeQuotes);
            initializationParameters.put("substanceClassName", this.substanceClassName);
            log.trace("created map");
        }
        log.trace("initializationParameters: " + initializationParameters);
        DelimTextImportAdapter importAdapter = new DelimTextImportAdapter(actions, initializationParameters);
        return importAdapter;
    }

    /*
    Actions that will be available when config does not contain any actions
     */
    @Override
    protected void defaultInitialize() {
        log.trace("using default actions");
        registry.put("common_name", new NameExtractorActionFactory());
        registry.put("code_import", new CodeExtractorActionFactory());
        registry.put("note_import", new NotesExtractorActionFactory());
        registry.put("property_import", new PropertyExtractorActionFactory());
        registry.put("protein_import", new ProteinSequenceExtractorActionFactory());
        registry.put("no-op", new NoOpActionFactory());
        registry.put(SIMPLE_REFERENCE_ACTION, new ReferenceExtractorActionFactory());
    }

    @Override
    public ImportAdapterStatistics predictSettings(InputStream is, ObjectNode settings) {
        log.trace("in predictSettings");
        Set<String> fields = null;
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
        TextFileReader textFileReader = new TextFileReader();
        Map<String, InputFieldStatistics> stats =
                textFileReader.getFileStatistics(is, this.lineValueDelimiter, this.removeQuotes, null, 10, 0);
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

    public JsonNode createDefaultFileImport(Map<String, InputFieldStatistics> map) {
        log.trace("in createDefaultSdfFileImport");
        Set<String> fieldNames = map.keySet();
        ObjectNode topLevelReturn = JsonNodeFactory.instance.objectNode();
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        fieldNames.forEach(f -> {
            ObjectNode actionNode = JsonNodeFactory.instance.objectNode();
            actionNode.put(FILE_FIELD, f);
            if (f.toUpperCase(Locale.ROOT).contains("NAME") || f.toUpperCase(Locale.ROOT).contains("SYNONYM")) {
                actionNode.put(ACTION_NAME, "common_name");// +createCleanFieldName(f));
                ObjectNode mapNode = createNameMap(f, null, null);
                actionNode.put("label", "Create Name Action");
                actionNode.set(ACTION_PARAMETERS, mapNode);
            } else if (looksLikeProperty(f)) {
                actionNode.put(ACTION_NAME, "property_import");
                actionNode.put("label", "Create Property Action");
                ObjectNode mapNode = createPropertyMap(f);
                actionNode.set(ACTION_PARAMETERS, mapNode);
            } else if (looksLikeProteinSequence(f)) {
                actionNode.put(ACTION_NAME, "protein_import");
                ObjectNode mapNode = createProteinSequenceMap(f);
                actionNode.set(ACTION_PARAMETERS, mapNode);
                actionNode.put("label", "Create Protein Subunit");
            } else if( looksLikeNucleicAcidSequence(f)){
                actionNode.put(ACTION_NAME, "nucleic_acid_import");
                ObjectNode mapNode = createNucleicAcidMap(f);
                actionNode.set(ACTION_PARAMETERS, mapNode);
                actionNode.put("label", "Create Simple Nucleic Acid");
            }
            else {
                actionNode.put(ACTION_NAME, "code_import");//  +createCleanFieldName(f));
                ObjectNode mapNode = createCodeMap(f, "PRIMARY");
                actionNode.put("label", "Create Code Action");
                actionNode.set(ACTION_PARAMETERS, mapNode);
            }
            result.add(actionNode);
        });
        result.add(createDefaultReferenceNode());
        topLevelReturn.set("actions", result);
        return topLevelReturn;
    }

    @Override
    public void setFileName(String fileName) {
        inputFileName = fileName;
    }

    @Override
    public String getFileName() {
        return this.inputFileName;
    }

    @Override
    public Class getStagingAreaService() {
        return this.stagingAreaService;
    }

    @Override
    public void setStagingAreaService(Class stagingService) {
        this.stagingAreaService = stagingService;
    }


    @Override
    public List<Class> getEntityServices() {
        return this.entityServices;
    }

    @Override
    public void setEntityServices(List<Class> services) {
        this.entityServices = services;
    }

    @Override
    public void setInputParameters(JsonNode parameters) {
        log.trace("in setInputParameters");
        if (parameters.hasNonNull("lineValueDelimiter")) {
            log.trace("lineValueDelimiter: {}", parameters.get("setInputParameters"));
            setLineValueDelimiter(parameters.get("lineValueDelimiter").asText());
        }
        if (parameters.hasNonNull("linesToSkip")) {
            log.trace("linesToSkip: {}", parameters.get("linesToSkip"));
            setLinesToSkip(parameters.get("linesToSkip").asInt());
        }
        if (parameters.hasNonNull("removeQuotes")) {
            log.trace("removeQuotes:  {}", parameters.get("removeQuotes"));
            removeQuotes = parameters.get("removeQuotes").asBoolean();
        }

        if (parameters.hasNonNull("substanceClassName")) {
            log.trace("substanceClassName: {}", parameters.get("substanceClassName").asText());
            this.substanceClassName = parameters.get("substanceClassName").asText();
        }
    }

    public void setLineValueDelimiter(String newDelimiter) {
        this.lineValueDelimiter = newDelimiter;
    }

    public String getLineValueDelimiter() {
        return this.lineValueDelimiter;
    }

    public int getLinesToSkip() {
        return linesToSkip;
    }

    public void setLinesToSkip(int linesToSkip) {
        this.linesToSkip = linesToSkip;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }
}
