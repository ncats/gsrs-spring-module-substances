package gsrs.module.substance.importers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.module.substance.utils.NCATSFileUtils;
import ix.ginas.models.CommonDataElementOfCollection;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class SDFImportAdaptorFactory implements AbstractImportSupportingGsrsEntityController.ImportAdapterFactory<Substance> {
    public final static String SIMPLE_REF = "UUID_1";
    public final static String SIMPLE_REFERENCE_ACTION = "public_reference";
    public final static String ACTION_NAME = "actionName";
    public final static String ACTION_PARAMETERS = "actionParameters";
    public final static String CATALOG_REFERENCE = "CATALOG";
    public final static String REFERENCE_INSTRUCTION = "INSERT REFERENCE CITATION HERE";
    public final static String REFERENCE_ID_INSTRUCTION = "INSERT REFERENCE ID HERE";
	public final static String SDF_FIELD_LIST = "SDF Fields";
    //** ADDING ABSTRACT LAYERS START
    public static interface MappingAction<T, U> {
        public T act(T building, U source) throws Exception;
    }

    public static interface MappingActionFactory<T, U> {
        public MappingAction<T, U> create(Map<String, Object> params);
    }
    //** ADDING ABSTRACT LAYERS END

    //** TYLER ADDING SPECIAL START
    public static final Pattern SDF_RESOLVE = Pattern.compile("\\{\\{([^\\}]*)\\}\\}");
    public static final Pattern SPECIAL_RESOLVE = Pattern.compile("\\[\\[([^\\]]*)\\]\\]");

    public static interface SDRecordContext {
        public String getStructure();

        public Optional<String> getProperty(String name);

        public List<String> getProperties();

        public Optional<String> resolveSpecial(String name);
    }

    public static class ChemicalBackedSDRecordContext implements SDRecordContext {
        private Chemical c;
        private Map<String, String> specialProps = new HashMap<>();

        public ChemicalBackedSDRecordContext(Chemical c) {
            this.c = c;
        }

        @Override
        public String getStructure() {
            try {
                return c.toMol();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public Optional<String> getProperty(String name) {
            return Optional.ofNullable(c.getProperty(name));
        }

        @Override
        public List<String> getProperties() {
            return c.getProperties().keySet().stream().collect(Collectors.toList());
        }

        @Override
        public Optional<String> resolveSpecial(String name) {

            if (name.startsWith("UUID_")) {
                String ret = specialProps.computeIfAbsent(name, k -> {
                    return UUID.randomUUID().toString();
                });
                return Optional.ofNullable(ret);
            }
            return Optional.empty();
        }
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

    public static String resolveParameter(SDRecordContext rec, String inp){
		inp = replacePattern(inp,SDF_RESOLVE, (p)->{
			if(p.equals("molfile"))return Optional.ofNullable(rec.getStructure());
			return rec.getProperty(p);
		});
		inp = replacePattern(inp,SPECIAL_RESOLVE, (p)->rec.resolveSpecial(p));
        return inp;
    }

    public static List<String> resolveParameters(SDRecordContext rec, List<String> inputList) {
        return inputList.stream().map(s -> resolveParameter(rec, s)).collect(Collectors.toList());
    }

    public static Map<String, Object> resolveParametersMap(SDRecordContext rec, Map<String, Object> inputMap) throws Exception {
        ObjectMapper om = new ObjectMapper();
        Map<String, Object> newMap;
        JsonNode jsn = om.valueToTree(inputMap);
        String json = resolveParameter(rec, jsn.toString());
        newMap = (Map<String, Object>) (om.readValue(json, LinkedHashMap.class));
        return newMap;
    }


    public static class NameExtractorActionFactory implements MappingActionFactory<Substance, SDRecordContext> {
        public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
            return (sub, sdRec) -> {
                Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
                Name n = new Name();
                n.setName((String) params.get("name"));
                assignReferences(n, params.getOrDefault("referenceUUIDs", null));
                if (params.get("uuid") != null) {
                    n.uuid = UUID.fromString(params.get("uuid").toString());
                }
                //TODO: more params
                sub.names.add(n);
                return sub;
            };
        }
    }

    private static void assignReferences(CommonDataElementOfCollection object, Object referenceList) {
        if( referenceList != null ) {
            Arrays.asList((String[]) referenceList).forEach(r -> object.addReference(r));
        }
    }

    public static class CodeExtractorActionFactory implements MappingActionFactory<Substance, SDRecordContext> {
        public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
            return (sub, sdRec) -> {
                Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
                Code c = new Code((String) params.get("codeSystem"), (String) params.get("code"));
                c.type = (String) params.get("codeType");
                assignReferences(c, params.getOrDefault("referenceUUIDs", null));
                if (params.get("uuid") != null) {
                    c.uuid = UUID.fromString(params.get("uuid").toString());
                }
                //TODO: more params
                sub.addCode(c);
                return sub;
            };
        }
    }

    public static class ReferenceExtractorActionFactory implements MappingActionFactory<Substance, SDRecordContext> {
        public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
            return (sub, sdRec) -> {
                Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
                Reference r = new Reference();
                r.citation = (String) params.get("citation");
                r.docType = (String) params.get("docType");
                r.uuid = params.get("referenceID") ==null? UUID.randomUUID() : UUID.fromString((String) params.get("referenceID"));
                if (params.get("uuid") != null) {
                    r.uuid = UUID.fromString(params.get("uuid").toString());
                }
                //TODO: more params
                sub.addReference(r);
                return sub;
            };
        }
    }

    public static class StructureExtractorActionFactory implements MappingActionFactory<Substance, SDRecordContext> {
        public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
            return (sub, sdRec) -> {
                Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
                GinasChemicalStructure s = new GinasChemicalStructure();
                s.molfile = (String) params.get("molfile");
                //s setReferenceID(params.get("referenceID"));
                          /*if(params.get("uuid")!=null){
                            s.setUuid(UUID.fromString(params.get("uuid").toString()));
                          }*/
                //TODO: more params like stereo? where do moieties things?
                ((ChemicalSubstance) sub).setStructure(s);
                return sub;
            };
        }
    }


    private static Map<String, MappingActionFactory<Substance, SDRecordContext>> registry = new ConcurrentHashMap<>();

    static {
        registry.put("common_name", new NameExtractorActionFactory());
        registry.put("code_import", new CodeExtractorActionFactory());
        registry.put("structure_and_moieties", new StructureExtractorActionFactory());
        registry.put(SIMPLE_REFERENCE_ACTION, new ReferenceExtractorActionFactory());
    }


    public static List<MappingAction<Substance, SDRecordContext>> getMappingActions(JsonNode adapterSettings) {
        List<MappingAction<Substance, SDRecordContext>> actions = new ArrayList<>();
        adapterSettings.get("actions").forEach(js -> {
            String actionName = js.get("actionName").asText();
            JsonNode actionParameters = js.get("actionParameters");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> params = mapper.convertValue(actionParameters, new TypeReference<Map<String, Object>>() {
            });
            MappingAction<Substance, SDRecordContext> action =
                    (MappingAction<Substance, SDRecordContext>) registry.get(actionName).create(params);
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

    @Override
    public AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        List<MappingAction<Substance, SDRecordContext>> actions = getMappingActions(adapterSettings);
        AbstractImportSupportingGsrsEntityController.ImportAdapter sDFImportAdapter = new SDFImportAdapter(actions);
        return sDFImportAdapter;
    }

    @Override
    public AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics predictSettings(InputStream is) {
        Set<String> fields = null;
        try {
            fields=getFieldsForFile(is);
            AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics statistics = new AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics();
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.putPOJO(SDF_FIELD_LIST, fields);
            statistics.setAdapterSchema(node);
            statistics.setAdapterSettings(createDefaultSdfFileImport(fields));
            return statistics;
        } catch (IOException ex) {
            log.error("error reading list of fields from SD file: " + ex.getMessage());
        }
        return null;
    }


    public JsonNode createDefaultSdfFileImport(Set<String> fieldNames) {
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
                ObjectNode mapNode= createCodeMap(f, "PRIMARY");
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

    public Set<String> getFieldsForFile( InputStream input) throws IOException {
        log.trace("starting in fieldsForSDF");
        Set<String> fields = NCATSFileUtils.getSdFileFields(input);
        log.trace("total fields: " + fields.size());
        return fields;
    }

    public ObjectNode createCodeMap(String codeSystem, String codeType) {
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        if(codeType== null || codeType.length()==0){
            codeType= "PRIMARY";
        }
        mapNode.put("codeSystem", codeSystem);
        mapNode.put("code", String.format("{{%s}}", codeSystem));
        mapNode.put("codeType", codeType);
        return mapNode;
    }

    public ObjectNode createNameMap(String nameField, String nameType) {
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        if(nameType== null || nameType.length()==0){
            nameType= "cn";
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
