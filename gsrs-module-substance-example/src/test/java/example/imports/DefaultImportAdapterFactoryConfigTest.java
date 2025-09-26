package example.imports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.GsrsFactoryConfiguration;
import gsrs.dataexchange.SubstanceStagingAreaEntityService;
import gsrs.dataexchange.model.MappingAction;
import gsrs.stagingarea.service.DefaultStagingAreaService;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.imports.*;
import gsrs.module.substance.importers.NSRSSDFImportAdapterFactory;
import gsrs.module.substance.importers.SDFImportAdapterFactory;
import gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;

public class DefaultImportAdapterFactoryConfigTest extends AbstractSubstanceJpaEntityTest {

    @Test
    public void testSetup() throws IllegalAccessException, NoSuchFieldException, JsonProcessingException {
        String substanceContext = "substances";
        //build up a complete configuration
        GsrsFactoryConfiguration config = new GsrsFactoryConfiguration();

        Map<String,Map<String, Map<String, Object>>> blankSubstanceContextHashMap = new HashMap<>();

        Map<String, Map<String, Map<String, Map<String, Object>>>> adapterConfig = new HashMap<>();
        Map<String, Object> oneAdapter = new HashMap<>();
        oneAdapter.put("importAdapterFactoryClass", "gsrs.module.substance.importers.SDFImportAdapterFactory");
        oneAdapter.put("order", 1000);
        oneAdapter.put("parentKey", "SDFImportAdapterFactory");
        oneAdapter.put("adapterName", "NSRS SDF Adapter");
        oneAdapter.put("extensions", Arrays.asList("sdf", "sd"));
        oneAdapter.put("parameters", buildConfigParameters());
        oneAdapter.put("stagingAreaServiceClass", gsrs.stagingarea.service.DefaultStagingAreaService.class);
        oneAdapter.put("entityServiceClass", "gsrs.dataexchange.SubstanceStagingAreaEntityService");
        Map<String, Map<String, Object>> adapters = new HashMap<>();
        // ix.ginas.export.exporterfactories.substances.list.SdfExporterFactory =
        adapters.put("SDFImportAdapterFactory", oneAdapter);

        // adapterConfig.put(substanceContext, null);
        adapterConfig.put(substanceContext, blankSubstanceContextHashMap);
        adapterConfig.get(substanceContext).put("list", adapters);
        config.setImportAdapterFactories(adapterConfig);
        ConfigBasedGsrsImportAdapterFactoryFactory factoryFactory = new ConfigBasedGsrsImportAdapterFactoryFactory();
        Field configField = factoryFactory.getClass().getDeclaredField("gsrsFactoryConfiguration"); //gsrs.imports.ConfigBasedGsrsImportAdapterFactoryFactory.
        configField.setAccessible(true);
        configField.set(factoryFactory, config);

        ObjectMapper mapper = new ObjectMapper();
        System.out.println("config: " + mapper.writeValueAsString(adapterConfig));

        List<ImportAdapterFactory<SubstanceBuilder>> adapterFactories = factoryFactory.newFactory(substanceContext,
                SubstanceBuilder.class);
        Assertions.assertEquals(1, adapterFactories.size());
    }

    @Test
    public void testSetupActions() throws Exception {
        List<ActionConfig> actionConfigs= buildTypedConfig();
        SDFImportAdapterFactory factory = new SDFImportAdapterFactory();
        factory.setFileImportActions(actionConfigs);
        factory.initialize();
        JsonNode dataNode = buildDataNode();

        List<MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext>> mappingActions= factory.getMappingActions(dataNode);
        Assertions.assertTrue(mappingActions.size()>0);
        System.out.println("class: " + mappingActions.get(0).getClass().getName());

    }

    @Test
    public void testConfigGeneration() throws JsonProcessingException {
        DefaultImportAdapterFactoryConfig config = new DefaultImportAdapterFactoryConfig();
        config.setImportAdapterFactoryClass(NSRSSDFImportAdapterFactory.class);
        config.setAdapterName("Test adapter");
        config.setSupportedFileExtensions(Arrays.asList("sd", "pdf"));
        config.setStagingAreaServiceClass(DefaultStagingAreaService.class);
        config.setEntityServiceClass(SubstanceStagingAreaEntityService.class);

        ObjectMapper mapper = new ObjectMapper();
        String configString =mapper.writeValueAsString(config);

        System.out.println("config: " + configString);
        Assertions.assertNotNull(config);

        ImportAdapterFactoryConfig deserConfig =mapper.readValue(configString, DefaultImportAdapterFactoryConfig.class);
        Assertions.assertNotNull(deserConfig);
    }

    @Test
    public void testDeserialize() throws JsonProcessingException {
        String serializedConfig="{  \"adapterName\": \"NSRS SDF Adapter\", \"importAdapterFactoryClass\": \"gsrs.module.substance.importers.SDFImportAdapterFactory\",  \"extensions\": [ \"sdf\", \"sd\" ],  \"parameters\": {   \"fileImportActions\": [   { \"actionClass\": \"gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory\", \"fields\": [ { \"fieldName\": \"code\", \"fieldLabel\": \"CAS Number\", \"defaultValue\": null, \"fieldType\": \"java.lang.String\", \"expectedToChange\": true, \"required\": true, \"lookupKey\": null }, { \"fieldName\": \"codeType\", \"fieldLabel\": \"Primary or Alternative\", \"defaultValue\": \"PRIMARY\", \"fieldType\": \"java.lang.String\", \"expectedToChange\": true, \"required\": false, \"lookupKey\": null } ], \"parameters\": { \"codeSystem\": \"CAS\" }, \"actionName\": \"cas_import\" }, { \"actionClass\": \"gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory\", \"fields\": [ { \"fieldName\": \"code\", \"fieldLabel\": \"NCI Number\", \"defaultValue\": null, \"fieldType\": \"java.lang.String\", \"expectedToChange\": true, \"required\": true, \"lookupKey\": null }, { \"fieldName\": \"codeType\", \"fieldLabel\": \"Primary or Alternative\", \"defaultValue\": \"PRIMARY\", \"fieldType\": \"java.lang.String\", \"expectedToChange\": true, \"required\": false, \"lookupKey\": null } ], \"parameters\": { \"codeSystem\": \"NCI\" }, \"actionName\": \"nci_import\" }  ] } } ";
        ObjectMapper mapper = new ObjectMapper();
        DefaultImportAdapterFactoryConfig config =mapper.readValue(serializedConfig, DefaultImportAdapterFactoryConfig.class);
        Assertions.assertEquals("NSRS SDF Adapter", config.getAdapterName());
    }
    @Test
    public void deserializeConfig1() throws UnsupportedEncodingException {
        String rawConfig="%5B%7B%22adapterName%22%3A%22NSRS+SDF+Adapter%22%2C%22importAdapterFactoryClass%22%3A+%22gsrs.module.substance.importers.SDFImportAdapterFactory%22%2C+%22extensions%22%3A+%5B+%22sdf%22%2C+%22sd%22%2C+%22sdfile%22+%5D%2C+%22parameters%22%3A+%7B+%22fileImportActions%22%3A+%5B%7B+%22actionClass%22%3A+%22gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory%22%2C+%22fields%22%3A+%5B+%7B+%22fieldName%22%3A+%22code%22%2C+%22fieldLabel%22%3A+%22CAS+Number%22%2C+%22defaultValue%22%3A+null%2C+%22fieldType%22%3A+%22java.lang.String%22%2C+%22expectedToChange%22%3A+true%2C+%22required%22%3A+true%2C+%22lookupKey%22%3A+null+%7D%2C+%7B+%22fieldName%22%3A+%22codeType%22%2C+%22fieldLabel%22%3A+%22Primary+or+Alternative%22%2C+%22defaultValue%22%3A+%22PRIMARY%22%2C+%22fieldType%22%3A+%22java.lang.String%22%2C+%22expectedToChange%22%3A+true%2C+%22required%22%3A+false%2C+%22lookupKey%22%3A+null+%7D+%5D%2C+%22parameters%22%3A+%7B+%22codeSystem%22%3A+%22CAS%22+%7D%2C+%22actionName%22%3A+%22cas_import%22+%7D%2C+%7B+%22actionClass%22%3A+%22gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory%22%2C+%22fields%22%3A+%5B+%7B+%22fieldName%22%3A+%22code%22%2C+%22fieldLabel%22%3A+%22NCI+Number%22%2C+%22defaultValue%22%3A+null%2C+%22fieldType%22%3A+%22java.lang.String%22%2C+%22expectedToChange%22%3A+true%2C+%22required%22%3A+true%2C+%22lookupKey%22%3A+null+%7D%2C+%7B+%22fieldName%22%3A+%22codeType%22%2C+%22fieldLabel%22%3A+%22Primary+or+Alternative%22%2C+%22defaultValue%22%3A+%22PRIMARY%22%2C+%22fieldType%22%3A+%22java.lang.String%22%2C+%22expectedToChange%22%3A+true%2C+%22required%22%3A+false%2C+%22lookupKey%22%3A+null+%7D+%5D%2C+%22parameters%22%3A+%7B+%22codeSystem%22%3A+%22NCI%22+%7D%2C+%22actionName%22%3A+%22nci_import%22%7D%5D%7D%7D%5D";
        String config= URLDecoder.decode(rawConfig, Charset.defaultCharset().name());
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<DefaultImportAdapterFactoryConfig> configObjects = mapper.readValue(config, mapper.getTypeFactory().constructCollectionType(ArrayList.class, DefaultImportAdapterFactoryConfig.class));
            Assertions.assertTrue(configObjects.size()>0);
        } catch (Exception ex) {
            System.err.println("Error deserializing: " + ex.getMessage());
            ex.printStackTrace();

            String config2 = config.substring(1, config.length()-1);
            //config2="{\"adapterName\":\"NSRS SDF Adapter\",\"importAdapterFactoryClass\":\"gsrs.module.substance.importers.SDFImportAdapterFactory\",\"extensions\":[\"sdf\", \"sd\", \"sdfile\" ]}";
            try {
                DefaultImportAdapterFactoryConfig extractedConfig = mapper.readValue(config2, DefaultImportAdapterFactoryConfig.class);
                Assertions.assertNotNull(extractedConfig);
            }catch (Exception ex2) {
                System.err.println("error2: " + ex2.getMessage());
                ex2.printStackTrace();
            }

        }

    }

    private Object buildConfigParameters(){
        Map<String, Object> parameters = new HashMap<>();
        List< Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action1 = new HashMap<>();
        action1.put("actionName", "cas_import");
        action1.put("actionClass", "gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory");
        Map<String, Object> parms = new HashMap<>();
        parms.put("codeSystem", "CAS");
        action1.put("parameters", parms);

        List<CodeProcessorField> fields = new ArrayList<>();
        CodeProcessorField field1 = new CodeProcessorFieldImpl();
        field1.setFieldName( "code");
        field1.setFieldLabel("CAS Number");
        field1.setFieldType(String.class);
        field1.setRequired(true);
        field1.setExpectedToChange(true);
        fields.add(field1);
        CodeProcessorField field2 = new CodeProcessorFieldImpl();
        field2.setFieldName( "codeType");
        field2.setFieldLabel("Primary or Alternative");
        field2.setFieldType(String.class);
        field2.setRequired(false);
        field2.setDefaultValue("PRIMARY");
        field2.setExpectedToChange(true);
        fields.add(field2);
        action1.put("fields", fields);

        actions.add(action1);
        parameters.put("fileImportActions", actions);
        return parameters;
    }

    private List<ActionConfig> buildTypedConfig(){

        List<ActionConfig> configList = new ArrayList<>();
        //List< Map<String, Object>> actions = new ArrayList<>();
        ActionConfig config = new ActionConfigImpl();
        config.setActionClass(NSRSCustomCodeExtractorActionFactory.class);
        config.setActionName("code_import");
        config.setActionClass(NSRSCustomCodeExtractorActionFactory.class);
        Map<String, Object> actionParameters= new HashMap<>();
        actionParameters.put("codeSystem", "CAS");
        config.setParameters(actionParameters);

        List<CodeProcessorField> fields = new ArrayList<>();
        CodeProcessorField field1 = new CodeProcessorFieldImpl();

        field1.setFieldName( "code");
        field1.setFieldLabel( "CAS Number");
        field1.setFieldType(String.class);
        field1.setRequired(true);
        field1.setRequired(true);
        fields.add(field1);
        CodeProcessorField field2 = new CodeProcessorFieldImpl();
        field2.setFieldName( "codeType");
        field2.setFieldLabel("Primary or Alternative");
        field2.setFieldType(String.class);
        field2.setRequired(false);
        field2.setDefaultValue("PRIMARY");
        field2.setExpectedToChange(true);
        fields.add(field2);

        configList.add((ActionConfigImpl) config);
        return configList;
    }

    private JsonNode buildDataNode(){
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        ArrayNode actions = JsonNodeFactory.instance.arrayNode();
        ObjectNode action1 = JsonNodeFactory.instance.objectNode();
        action1.put("actionName", "code_import");

        ObjectNode fieldValues = JsonNodeFactory.instance.objectNode();
        fieldValues.put("code", "50-00-0");
        fieldValues.put("codeType", "PRIMARY");
        action1.set("actionParameters", fieldValues);
        actions.add(action1);
        data.set("actions", actions);

        return data;
    }

}
