package example.imports;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import gsrs.dataexchange.model.MappingActionFactory;
import gsrs.imports.ActionConfigImpl;
import gsrs.imports.CodeProcessorFieldImpl;
import gsrs.imports.ImportAdapter;
import gsrs.module.substance.importers.DelimTextImportAdapter;
import gsrs.module.substance.importers.DelimTextImportAdapterFactory;
import gsrs.module.substance.importers.importActionFactories.NameExtractorActionFactory;
import gsrs.module.substance.importers.importActionFactories.ProteinSequenceExtractorActionFactory;
import gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase;
import gsrs.module.substance.importers.model.PropertyBasedDataRecordContext;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.models.v1.ProteinSubstance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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

        ImportAdapter<AbstractSubstanceBuilder> importAdapter= factory.createAdapter(adapterSettings);
        Assertions.assertTrue(importAdapter instanceof DelimTextImportAdapter);
        DelimTextImportAdapter delimTextImportAdapter = (DelimTextImportAdapter) importAdapter;
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
        actionParameters.put("substanceClass", ProteinSubstance.class);
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
        nameNode.set("actionParameters", adapter2Parameters);
        actionListNode.add(nameNode);
        
        ObjectNode adapterSettings = JsonNodeFactory.instance.objectNode();
        adapterSettings.set("actions", actionListNode);
        ObjectNode generalParameters = JsonNodeFactory.instance.objectNode();
        generalParameters.put("substanceClass", "Protein");
        generalParameters.put("removeQuotes", true);
        adapterSettings.set("parameters", generalParameters);

        ImportAdapter<AbstractSubstanceBuilder> importAdapter= factory.createAdapter(adapterSettings);
        Assertions.assertTrue(importAdapter instanceof DelimTextImportAdapter);
        DelimTextImportAdapter delimTextImportAdapter = (DelimTextImportAdapter) importAdapter;

        String fileName = "testText/3proteins.csv";
        File dataFile = new ClassPathResource(fileName).getFile();
        log.trace("using dataFile.getAbsoluteFile(): " + dataFile.getAbsoluteFile());
        InputStream fis = new FileInputStream(dataFile.getAbsoluteFile());
        String fileEncoding = "UTF-8";
        Stream<AbstractSubstanceBuilder> substanceBuilderStream= delimTextImportAdapter.parse(fis, fileEncoding);
        List<ProteinSubstance> proteinSubstances = substanceBuilderStream
                .map(p->((ProteinSubstanceBuilder)p).build())
                .collect(Collectors.toList());
        Assertions.assertTrue(proteinSubstances.stream().allMatch(p->p.protein.subunits.get(0).sequence.length()>0));
        Assertions.assertTrue(proteinSubstances.stream().allMatch(p->p.names.get(0).name.length()>0));
        Assertions.assertEquals(3, proteinSubstances.size());

    }
}
