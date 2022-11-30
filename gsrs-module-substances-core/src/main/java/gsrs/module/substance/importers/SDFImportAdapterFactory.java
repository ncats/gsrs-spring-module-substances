package gsrs.module.substance.importers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactory;
import gsrs.imports.*;
import gsrs.module.substance.importers.importActionFactories.*;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import gsrs.module.substance.utils.NCATSFileUtils;
import ix.ginas.models.v1.Substance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringBootConfiguration;

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
public class SDFImportAdapterFactory implements ImportAdapterFactory<Substance> {
    public final static String SIMPLE_REF = "UUID_1";
    public final static String SIMPLE_REFERENCE_ACTION = "public_reference";
    public final static String ACTION_NAME = "actionName";
    public final static String ACTION_PARAMETERS = "actionParameters";
    public final static String CATALOG_REFERENCE = "CATALOG";
    public final static String REFERENCE_INSTRUCTION = "INSERT REFERENCE CITATION HERE";
    public final static String REFERENCE_ID_INSTRUCTION = "INSERT REFERENCE ID HERE";
    public final static String SDF_FIELD_LIST = "SDF Fields";

    private Map<String, NCATSFileUtils.InputFieldStatistics> fileFieldStatisticsMap;

    protected List<ActionConfigImpl> fileImportActions;

    private Class holdingAreaService;

    private List<Class> entityServices;

    private Class entityServiceClass;

    public SDFImportAdapterFactory() {
    }
    //** ADDING ABSTRACT LAYERS START


    //** ADDING ABSTRACT LAYERS END

    //** TYLER ADDING SPECIAL START
    public static final Pattern SDF_RESOLVE = Pattern.compile("\\{\\{([^\\}]*)\\}\\}");
    public static final Pattern SPECIAL_RESOLVE = Pattern.compile("\\[\\[([^\\]]*)\\]\\]");

    private String originalFileName;
    private ImportAdapterStatistics statistics;

    public List<ActionConfigImpl> getFileImportActions() {
        return fileImportActions;
    }

    public void setFileImportActions(List<ActionConfigImpl> fileImportActions) {
        log.trace("setFileImportActions");
        this.fileImportActions = fileImportActions;
    }

    private static String replacePattern(String inp, Pattern p, Function<String, Optional<String>> resolver) {
        Matcher m = p.matcher(inp);
        StringBuilder newString = new StringBuilder();
        int start = 0;
        boolean foundValue=false;
        while (m.find()) {
            int ss = m.start(0);
            newString.append(inp.substring(start, ss));
            start = m.end(0);
            String prop = m.group(1);
            log.trace("prop: {} from inp: {}", prop, inp);
            Optional<String> value = resolver.apply(prop);
            if (value.isPresent()) {
                newString.append(value.get());
                log.trace("We have value: " + value.get());
                foundValue= true;
            }
            else {
                log.trace("no value");
            }
        }
        if( foundValue) {
            newString.append(inp.substring(start));
            return newString.toString();
        }
        return inp;
    }

    /*
    simplified overload that uses the identity function as an encoder
     */
    public static String resolveParameter(SDRecordContext rec, String inp) {
        return resolveParameter(rec, inp, s -> s);
    }

    /*
    Gets value for 3 special fields:
    1) molfiles -- the structure field of an SD file record
    2) name within molfile
    3) UUID, coded as [[[UUID_1]]]
    as well as regular SD file properties
    Passes the result through an encoder function before returning
     */
    public static String resolveParameter(PropertyBasedDataRecordContext rec, String inp, Function<String, String> encoder) {
        log.trace("in resolveParameter, inp: {}", inp);
        if(rec instanceof SDRecordContext) {
            SDRecordContext sdRec = (SDRecordContext)rec;
            inp = replacePattern(inp, SDF_RESOLVE, (p) -> {
                if (p.equals("molfile")) return Optional.ofNullable(sdRec.getStructure()).map(encoder);
                if (p.equals("molfile_name")) return Optional.ofNullable(sdRec.getMolfileName()).map(encoder);
                return rec.getProperty(p).map(encoder);
            });
        }
        inp = replacePattern(inp, SPECIAL_RESOLVE, (p) -> rec.resolveSpecial(p).map(encoder));
        return inp;
    }

    public static List<String> resolveParameters(SDRecordContext rec, List<String> inputList) {
        return inputList.stream().map(s -> resolveParameter(rec, s)).collect(Collectors.toList());
    }

    /*
    Adapts the abstract supplied parameters (inputMap) to real record-based parameters by replacing variable names
    with values found in the record.
    todo: add a table of example input/output values
     */
    public static Map<String, Object> resolveParametersMap(PropertyBasedDataRecordContext rec, Map<String, Object> inputMap)
        throws Exception{
        log.trace("in resolveParametersMap. rec properties: ");
        rec.getProperties().forEach(p->log.trace("property: {}; value: {}", p, rec.getProperty(p)));
        inputMap.keySet().forEach(k->log.trace("key: {}, val: {}", k, inputMap.get(k)));
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
            throw ex;
            //ex.printStackTrace();
        }
    }


    protected Map<String, MappingActionFactory<Substance, PropertyBasedDataRecordContext>> registry = new ConcurrentHashMap<>();

    @Override
    public void initialize(){
        log.trace("fileImportActions: " + fileImportActions);
        registry.clear();
        if(fileImportActions !=null && fileImportActions.size() >0) {
            log.trace("config-specific init");
            ObjectMapper mapper = new ObjectMapper();
            fileImportActions.forEach(fia->{
                String actionName =fia.getActionName();
                Class actionClass =fia.getActionClass();
                MappingActionFactory<Substance, PropertyBasedDataRecordContext> mappingActionFactory;
                try {
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    mappingActionFactory = (MappingActionFactory<Substance, PropertyBasedDataRecordContext>) mapper.convertValue(fia,
                            actionClass);

                    if(!fia.getParameters().isEmpty()) {
                        mappingActionFactory.setParameters(fia.getParameters());
                        mappingActionFactory.implementParameters();
                    }
                } catch (Exception e) {
                    log.error("Error parsing parameter metadata", e);
                    throw new RuntimeException(e);
                }
                registry.put(actionName, mappingActionFactory);
            });
        }
        else {
            defaultInitialize();
        }
    }

    protected void defaultInitialize() {
        log.trace("using default actions");
        registry.put("common_name", new NameExtractorActionFactory());
        registry.put("code_import", new CodeExtractorActionFactory());
        registry.put("structure_and_moieties", new StructureExtractorActionFactory());
        registry.put("note_import", new NotesExtractorActionFactory());
        registry.put("property_import", new PropertyExtractorActionFactory());
        registry.put(SIMPLE_REFERENCE_ACTION, new ReferenceExtractorActionFactory());
    }

    public List<MappingAction<Substance, PropertyBasedDataRecordContext>> getMappingActions(JsonNode adapterSettings) throws Exception {
        List<MappingAction<Substance, PropertyBasedDataRecordContext>> actions = new ArrayList<>();
        adapterSettings.get("actions").forEach(js -> {
            String actionName = js.get("actionName").asText();
            JsonNode actionParameters = js.get("actionParameters");
            ObjectMapper mapper = new ObjectMapper();
            log.trace("about to call convertValue");
            Map<String, Object> params = mapper.convertValue(actionParameters, new TypeReference<Map<String, Object>>() {});
            log.trace("Finished call to convertValue");
            MappingAction<Substance, PropertyBasedDataRecordContext> action =null;
            try {
                log.trace("looking for action {}; registry size: {}", actionName, registry.size());
                MappingActionFactory<Substance, PropertyBasedDataRecordContext> mappingActionFactory=registry.get(actionName);
                if( mappingActionFactory instanceof BaseActionFactory && this.statistics != null) {
                    ((BaseActionFactory) mappingActionFactory).setAdapterSchema(this.statistics.getAdapterSchema());
                    log.trace("called setAdapterSchema");
                }
                log.trace("mappingActionFactory: " + mappingActionFactory);
                if( mappingActionFactory!=null ) {
                    action=mappingActionFactory.create(params);
                    actions.add(action);
                }
                else {
                    log.error("No action found for "+ actionName);
                }
            }
            catch (Exception ex) {
                log.error("Error in getMappingActions: " + ex.getMessage());
                ex.printStackTrace();
            }


        });
        return actions;
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
        return Arrays.asList("sdf", "sd");
    }

    @SneakyThrows
    @Override
    public ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        log.trace("starting in createAdapter. adapterSettings: " + adapterSettings.toPrettyString());
        List<MappingAction<Substance, PropertyBasedDataRecordContext>> actions = getMappingActions(adapterSettings);
        ImportAdapter sDFImportAdapter = new SDFImportAdapter(actions);
        return sDFImportAdapter;
    }

    @Override
    public ImportAdapterStatistics predictSettings(InputStream is) {
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
            Map<String, NCATSFileUtils.InputFieldStatistics>stats= getFieldsForFile(is);
            ImportAdapterStatistics statistics =
                    new ImportAdapterStatistics();
            fields =stats.keySet();
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.putPOJO(SDF_FIELD_LIST, fields);
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
    public Class getHoldingAreaService() {
        log.trace("using holding area service name from adapter factory");
        return this.holdingAreaService;
    }

    @Override
    public void setHoldingAreaService(Class holdingAreaService){
        log.trace("in setHoldingAreaService " + holdingAreaService.getName());
        this.holdingAreaService=holdingAreaService;
    }

    @Override
    public Class getHoldingAreaEntityService() {
        return this.holdingAreaService;
    }

    @Override
    public void setHoldingAreaEntityService(Class holdingAreaEntityService) {
        this.holdingAreaService=holdingAreaEntityService;
    }

    @Override
    public List<Class> getEntityServices() {
        return this.entityServices;
    }

    @Override
    public void setEntityServices(List<Class> services) {
        this.entityServices=services;
    }

    @Override
    public Class getEntityServiceClass() {
        return this.entityServiceClass;
    }

    @Override
    public void setEntityServiceClass(Class newClass) {
        this.entityServiceClass=newClass;
    }

    public JsonNode createDefaultSdfFileImport(Map<String, NCATSFileUtils.InputFieldStatistics> map) {
        log.trace("in createDefaultSdfFileImport");
        Set<String> fieldNames =map.keySet();
        ObjectNode topLevelReturn = JsonNodeFactory.instance.objectNode();
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        ObjectNode structureNode = JsonNodeFactory.instance.objectNode();
        structureNode.put(ACTION_NAME, "structure_and_moieties");
        structureNode.set(ACTION_PARAMETERS, createMolfileMap());

        result.add(structureNode);
        fieldNames.forEach(f -> {
            ObjectNode actionNode = JsonNodeFactory.instance.objectNode();
            if (f.toUpperCase(Locale.ROOT).contains("NAME") || f.toUpperCase(Locale.ROOT).contains("SYNONYM")) {
                actionNode.put(ACTION_NAME, "common_name");// +createCleanFieldName(f));
                ObjectNode mapNode = createNameMap(f, null, null);
                actionNode.set(ACTION_PARAMETERS, mapNode);
            } else if(looksLikeProperty(f)) {
                actionNode.put(ACTION_NAME, "property_import");
                ObjectNode mapNode = createPropertyMap(f);
                actionNode.set(ACTION_PARAMETERS, mapNode);
            } else {
                actionNode.put(ACTION_NAME, "code_import");//  +createCleanFieldName(f));
                ObjectNode mapNode = createCodeMap(f, "PRIMARY");
                actionNode.set(ACTION_PARAMETERS, mapNode);
            }
            result.add(actionNode);
        });
        result.add(createDefaultReferenceNode());
        topLevelReturn.set("actions", result);
        return topLevelReturn;
    }

    private boolean looksLikeProperty(String fieldName) {
        List<String> propertyWords = Arrays.asList("melting", "boiling","molecular", "density", "pka");
        return propertyWords.stream().anyMatch(p->fieldName.toUpperCase(Locale.ROOT).contains(p.toUpperCase(Locale.ROOT)));
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

    public ObjectNode createNameMap(String nameField, String nameType, String language) {
        log.trace("in createNameMap");
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        if (nameType == null || nameType.length() == 0) {
            nameType = "cn";
        }
        if(language==null || language.length()==0) {
            language="en";
        }
        mapNode.put("name", String.format("{{%s}}", nameField));
        mapNode.put("nameType", nameType);
        mapNode.put("lang", language);
        ArrayNode refs = JsonNodeFactory.instance.arrayNode();
        refs.add(String.format("[[%s]]", SIMPLE_REF));
        mapNode.set("referenceUUIDs", refs);
        return mapNode;
    }

    public ObjectNode createPropertyMap(String fieldName) {
        log.trace("in createPropertyMap");
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        mapNode.put("name", fieldName);
        mapNode.put("propertyType", "chemical|physical");
        mapNode.put("valueRange", String.format("{{%s}}", fieldName));
        mapNode.put("valueUnits", "");
        return mapNode;
    }

    public ObjectNode createMolfileMap() {
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        mapNode.put("molfile", "{{molfile}}");
        ArrayNode refs = JsonNodeFactory.instance.arrayNode();
        refs.add(String.format("[[%s]]", SIMPLE_REF));
        mapNode.set("referenceUUIDs", refs);
        return mapNode;
    }


}
