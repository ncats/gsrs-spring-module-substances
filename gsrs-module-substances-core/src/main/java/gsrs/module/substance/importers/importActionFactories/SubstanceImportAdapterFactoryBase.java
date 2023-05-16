package gsrs.module.substance.importers.importActionFactories;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactory;
import gsrs.importer.ImportFieldMetadata;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.imports.ActionConfigImpl;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterFactory;
import gsrs.imports.ImportAdapterStatistics;
import gsrs.module.substance.importers.model.SDRecordContext;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.importers.InputFieldStatistics;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SubstanceImportAdapterFactoryBase implements ImportAdapterFactory<Substance> {

    public final static String SIMPLE_REFERENCE_ACTION = "public_reference";

    //** TYLER ADDING SPECIAL START
    public static final Pattern SDF_RESOLVE = Pattern.compile("\\{\\{([^\\}]*)\\}\\}");
    public static final Pattern SPECIAL_RESOLVE = Pattern.compile("\\[\\[([^\\]]*)\\]\\]");

    protected ImportAdapterStatistics statistics;

    protected Map<String, MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> registry = new ConcurrentHashMap<>();

    protected List<ActionConfigImpl> fileImportActions;

    public final static String ACTION_NAME = "actionName";

    public final static String FILE_FIELD ="fileField";

    public final static String ACTION_PARAMETERS = "actionParameters";

    public final static String SIMPLE_REF = "UUID_1";

    public final static String CATALOG_REFERENCE = "CATALOG";
    public final static String REFERENCE_INSTRUCTION = "INSERT REFERENCE CITATION HERE";
    public final static String REFERENCE_ID_INSTRUCTION = "INSERT REFERENCE ID HERE";

    private Class entityServiceClass;

    @Override
    public String getAdapterName() {
        return null;
    }

    @Override
    public String getAdapterKey() {
        return null;
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return null;
    }

    @Override
    public void setSupportedFileExtensions(List<String> extensions) {

    }

    @Override
    public ImportAdapter<Substance> createAdapter(JsonNode adapterSettings) {
        return null;
    }

    @Override
    public ImportAdapterStatistics predictSettings(InputStream is, ObjectNode settings) {
        return null;
    }

    @Override
    public void setFileName(String fileName) {

    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public Class getStagingAreaService() {
        return null;
    }

    @Override
    public void setStagingAreaService(Class stagingService) {

    }

    @Override
    public Class getStagingAreaEntityService() {
        return null;
    }

    @Override
    public void setStagingAreaEntityService(Class stagingAreaEntityService) {

    }

    @Override
    public List<Class> getEntityServices() {
        return null;
    }

    @Override
    public void setEntityServices(List<Class> services) {

    }
    @Override
    public Class getEntityServiceClass() {
        return this.entityServiceClass;
    }

    @Override
    public void setEntityServiceClass(Class newClass) {
        this.entityServiceClass=newClass;
    }

    @Override
    public void setInputParameters(JsonNode parameters) {

    }

    @Override
    public String getDescription() {
        return "Generalized file importer. (this message is expected NOT to appear!)";
    }

    @Override
    public void setDescription(String description) {
        log.info("general setDescription method called");
    }

    public List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> getMappingActions(JsonNode adapterSettings) {
        List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actions = new ArrayList<>();
        adapterSettings.get("actions").forEach(js -> {
            String actionName = js.get("actionName").asText();
            JsonNode actionParameters = js.get("actionParameters");
            ObjectMapper mapper = new ObjectMapper();
            //log.trace("about to call convertValue");
            Map<String, Object> params = mapper.convertValue(actionParameters, new TypeReference<Map<String, Object>>() {});
            //log.trace("Finished call to convertValue");
            MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> action = null;
            try {
                log.trace("looking for action {}; registry size: {}", actionName, registry.size());
                MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> mappingActionFactory = registry.get(actionName);
                if (mappingActionFactory instanceof BaseActionFactory && statistics != null) {
                    ((BaseActionFactory) mappingActionFactory).setAdapterSchema(statistics.getAdapterSchema());
                    log.trace("called setAdapterSchema");
                }else if(this.getFileName()!=null ){
                    ObjectNode adapterSchema = JsonNodeFactory.instance.objectNode();
                    adapterSchema.put("fileName", getFileName());
                    ((BaseActionFactory) mappingActionFactory).setAdapterSchema(adapterSchema);
                    log.trace("passed file name to baseActionFactory");
                }
                log.trace("mappingActionFactory: " + mappingActionFactory);
                if (mappingActionFactory != null) {
                    AutowireHelper.getInstance().autowireAndProxy(mappingActionFactory);
                    action = mappingActionFactory.create(params);
                    actions.add(action);
                } else {
                    log.error("No action found for " + actionName);
                }
            } catch (Exception ex) {
                log.error("Error in getMappingActions: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        return actions;
    }

    protected void defaultInitialize(){}


    public void setFileImportActions(List<ActionConfigImpl> fileImportActions) {
        log.trace("setFileImportActions");
        this.fileImportActions = fileImportActions;
    }

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
                log.trace(" handling actionName: {}; class: {}", actionName, actionClass.getName());
                MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> mappingActionFactory;
                try {
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    mappingActionFactory = (MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>) mapper.convertValue(fia,
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
        mapNode.put("langCV", "LANGUAGE");
        mapNode.put("displayName", false);
        mapNode.put("nameTypeCV", "NAME_TYPE");
        ArrayNode refs = JsonNodeFactory.instance.arrayNode();
        refs.add(String.format("[[%s]]", SIMPLE_REF));
        mapNode.set("referenceUUIDs", refs);
        return mapNode;
    }

    public ObjectNode createPropertyMap(String fieldName) {
        log.trace("in createPropertyMap");
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        mapNode.put("name", fieldName);
        mapNode.put("propertyType", "CHEMICAL");
        mapNode.put("valueRange", String.format("{{%s}}", fieldName));
        mapNode.put("propertyTypeCV", "PROPERTY_TYPE");
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

    public ObjectNode createProteinSequenceMap(String sequenceFieldName) {
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        mapNode.put("proteinSequence", "{{" + sequenceFieldName+ "}}");
        return mapNode;
    }

    public ObjectNode createNucleicAcidMap(String sequenceFieldName) {
        ObjectNode mapNode= JsonNodeFactory.instance.objectNode();
        mapNode.put("nucleicAcidSequence", "{{" + sequenceFieldName+ "}}");
        return mapNode;
    }

    public ObjectNode createNoteMap(String noteFieldName) {
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        mapNode.put("note", "{{" + noteFieldName + "}}");
        return mapNode;
    }

    public ObjectNode createCodeMap(String codeSystem, String codeType) {
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        if (codeType == null || codeType.length() == 0) {
            codeType = "PRIMARY";
        }
        mapNode.put("codeSystem", codeSystem);
        mapNode.put("code", String.format("{{%s}}", codeSystem));
        mapNode.put("codeType", codeType);
        mapNode.put("codeSystemCV", "CODE_SYSTEM");
        mapNode.put("codeTypeCV", "CODE_TYPE");
        ArrayNode refs = JsonNodeFactory.instance.arrayNode();
        refs.add(String.format("[[%s]]", SIMPLE_REF));
        mapNode.set("referenceUUIDs", refs);
        return mapNode;
    }

    protected boolean looksLikeProperty(String fieldName) {
        List<String> propertyWords = Arrays.asList("melting", "boiling","molecular", "density", "pka", "logp", "logd", "hbond",
                "tpsa", "count", "rotatable", "mass", "formula");
        return propertyWords.stream().anyMatch(p->fieldName.toUpperCase(Locale.ROOT).contains(p.toUpperCase(Locale.ROOT)));
    }

    protected boolean looksLikeCode(String fieldName) {
        List<String> codeIndicatorsPartial = Arrays.asList("CAS", "EINECS","CHEMBL", "CHEBI", "NSC", "NCI", "ANUM", "PUBCHEM_COMPOUND_CID");
        List<String> codeIndicatorsFull = Arrays.asList("RN", "EC","NCIT", "RXCUI", "NCBI", "GRIN", "MPNS", "INN_ID", "USAN_ID");
        return codeIndicatorsPartial.stream().anyMatch(p->fieldName.toUpperCase(Locale.ROOT).contains(p.toUpperCase(Locale.ROOT)))
                ||codeIndicatorsFull.stream().anyMatch(p2->fieldName.toUpperCase().equals(p2));
    }
    protected boolean looksLikeProteinSequence(String fieldName) {
        if(fieldName.toUpperCase().contains("PROTEIN") && fieldName.toUpperCase().contains("SEQUENCE")){
            return true;
        }
        return false;
    }

    protected boolean looksLikeNucleicAcidSequence(String fieldName) {
        if(fieldName.toUpperCase().contains("NUCLEIC") && fieldName.toUpperCase().contains("ACID") && fieldName.toUpperCase().contains("SEQUENCE")){
            return true;
        }
        return false;
    }
    protected JsonNode createDefaultReferenceReferenceNode() {
        ArrayNode parameters = JsonNodeFactory.instance.arrayNode();
        parameters.add(String.format("[[%s]]", SIMPLE_REF));
        return parameters;
    }

    protected JsonNode createDefaultReferenceNode() {
        ObjectNode referenceNode = JsonNodeFactory.instance.objectNode();
        referenceNode.put(ACTION_NAME, SIMPLE_REFERENCE_ACTION);

        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        parameters.put("docType", CATALOG_REFERENCE);
        parameters.put("citation", REFERENCE_INSTRUCTION);
        parameters.put("referenceID", REFERENCE_ID_INSTRUCTION);
        parameters.put("uuid", String.format("[[%s]]", SIMPLE_REF));
        parameters.put("publicDomain", true);
        referenceNode.set(ACTION_PARAMETERS, parameters);
        referenceNode.put("label", "Create Reference");

        return referenceNode;
    }

    /*
simplified overload that uses the identity function as an encoder
 */
    public static String resolveParameter(SDRecordContext rec, String inp) {
        return resolveParameter(rec, inp, s -> s);
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
        } else {
            inp= replacePattern(inp, SDF_RESOLVE, (p)-> rec.getProperty(p).map(encoder));
        }
        inp = replacePattern(inp, SPECIAL_RESOLVE, (p) -> rec.resolveSpecial(p).map(encoder));
        return inp;
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
            log.error("Error in resolveParametersMap ",  ex);
            throw ex;
        }
    }

    public Set<ImportFieldMetadata> createMetadata(Set<String> fieldNames) {
        Set<ImportFieldMetadata> fieldMetadata = new HashSet<>();
        fieldNames.forEach(fn->{
            fieldMetadata.add(ImportFieldMetadata.builder()
                    .fieldName(fn)
                    .statistics(new InputFieldStatistics(fn)) //todo: fill in real data... will require additional parms to this method
                    .build());
        });
        return fieldMetadata;
    }

}