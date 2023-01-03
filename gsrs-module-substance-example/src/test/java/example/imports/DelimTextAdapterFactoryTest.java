package example.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import gsrs.dataexchange.model.MappingAction;
import gsrs.dataexchange.model.MappingActionFactory;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.imports.ActionConfigImpl;
import gsrs.imports.CodeProcessorFieldImpl;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterStatistics;
import gsrs.module.substance.importers.DelimTextImportAdapter;
import gsrs.module.substance.importers.DelimTextImportAdapterFactory;
import gsrs.module.substance.importers.importActionFactories.CodeExtractorActionFactory;
import gsrs.module.substance.importers.importActionFactories.NameExtractorActionFactory;
import gsrs.module.substance.importers.importActionFactories.ProteinSequenceExtractorActionFactory;
import gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DelimTextAdapterFactoryTest {

    /*
    Make sure the initialize method populates the registry (a Map)
     */
    @Test
    public void testInitialize() throws NoSuchFieldException, IllegalAccessException {
        List<ActionConfigImpl> simpleConfig = new ArrayList<>();
        ActionConfigImpl actionConfig = new ActionConfigImpl();
        actionConfig.setActionClass(NameExtractorActionFactory.class);
        actionConfig.setActionName("name_import");
        List<CodeProcessorFieldImpl> fields = new ArrayList<>();
        CodeProcessorFieldImpl codeProcessorField = new CodeProcessorFieldImpl();
        codeProcessorField.setFieldName("proteinSequence");
        codeProcessorField.setRequired(true);
        codeProcessorField.setFieldLabel("Protein Sequence Field");
        codeProcessorField.setFieldType(String.class);
        codeProcessorField.setExpectedToChange(true);
        codeProcessorField.setDefaultValue("PROTEIN_SEQUENCE");
        actionConfig.setFields(fields);
        Map<String, Object> actionParameters = new HashMap<>();
        actionParameters.put("substanceClass", ProteinSubstance.class);
        actionConfig.setParameters(actionParameters);
        simpleConfig.add(actionConfig);
        DelimTextImportAdapterFactory factory = new DelimTextImportAdapterFactory();
        factory.setFileImportActions(simpleConfig);
        factory.initialize();

        Field registryField= SubstanceImportAdapterFactoryBase.class.getDeclaredField("registry");
        registryField.setAccessible(true);
        Map<String, MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> reg = (Map<String, MappingActionFactory<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>>)
                registryField.get(factory);
        Assertions.assertEquals(1, reg.size());
    }

    /*
    Confirm ability to create a delimTextImportAdapter
     */
    @Test
    public void testCreate()  {
        List<ActionConfigImpl> simpleConfig = new ArrayList<>();
        ActionConfigImpl actionConfig = new ActionConfigImpl();
        actionConfig.setActionClass(NameExtractorActionFactory.class);
        actionConfig.setActionName("protein_import");
        List<CodeProcessorFieldImpl> fields = new ArrayList<>();
        CodeProcessorFieldImpl codeProcessorField = new CodeProcessorFieldImpl();
        codeProcessorField.setFieldName("proteinSequence");
        codeProcessorField.setRequired(true);
        codeProcessorField.setFieldLabel("Protein Sequence Field");
        codeProcessorField.setFieldType(String.class);
        codeProcessorField.setExpectedToChange(true);
        codeProcessorField.setDefaultValue("PROTEIN_SEQUENCE");
        actionConfig.setFields(fields);
        Map<String, Object> actionParameters = new HashMap<>();
        actionParameters.put("substanceClass", ProteinSubstance.class);
        actionConfig.setParameters(actionParameters);
        simpleConfig.add(actionConfig);
        DelimTextImportAdapterFactory factory = new DelimTextImportAdapterFactory();
        factory.setFileImportActions(simpleConfig);
        factory.initialize();

        ArrayNode actionListNode = JsonNodeFactory.instance.arrayNode();
        ObjectNode actionNode = JsonNodeFactory.instance.objectNode();
        TextNode actionNameNode = JsonNodeFactory.instance.textNode("protein_import");
        actionNode.set("actionName", actionNameNode);
        actionListNode.add(actionNode);
        ObjectNode adapterSettings = JsonNodeFactory.instance.objectNode();
        adapterSettings.set("actions", actionListNode);

        ImportAdapter<Substance> importAdapter= factory.createAdapter(adapterSettings);
        Assertions.assertTrue(importAdapter instanceof DelimTextImportAdapter);
    }

    @Test
    public void testConstructor1() throws NoSuchFieldException, IllegalAccessException {
        List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actionSet = new ArrayList<>();
        String requiredClass="Chemical";
        Map<String, Object> parameterSet = new HashMap<>();
        parameterSet.put("substanceClassName", requiredClass);
        parameterSet.put("","");
        //parameterSet.put("","");
        DelimTextImportAdapter delimTextImportAdapter = new DelimTextImportAdapter(actionSet, parameterSet);
        Field substanceClassField= DelimTextImportAdapter.class.getDeclaredField("substanceClassName");
        substanceClassField.setAccessible(true);
        String receivedClass= (String) substanceClassField.get(delimTextImportAdapter);
        Assertions.assertEquals(requiredClass, receivedClass);
    }

    @Test
    public void testConstructor2() throws NoSuchFieldException, IllegalAccessException {
        List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actionSet = new ArrayList<>();
        String requiredDelim="$%$";
        Map<String, Object> parameterSet = new HashMap<>();
        parameterSet.put("lineValueDelimiter", requiredDelim);
        parameterSet.put("","");
        DelimTextImportAdapter delimTextImportAdapter = new DelimTextImportAdapter(actionSet, parameterSet);
        Field delimField= DelimTextImportAdapter.class.getDeclaredField("lineValueDelimiter");
        delimField.setAccessible(true);
        String receivedClass= (String) delimField.get(delimTextImportAdapter);
        Assertions.assertEquals(requiredDelim, receivedClass);
    }

    @Test
    public void testConstructor3() throws NoSuchFieldException, IllegalAccessException {
        List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> actionSet = new ArrayList<>();
        Boolean removeQuotesValue=true;
        Map<String, Object> parameterSet = new HashMap<>();
        parameterSet.put("removeQuotes", removeQuotesValue);
        //parameterSet.put("","");
        DelimTextImportAdapter delimTextImportAdapter = new DelimTextImportAdapter(actionSet, parameterSet);
        Field removeQuotesField= DelimTextImportAdapter.class.getDeclaredField("removeQuotes");
        removeQuotesField.setAccessible(true);
        Boolean receivedRemoveQuotes= (Boolean) removeQuotesField.get(delimTextImportAdapter);
        Assertions.assertEquals(removeQuotesValue, receivedRemoveQuotes);
    }

    /*
    Confirm ability to read data
    */
    @Test
    public void testParse() throws IOException {
        List<ActionConfigImpl> simpleConfig = new ArrayList<>();

        ActionConfigImpl proteinActionConfig = new ActionConfigImpl();
        proteinActionConfig.setActionClass(ProteinSequenceExtractorActionFactory.class);
        proteinActionConfig.setActionName("protein_import");
        List<CodeProcessorFieldImpl> fields = new ArrayList<>();
        CodeProcessorFieldImpl codeProcessorField = new CodeProcessorFieldImpl();
        codeProcessorField.setFieldName("proteinSequence");
        codeProcessorField.setRequired(true);
        codeProcessorField.setFieldLabel("Protein Sequence Field");
        codeProcessorField.setFieldType(String.class);
        codeProcessorField.setExpectedToChange(true);
        codeProcessorField.setDefaultValue("PROTEIN_SEQUENCE");
        fields.add(codeProcessorField);
        proteinActionConfig.setFields(fields);
        Map<String, Object> actionParameters = new HashMap<>();
        //actionParameters.put("substanceClass", "Protein");
        actionParameters.put("subunitDelimiter", "\\|");
        proteinActionConfig.setParameters(actionParameters);
        simpleConfig.add(proteinActionConfig);

        ActionConfigImpl nameActionConfig = new ActionConfigImpl();
        nameActionConfig.setActionClass(NameExtractorActionFactory.class);
        nameActionConfig.setActionName("common_name");
        List<CodeProcessorFieldImpl> fieldsName = new ArrayList<>();
        CodeProcessorFieldImpl nameField = new CodeProcessorFieldImpl();
        nameField.setFieldName("name");
        nameField.setRequired(true);
        nameField.setFieldLabel("Name");
        nameField.setFieldType(String.class);
        nameField.setExpectedToChange(true);
        fieldsName.add(nameField);

        CodeProcessorFieldImpl nameTypeField = new CodeProcessorFieldImpl();
        nameTypeField.setFieldName("nameType");
        nameTypeField.setRequired(true);
        nameTypeField.setFieldLabel("Name Type");
        nameTypeField.setFieldType(String.class);
        nameTypeField.setExpectedToChange(true);
        nameTypeField.setDefaultValue("cn");
        fieldsName.add(nameTypeField);
        nameActionConfig.setFields(fieldsName);
        simpleConfig.add(nameActionConfig);

        ActionConfigImpl rnCodeActionConfig = new ActionConfigImpl();
        rnCodeActionConfig.setActionClass(CodeExtractorActionFactory.class);
        rnCodeActionConfig.setActionName("cas_code");
        List<CodeProcessorFieldImpl> fieldsRn = new ArrayList<>();
        CodeProcessorFieldImpl rnField = new CodeProcessorFieldImpl();
        rnField.setFieldName("code");
        rnField.setRequired(true);
        rnField.setFieldLabel("Code");
        rnField.setFieldType(String.class);
        rnField.setExpectedToChange(true);
        fieldsRn.add(rnField);

        CodeProcessorFieldImpl rnTypeField = new CodeProcessorFieldImpl();
        rnTypeField.setFieldName("codeType");
        rnTypeField.setRequired(false);
        rnTypeField.setFieldLabel("Code Type");
        rnTypeField.setFieldType(String.class);
        rnTypeField.setExpectedToChange(false);
        rnTypeField.setDefaultValue("PRIMARY");
        fieldsRn.add(rnTypeField);

        CodeProcessorFieldImpl rnSystemField = new CodeProcessorFieldImpl();
        rnSystemField.setFieldName("codeSystem");
        rnSystemField.setRequired(true);
        rnSystemField.setFieldLabel("Code System");
        rnSystemField.setFieldType(String.class);
        rnSystemField.setExpectedToChange(false);
        rnSystemField.setDefaultValue("CAS");
        fieldsRn.add(rnSystemField);
        rnCodeActionConfig.setFields(fieldsRn);
        simpleConfig.add(rnCodeActionConfig);
        
        DelimTextImportAdapterFactory factory = new DelimTextImportAdapterFactory();
        factory.setFileImportActions(simpleConfig);
        factory.initialize();

        ArrayNode actionListNode = JsonNodeFactory.instance.arrayNode();
        ObjectNode actionNode = JsonNodeFactory.instance.objectNode();
        TextNode actionNameNode = JsonNodeFactory.instance.textNode("protein_import");
        actionNode.set("actionName", actionNameNode);
        ObjectNode adapter1Parameters = JsonNodeFactory.instance.objectNode();
        adapter1Parameters.put("proteinSequence","{{PROTEIN_SEQUENCE}}");
        actionNode.set("actionParameters", adapter1Parameters);
        actionListNode.add(actionNode);

        ObjectNode nameNode = JsonNodeFactory.instance.objectNode();
        TextNode nameActionNameNode = JsonNodeFactory.instance.textNode("common_name");
        nameNode.set("actionName", nameActionNameNode);
        ObjectNode adapter2Parameters = JsonNodeFactory.instance.objectNode();
        adapter2Parameters.put("name","{{DISPLAY_NAME}}");
        adapter2Parameters.put("displayName", true);
        adapter2Parameters.put("referenceUUIDs", "[[UUID_1]]");
        nameNode.set("actionParameters", adapter2Parameters);
        actionListNode.add(nameNode);

        ObjectNode rnNode = JsonNodeFactory.instance.objectNode();
        TextNode rnActionNameNode = JsonNodeFactory.instance.textNode("cas_code");
        rnNode.set("actionName", rnActionNameNode);
        ObjectNode adapterRn = JsonNodeFactory.instance.objectNode();
        adapterRn.put("code","{{RN}}");
        adapterRn.put("codeSystem","CAS");
        adapterRn.put("codeType","PRIMARY");
        rnNode.set("actionParameters", adapterRn);
        actionListNode.add(rnNode);

        ObjectNode refNode = JsonNodeFactory.instance.objectNode();
        TextNode refActionNameNode = JsonNodeFactory.instance.textNode("public_reference");
        refNode.set("actionName", refActionNameNode);
        ObjectNode adapterRef = JsonNodeFactory.instance.objectNode();
        adapterRef.put("docType","CATALOG");
        adapterRef.put("citation","INSERT REFERENCE CITATION HERE");
        adapterRef.put("referenceID", "INSERT REFERENCE ID HERE");
        adapterRef.put("uuid", "[[UUID_1]]");
        refNode.set("actionParameters", adapterRef);
        actionListNode.add(refNode);
        
        ObjectNode adapterSettings = JsonNodeFactory.instance.objectNode();
        adapterSettings.set("actions", actionListNode);
        ObjectNode generalParameters = JsonNodeFactory.instance.objectNode();
        generalParameters.put("substanceClassName", "Protein");
        generalParameters.put("removeQuotes", true);
        adapterSettings.set("parameters", generalParameters);

        ImportAdapter<Substance> importAdapter= factory.createAdapter(adapterSettings);
        Assertions.assertTrue(importAdapter instanceof DelimTextImportAdapter);
        DelimTextImportAdapter delimTextImportAdapter = (DelimTextImportAdapter) importAdapter;

        String fileName = "testText/3proteins.csv";
        File dataFile = new ClassPathResource(fileName).getFile();
        log.trace("using dataFile.getAbsoluteFile(): " + dataFile.getAbsoluteFile());
        InputStream fis = new FileInputStream(dataFile.getAbsoluteFile());
        String fileEncoding = "UTF-8";
        Stream<Substance> substanceBuilderStream= delimTextImportAdapter.parse(fis, fileEncoding);
        List<ProteinSubstance> proteinSubstances = substanceBuilderStream
                .map(p->((ProteinSubstance)p))
                .collect(Collectors.toList());
        Assertions.assertTrue(proteinSubstances.stream().anyMatch(p->p.names.get(0).name.equals("ASPARTOCIN")
                && p.protein.subunits.stream().anyMatch(s->s.sequence.equals("CYINNCPLG"))
                && p.codes.get(0).code.equals("4117-65-1") && p.codes.get(0).type.equals("PRIMARY")));
        Assertions.assertTrue(proteinSubstances.stream().anyMatch(p->p.protein.subunits.stream().anyMatch(s->s.sequence.equals("CYGRKKRRQRRR")) &&
                p.protein.subunits.stream().anyMatch(s->s.sequence.equals("CSFNSYELGSL"))));
        Assertions.assertEquals(3, proteinSubstances.size());
    }

    @Test
    public void basicSerializationTest() {
        Map<String, String> exampleData = new HashMap<>();
        exampleData.put("key1", "value one");
        exampleData.put("key2", "value two");
        exampleData.put("key3", "THREE");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.valueToTree(exampleData);
        ObjectNode node = (ObjectNode)jsonNode;
        Assertions.assertTrue( exampleData.keySet().stream().allMatch(k-> exampleData.get(k).equals(node.get(k).asText())));
    }

    @Test
    public void createSubstanceStreamTest() throws IOException {
        DelimTextImportAdapterFactory factory = new DelimTextImportAdapterFactory();
        factory.initialize();
        String fileName = "testText/3proteins.csv";
        File dataFile = new ClassPathResource(fileName).getFile();
        log.trace("using dataFile.getAbsoluteFile(): " + dataFile.getAbsoluteFile());
        InputStream fis = new FileInputStream(dataFile.getAbsoluteFile());
        String fileEncoding = "UTF-8";
        ObjectNode adapterSettings = JsonNodeFactory.instance.objectNode();
        adapterSettings.put("lineValueDelimiter", ",");
        adapterSettings.put("removeQuotes", true);
        adapterSettings.put("substanceClassName", "Protein");
        adapterSettings.put("linesToSkip", 0);
        factory.setInputParameters(adapterSettings);
        ImportAdapterStatistics settings = factory.predictSettings(fis);
        fis = new FileInputStream(dataFile.getAbsoluteFile());
        JsonNode adapter = settings.getAdapterSettings();
        log.trace("adapter: ");
        log.trace(adapter.toPrettyString());
        ImportAdapter<Substance> importAdapter = factory.createAdapter(adapter);
        Stream<Substance> substanceStream = importAdapter.parse(fis, Charset.defaultCharset().name());
        substanceStream.forEach(s -> {
            log.trace("full substance: ");
            String fullSubstanceJson =s.toFullJsonNode().toPrettyString();
            log.trace(fullSubstanceJson);
            System.out.println(fullSubstanceJson);
            Assertions.assertTrue(s.substanceClass.toString().contains("protein"));
            Assertions.assertTrue(s.codes.stream().noneMatch(c->c.code.length()==0));
            //Assertions.assertTrue( s.build().pro);
        });
    }

}
