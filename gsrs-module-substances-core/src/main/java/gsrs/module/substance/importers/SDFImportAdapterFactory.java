package gsrs.module.substance.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.ImportFieldMetadata;
import gsrs.imports.ActionConfigImpl;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterStatistics;
import gsrs.module.substance.importers.importActionFactories.*;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import gsrs.module.substance.utils.NCATSFileUtils;
import ix.ginas.importers.InputFieldStatistics;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringBootConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootConfiguration
@Slf4j
public class SDFImportAdapterFactory extends SubstanceImportAdapterFactoryBase {
    public final static String SDF_FIELD_LIST = "fields";

    private Map<String, InputFieldStatistics> fileFieldStatisticsMap;

    private Class stagingAreaService;

    private List<Class> entityServices;

    private String description="SD file importer for all";

    public SDFImportAdapterFactory() {
    }
    //** ADDING ABSTRACT LAYERS START


    //** ADDING ABSTRACT LAYERS END

    private String originalFileName;

    private ImportAdapterStatistics statistics;

    public List<ActionConfigImpl> getFileImportActions() {
        return fileImportActions;
    }

    private List<String> extensions =Arrays.asList("sdf", "sd");

    public static List<String> resolveParameters(SDRecordContext rec, List<String> inputList) {
        return inputList.stream().map(s -> resolveParameter(rec, s)).collect(Collectors.toList());
    }


    @Override
    protected void defaultInitialize() {
        log.trace("using default actions");
        registry.put("common_name", new NameExtractorActionFactory());
        registry.put("code_import", new CodeExtractorActionFactory());
        registry.put("structure_and_moieties", new StructureExtractorActionFactory());
        registry.put("note_import", new NotesExtractorActionFactory());
        registry.put("property_import", new PropertyExtractorActionFactory());
        registry.put("no-op", new NoOpActionFactory());
        registry.put(SIMPLE_REFERENCE_ACTION, new ReferenceExtractorActionFactory());
    }

    //** TYLER ADDING SPECIAL END

    @Override
    public String getAdapterName() {
        return "SDF Adapter";
    }

    @Override
    public String getAdapterKey() {
        return "SDF";
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return this.extensions;
    }

    @Override
    public void setSupportedFileExtensions(List<String> extensions) {
        this.extensions=extensions;
    }

    @SneakyThrows
    @Override
    public ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        log.trace("starting in createAdapter. adapterSettings: " + adapterSettings.toPrettyString());
        List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions = getMappingActions(adapterSettings);
        ImportAdapter sDFImportAdapter = new SDFImportAdapter(actions);
        return sDFImportAdapter;
    }

    @Override
    public ImportAdapterStatistics predictSettings(InputStream is, ObjectNode settings) {
        log.trace("in predictSettings");
        Set<String> fields = null;
        try {
            if(registry == null || registry.isEmpty()) {
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
            Map<String, InputFieldStatistics>stats= getFieldsForFile(is);
            ImportAdapterStatistics statistics =
                    new ImportAdapterStatistics();
            Set<ImportFieldMetadata> fullFields = createMetadata(stats.keySet());
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.putPOJO(SDF_FIELD_LIST, fullFields);
            node.put("fileName", getFileName());
            statistics.setAdapterSchema(node);
            statistics.setAdapterSettings(createDefaultSdfFileImport(stats));
            this.statistics=statistics;
            return statistics;
        } catch (IOException ex) {
            log.error("error reading list of fields from SD file: " + ex.getMessage());
        }
        return null;
    }

    @Override
    public void setFileName(String fileName) {
        originalFileName=fileName;
    }

    @Override
    public String getFileName() {
        return originalFileName;
    }

    @Override
    public Class getStagingAreaService() {
        log.trace("using staging area service name from adapter factory");
        return this.stagingAreaService;
    }

    @Override
    public void setStagingAreaService(Class stagingService){
        log.trace("in setHoldingAreaService " + stagingService.getName());
        this.stagingAreaService = stagingService;
    }

    @Override
    public Class getStagingAreaEntityService() {
        return this.stagingAreaService;
    }

    @Override
    public void setStagingAreaEntityService(Class stagingAreaEntityService) {
        this.stagingAreaService =stagingAreaEntityService;
    }

    @Override
    public List<Class> getEntityServices() {
        return this.entityServices;
    }

    public JsonNode createDefaultSdfFileImport(Map<String, InputFieldStatistics> map) {
        log.trace("in createDefaultSdfFileImport");
        Set<String> fieldNames =map.keySet();
        ObjectNode topLevelReturn = JsonNodeFactory.instance.objectNode();
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        ObjectNode structureNode = JsonNodeFactory.instance.objectNode();
        structureNode.put(ACTION_NAME, "structure_and_moieties");
        structureNode.set(ACTION_PARAMETERS, createMolfileMap());
        structureNode.put("label", "Import Structure Action");
        structureNode.put(FILE_FIELD, "(Structure)");

        result.add(structureNode);
        fieldNames.forEach(f -> {
            ObjectNode actionNode = JsonNodeFactory.instance.objectNode();
            actionNode.put(FILE_FIELD, f);
            if (f.toUpperCase(Locale.ROOT).contains("NAME") || f.toUpperCase(Locale.ROOT).contains("SYNONYM")) {
                actionNode.put(ACTION_NAME, "common_name");// +createCleanFieldName(f));
                actionNode.put("label", "Create Name Action");
                ObjectNode mapNode = createNameMap(f, null, null);
                actionNode.set(ACTION_PARAMETERS, mapNode);
            } else if(looksLikeProperty(f)) {
                actionNode.put(ACTION_NAME, "property_import");
                actionNode.put("label", "Create Property Action");
                ObjectNode mapNode = createPropertyMap(f);
                actionNode.set(ACTION_PARAMETERS, mapNode);
            } else {
                actionNode.put(ACTION_NAME, "code_import");//  +createCleanFieldName(f));
                actionNode.put("label", "Create Code Action");
                ObjectNode mapNode = createCodeMap(f, "PRIMARY");
                actionNode.set(ACTION_PARAMETERS, mapNode);
            }
            result.add(actionNode);
        });
        result.add(createDefaultReferenceNode());
        topLevelReturn.set("actions", result);
        return topLevelReturn;
    }


    public Map<String, InputFieldStatistics> getFieldsForFile(InputStream input) throws IOException {
        log.trace("starting in fieldsForSDF");
        Map<String, InputFieldStatistics> fieldStatisticsMap =
                NCATSFileUtils.getSDFieldStatistics(input);
        fileFieldStatisticsMap = fieldStatisticsMap;
        log.trace("total fields: " + fileFieldStatisticsMap.keySet().size());
        return fieldStatisticsMap;
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
