package example.imports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.GsrsFactoryConfiguration;
import gsrs.dataExchange.model.MappingAction;
import gsrs.imports.ConfigBasedGsrsImportAdapterFactoryFactory;
import gsrs.imports.ImportAdapterFactory;
import gsrs.module.substance.importers.SDFImportAdaptorFactory;
import gsrs.module.substance.importers.model.SDRecordContext;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

public class DefaultImportAdapterFactoryConfigTest extends AbstractSubstanceJpaEntityTest {

    @Test
    public void testSetup() throws IllegalAccessException, NoSuchFieldException {
        String substanceContext = "substance";
        GsrsFactoryConfiguration config = new GsrsFactoryConfiguration();
        Map<String, List<Map<String, Object>>> adapterConfig = new HashMap<>();
        Map<String, Object> oneAdapter = new HashMap<>();
        oneAdapter.put("importAdapterFactoryClass", "gsrs.module.substance.importers.SDFImportAdaptorFactory");
        oneAdapter.put("adapterName", "NSRS SDF Adapter");
        oneAdapter.put("extensions", Arrays.asList(new String[]{"sdf", "sd"}));
        oneAdapter.put("parameters", buildConfigParameters());
        List<Map<String, Object>> adapters = new ArrayList<>();
        adapters.add(oneAdapter);
        adapterConfig.put(substanceContext, adapters);
        config.setImportAdapterFactories(adapterConfig);
        ConfigBasedGsrsImportAdapterFactoryFactory factoryFactory = new ConfigBasedGsrsImportAdapterFactoryFactory();
        Field configField = factoryFactory.getClass().getDeclaredField("gsrsFactoryConfiguration"); //gsrs.imports.ConfigBasedGsrsImportAdapterFactoryFactory.
        configField.setAccessible(true);
        configField.set(factoryFactory, config);
        //System.out.println("set field value");

        List<ImportAdapterFactory<Substance>> adapterFactories = factoryFactory.newFactory(substanceContext,
                Substance.class);
        Assertions.assertEquals(1, adapterFactories.size());
    }

    @Test
    public void testSetupActions() throws Exception {
        JsonNode configNode= buildConfigNode();
        SDFImportAdaptorFactory factory = new SDFImportAdaptorFactory();
        factory.initialize();
        List<MappingAction<Substance, SDRecordContext>> mappingActions= SDFImportAdaptorFactory.getMappingActions(configNode);
        Assertions.assertTrue(mappingActions.size()>0);
        System.out.println("class: " + mappingActions.get(0).getClass().getName());
    }

    private Object buildConfigParameters(){
        Map<String, Object> parameters = new HashMap<>();
        List< Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action1 = new HashMap<>();
        action1.put("actionName", "cas_import");
        action1.put("importActionFactoryClass", "gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory");
        action1.put("codeSystem", "CAS");

        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> field1 = new HashMap<>();
        field1.put("fieldName", "code");
        field1.put("fieldLabel", "CAS Number");
        field1.put("fieldType", "java.lang.String");
        field1.put("required", true);
        field1.put("expectedToChange", true);
        fields.add(field1);
        Map<String, Object> field2 = new HashMap<>();
        field2.put("fieldName", "codeType");
        field2.put("fieldLabel", "Primary or Alternative");
        field2.put("fieldType", "java.lang.String");
        field2.put("required", false);
        field2.put("defaultValue", "PRIMARY");
        field2.put("expectedToChange", true);
        fields.add(field2);
        action1.put("fields", fields);

        actions.add(action1);
        parameters.put("fileImportActions", actions);
        return parameters;
    }

    private JsonNode buildConfigNode(){
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        //List< Map<String, Object>> actions = new ArrayList<>();
        ArrayNode actions = JsonNodeFactory.instance.arrayNode();
        ObjectNode action1 = JsonNodeFactory.instance.objectNode();
        action1.put("actionName", "code_import");
        action1.put("importActionFactoryClass", "gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory");
        action1.put("codeSystem", "CAS");

        ArrayNode fields = JsonNodeFactory.instance.arrayNode();
        ObjectNode field1 = JsonNodeFactory.instance.objectNode();
        field1.put("fieldName", "code");
        field1.put("fieldLabel", "CAS Number");
        field1.put("fieldType", "java.lang.String");
        field1.put("required", true);
        field1.put("expectedToChange", true);
        fields.add(field1);
        ObjectNode field2 = JsonNodeFactory.instance.objectNode();
        field2.put("fieldName", "codeType");
        field2.put("fieldLabel", "Primary or Alternative");
        field2.put("fieldType", "java.lang.String");
        field2.put("required", false);
        field2.put("defaultValue", "PRIMARY");
        field2.put("expectedToChange", true);
        fields.add(field2);
        action1.set("fields", fields);

        actions.add(action1);
        parameters.set("actions", actions);
        return parameters;
    }

}
