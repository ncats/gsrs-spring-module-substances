package gsrs.module.substance.importers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.dataExchange.model.MappingAction;
import gsrs.dataExchange.model.MappingActionFactory;
import gsrs.imports.ImportAdapterFactory;
import gsrs.module.substance.importers.importActionFactories.*;
import gsrs.module.substance.importers.model.SDRecordContext;
import gsrs.module.substance.utils.NCATSFileUtils;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SpringBootConfiguration
@Slf4j
public class SDFImportAdaptorFactory implements ImportAdapterFactory<Substance> {
    public final static String SIMPLE_REF = "UUID_1";
    public final static String SIMPLE_REFERENCE_ACTION = "public_reference";
    public final static String ACTION_NAME = "actionName";
    public final static String ACTION_PARAMETERS = "actionParameters";
    public final static String CATALOG_REFERENCE = "CATALOG";
    public final static String REFERENCE_INSTRUCTION = "INSERT REFERENCE CITATION HERE";
    public final static String REFERENCE_ID_INSTRUCTION = "INSERT REFERENCE ID HERE";
    public final static String SDF_FIELD_LIST = "SDF Fields";

    private Map<String, NCATSFileUtils.InputFieldStatistics> fileFieldStatisticsMap;

    @Value("#{${ix.gsrs.sdfActions}}")
    protected Map<String, String> defaultImportActions;

    protected Map<String, String> fileImportActions;

    public SDFImportAdaptorFactory() {
        //init();
    }
    //** ADDING ABSTRACT LAYERS START




    //** ADDING ABSTRACT LAYERS END

    //** TYLER ADDING SPECIAL START
    public static final Pattern SDF_RESOLVE = Pattern.compile("\\{\\{([^\\}]*)\\}\\}");
    public static final Pattern SPECIAL_RESOLVE = Pattern.compile("\\[\\[([^\\]]*)\\]\\]");

    public Map<String, String> getFileImportActions() {
        return fileImportActions;
    }

    public void setFileImportActions(Map<String, String> fileImportActions) {
        fileImportActions = fileImportActions;
    }

    private static String replacePattern(String inp, Pattern p, Function<String, Optional<String>> resolver) {
        Matcher m = p.matcher(inp);
        StringBuilder nstr = new StringBuilder();
        int start = 0;
        while (m.find()) {
            int ss = m.start(0);
            nstr.append(inp.substring(start, ss));
            start = m.end(0);
            String prop = m.group(1);
            nstr.append(resolver.apply(prop).get());
        }
        nstr.append(inp.substring(start));
        return nstr.toString();
    }

    public static String resolveParameter(SDRecordContext rec, String inp) {
        return resolveParameter(rec, inp, s -> s);
    }

    public static String resolveParameter(SDRecordContext rec, String inp, Function<String, String> encoder) {
        inp = replacePattern(inp, SDF_RESOLVE, (p) -> {
            if (p.equals("molfile")) return Optional.ofNullable(rec.getStructure()).map(encoder);
            if (p.equals("molfile_name")) return Optional.ofNullable(rec.getMolfileName()).map(encoder);
            return rec.getProperty(p).map(encoder);
        });
        inp = replacePattern(inp, SPECIAL_RESOLVE, (p) -> rec.resolveSpecial(p).map(encoder));
        return inp;
    }

    public static List<String> resolveParameters(SDRecordContext rec, List<String> inputList) {
        return inputList.stream().map(s -> resolveParameter(rec, s)).collect(Collectors.toList());
    }

    public static Map<String, Object> resolveParametersMap(SDRecordContext rec, Map<String, Object> inputMap) {
        ObjectMapper om = new ObjectMapper();
        Map<String, Object> newMap;
        JsonNode jsn = om.valueToTree(inputMap);
        String json = resolveParameter(rec, jsn.toString(), s -> {
            String m = om.valueToTree(s).toString();
            return m.substring(1, m.length() - 1);
        });
        try {
            newMap = (Map<String, Object>) (om.readValue(json, LinkedHashMap.class));
            return newMap;
        } catch (Exception ex) {
            log.error("Error in resolveParametersMap: " + ex.getMessage());
            ex.printStackTrace();
        }
        return new HashMap<>();
    }


    protected static Map<String, MappingActionFactory<Substance, SDRecordContext>> registry = new ConcurrentHashMap<>();

    @Override
    public void initialize(){
        if( fileImportActions== null && defaultImportActions!=null && defaultImportActions.size()>0 ) {
            fileImportActions = defaultImportActions;
        }
        log.trace("fileImportActions: " + fileImportActions);
        registry.clear();
        if(fileImportActions !=null && fileImportActions.size() >0) {
            Map<String, Object> params =  Collections.emptyMap();
            ObjectMapper mapper = new ObjectMapper();
            Set<String> actionNames=fileImportActions.keySet();
            actionNames.forEach(actionName->{
                try {
                    MappingActionFactory<Substance, SDRecordContext> mappingActionFactory =
                            (MappingActionFactory<Substance, SDRecordContext>) mapper.convertValue(params,
                                    Class.forName( fileImportActions.get(actionName)));
                    registry.put(actionName, mappingActionFactory);
                    log.trace(String.format("added action %s as class with name %s", actionName, fileImportActions.get(actionName)));
                } catch (ClassNotFoundException e) {
                    log.error("error instantiating class.  message: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
        else {
            log.trace("using default actions");
            registry.put("common_name", new NameExtractorActionFactory());
            registry.put("code_import", new CodeExtractorActionFactory());
            registry.put("structure_and_moieties", new StructureExtractorActionFactory());
            registry.put("note_import", new NotesExtractorActionFactory());
            registry.put("property_import", new PropertyExtractorActionFactory());
            registry.put(SIMPLE_REFERENCE_ACTION, new ReferenceExtractorActionFactory());
        }
    }


    public static List<MappingAction<Substance, SDRecordContext>> getMappingActions(JsonNode adapterSettings) throws Exception {
        List<MappingAction<Substance, SDRecordContext>> actions = new ArrayList<>();
        log.trace("adapterSettings: " + adapterSettings.toPrettyString());
        adapterSettings.get("actions").forEach(js -> {
            String actionName = js.get("actionName").asText();
            JsonNode actionParameters = js.get("actionParameters");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> params = mapper.convertValue(actionParameters, new TypeReference<Map<String, Object>>() {
            });
            MappingAction<Substance, SDRecordContext> action =null;
            try {
                action=(MappingAction<Substance, SDRecordContext>) registry.get(actionName).create(params);
            }
            catch (Exception ex) {
                log.error("Error in getMappingActions: " + ex.getMessage());
                ex.printStackTrace();
            }

            actions.add(action);
        });
        return actions;
    }

    //** TYLER ADDING SPECIAL END

    @Override
    public String getAdapterName() {
        return "SDF Adapter";
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return Arrays.asList("sdf", "sd");
    }

    @SneakyThrows
    @Override
    public AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        log.trace("starting in createAdapter. adapterSettings: " + adapterSettings.toPrettyString());
        List<MappingAction<Substance, SDRecordContext>> actions = getMappingActions(adapterSettings);
        AbstractImportSupportingGsrsEntityController.ImportAdapter sDFImportAdapter = new SDFImportAdapter(actions);
        return sDFImportAdapter;
    }

    @Override
    public AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics predictSettings(InputStream is) {
        Set<String> fields = null;
        try {
            Map<String, NCATSFileUtils.InputFieldStatistics>stats= getFieldsForFile(is);
            AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics statistics =
                    new AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics();
            fields =stats.keySet();
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.putPOJO(SDF_FIELD_LIST, fields);
            statistics.setAdapterSchema(node);
            statistics.setAdapterSettings(createDefaultSdfFileImport(stats));
            return statistics;
        } catch (IOException ex) {
            log.error("error reading list of fields from SD file: " + ex.getMessage());
        }
        return null;
    }


    public JsonNode createDefaultSdfFileImport(Map<String, NCATSFileUtils.InputFieldStatistics> map) {
        Set<String> fieldNames =map.keySet();
        ObjectNode topLevelReturn = JsonNodeFactory.instance.objectNode();
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        ObjectNode structureNode = JsonNodeFactory.instance.objectNode();
        structureNode.put(ACTION_NAME, "structure_and_moieties");
        structureNode.set(ACTION_PARAMETERS, createMolfileMap());

        result.add(structureNode);
        fieldNames.forEach(f -> {
            ObjectNode actionNode = JsonNodeFactory.instance.objectNode();
            if (f.toUpperCase(Locale.ROOT).endsWith("NAME")) {
                actionNode.put(ACTION_NAME, "common_name");
                ObjectNode mapNode = createNameMap(f, null);
                actionNode.set(ACTION_PARAMETERS, mapNode);
            } else {
                actionNode.put(ACTION_NAME, "code_import");
                ObjectNode mapNode = createCodeMap(f, "PRIMARY");
                actionNode.set(ACTION_PARAMETERS, mapNode);
            }
            result.add(actionNode);
        });
        result.add(createDefaultReferenceNode());
        topLevelReturn.set("actions", result);
        return topLevelReturn;
    }

    private JsonNode createDefaultReferenceReferenceNode() {
        ArrayNode parameters = JsonNodeFactory.instance.arrayNode();
        parameters.add(String.format("[[%s]]", SIMPLE_REF));
        return parameters;
    }

    private JsonNode createDefaultReferenceNode() {
        ObjectNode referenceNode = JsonNodeFactory.instance.objectNode();
        referenceNode.put(ACTION_NAME, SIMPLE_REFERENCE_ACTION);

        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        parameters.put("docType", CATALOG_REFERENCE);
        parameters.put("citation", REFERENCE_INSTRUCTION);
        parameters.put("referenceID", REFERENCE_ID_INSTRUCTION);
        parameters.put("uuid", String.format("[[%s]]", SIMPLE_REF));
        referenceNode.set(ACTION_PARAMETERS, parameters);

        return referenceNode;
    }

    public Map<String, NCATSFileUtils.InputFieldStatistics> getFieldsForFile(InputStream input) throws IOException {
        log.trace("starting in fieldsForSDF");
        Map<String, NCATSFileUtils.InputFieldStatistics> fieldStatisticsMap =
                NCATSFileUtils.getSDFieldStatistics(input);
        fileFieldStatisticsMap = fieldStatisticsMap;
        log.trace("total fields: " + fileFieldStatisticsMap.keySet().size());
        return fieldStatisticsMap;
    }

    public ObjectNode createCodeMap(String codeSystem, String codeType) {
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        if (codeType == null || codeType.length() == 0) {
            codeType = "PRIMARY";
        }
        mapNode.put("codeSystem", codeSystem);
        mapNode.put("code", String.format("{{%s}}", codeSystem));
        mapNode.put("codeType", codeType);
        return mapNode;
    }

    public ObjectNode createNameMap(String nameField, String nameType) {
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        if (nameType == null || nameType.length() == 0) {
            nameType = "cn";
        }
        mapNode.put("name", String.format("{{%s}}", nameField));
        mapNode.put("nameType", nameType);
        ArrayNode refs = JsonNodeFactory.instance.arrayNode();
        refs.add(String.format("[[%s]]", SIMPLE_REF));
        mapNode.set("referenceUUIDs", refs);
        return mapNode;
    }

    public ObjectNode createMolfileMap() {
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        mapNode.put("molfile", "{{molfile}}");
        return mapNode;
    }
}
