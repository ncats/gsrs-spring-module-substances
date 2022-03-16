package gsrs.module.substance.importers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.GinasCommonData;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Property;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

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
        public String getMolfileName();

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

        @Override
        public String getMolfileName() {
            return c.getName();
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
		return resolveParameter(rec,inp,s->s);
    }
    
    public static String resolveParameter(SDRecordContext rec, String inp, Function<String,String> encoder){
        inp = replacePattern(inp,SDF_RESOLVE, (p)->{
            if(p.equals("molfile"))return Optional.ofNullable(rec.getStructure()).map(encoder);
            if(p.equals("molfile_name"))return Optional.ofNullable(rec.getMolfileName()).map(encoder);
            return rec.getProperty(p).map(encoder);
        });
        inp = replacePattern(inp,SPECIAL_RESOLVE, (p)->rec.resolveSpecial(p).map(encoder));
        return inp;
    }

    public static List<String> resolveParameters(SDRecordContext rec, List<String> inputList) {
        return inputList.stream().map(s -> resolveParameter(rec, s)).collect(Collectors.toList());
    }

    public static Map<String, Object> resolveParametersMap(SDRecordContext rec, Map<String, Object> inputMap) throws Exception {
        ObjectMapper om = new ObjectMapper();
        Map<String, Object> newMap;
        JsonNode jsn = om.valueToTree(inputMap);
        String json = resolveParameter(rec, jsn.toString(), s->{
            String m = om.valueToTree(s).toString();
            return m.substring(1,m.length()-1);
        });
        newMap = (Map<String, Object>) (om.readValue(json, LinkedHashMap.class));
        return newMap;
    }


    private static void assignReferences(GinasAccessReferenceControlled object, Object referenceList) {
        List<String> refs  = (List<String>) referenceList;
        if( refs != null ) {
            refs.forEach(r -> object.addReference(r));
        }
    }
	
    private static void doBasicsImports(GinasAccessControlled object, Map<String, Object> params) {
	if(object instanceof GinasAccessReferenceControlled){
		assignReferences((GinasAccessReferenceControlled)object, params.getOrDefault("referenceUUIDs", null));
	}
	if(object instanceof GinasCommonData){
		if (params.get("uuid") != null) {
                	((GinasCommonData)object).setUuid(UUID.fromString(params.get("uuid").toString()));
        	}
	}
	if (params.get("access") != null) {
		//TODO: need to deal with this somehow, not sure how yet because of the
		//need to use Group objects	
	}
    }

    public static class NameExtractorActionFactory implements MappingActionFactory<Substance, SDRecordContext> {
        public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
            return (sub, sdRec) -> {
                Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
                
                boolean splitNames=Boolean.parseBoolean(params.getOrDefault("split_names", "true").toString());
                
                String suppliedName=(String) params.get("name");
                
                if(splitNames) {
                    for(String sn: suppliedName.trim().split("\n")) {
                        if(sn.isEmpty())continue;
                        sn=sn.trim();
                        Name n = new Name();
			n.setName(sn);
                        doBasicsImports(n,params);
                        //TODO: more params
                        sub.names.add(n);
                    }
                }else {
                    Name n = new Name();
                    n.setName(suppliedName.trim());
		    doBasicsImports(n,params);
                    sub.names.add(n);
                }
                return sub;
            };
        }
    }

    public static class CodeExtractorActionFactory implements MappingActionFactory<Substance, SDRecordContext> {
        public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
            return (sub, sdRec) -> {
                Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
                Code c = new Code((String) params.get("codeSystem"), (String) params.get("code"));
                c.type = (String) params.get("codeType");
                doBasicsImports(c,params);
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
		Optional.ofNullable(params.get("url")).ifPresent(url->{
                    r.url=url.toString();
                });
                Optional.ofNullable(params.get("referenceID")).ifPresent(rid->{
                    r.id=rid.toString();
                });
                doBasicsImports(r,params);
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
                doBasicsImports(s,params);
                ((ChemicalSubstance) sub).setStructure(s);
                return sub;
            };
        }
    }

    public static class NotesExtractorActionFactory implements MappingActionFactory<Substance, SDRecordContext> {
        public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
            return (sub, sdRec) -> {
                Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
		Note n = new Note();
		n.note = (String) params.get("note");
                doBasicsImports(n,params);
                //TODO: more params
                sub.notes.add(n);
                return sub;
            };
        }
    }
	
    public static class PropertyExtractorActionFactory implements MappingActionFactory<Substance, SDRecordContext> {
        public MappingAction<Substance, SDRecordContext> create(Map<String, Object> abstractParams) {
            return (sub, sdRec) -> {
                Map<String, Object> params = resolveParametersMap(sdRec, abstractParams);
		Property p = new Property();
		p.setName((String) params.get("name"));
                doBasicsImports(p,params);
		Amount amt=new Amount();
		p.setValue(amt);
		Optional.ofNullable(params.get("valueAverage")).ifPresent(aa->{
                    amt.average = (Double.parseDouble(aa));
                });
		Optional.ofNullable(params.get("valueLow")).ifPresent(aa->{
                    amt.low = (Double.parseDouble(aa));
                });
		Optional.ofNullable(params.get("valueHigh")).ifPresent(aa->{
                    amt.high = (Double.parseDouble(aa));
                });
		Optional.ofNullable(params.get("valueNonNumeric")).ifPresent(aa->{
                    amt.nonNumericValue = aa;
                });
		Optional.ofNullable(params.get("valueUnits")).ifPresent(aa->{
                    amt.units = aa;
                });
		Optional.ofNullable(params.get("defining")).ifPresent(aa->{
                    amt.defining = Boolean.parseBoolean(params.getOrDefault("defining", "false").toString());
                });		    
                //TODO: more params
                sub.notes.add(n);
                return sub;
            };
        }
    }

    private static Map<String, MappingActionFactory<Substance, SDRecordContext>> registry = new ConcurrentHashMap<>();

    static {
        registry.put("common_name", new NameExtractorActionFactory());
        registry.put("code_import", new CodeExtractorActionFactory());
        registry.put("structure_and_moieties", new StructureExtractorActionFactory());
	registry.put("note_import", new NotesExtractorActionFactory());
	registry.put("property_import", new PropertyExtractorActionFactory());
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
