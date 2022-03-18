package example.imports;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import gsrs.dataExchange.model.MappingActionFactory;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.module.substance.importers.model.SDRecordContext;
import gsrs.module.substance.utils.NCATSFileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.module.substance.importers.SDFImportAdaptorFactory;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@SpringBootConfiguration
@Slf4j
@TestPropertySource(properties = {
        "ix.gsrs.sdfActions={structure_and_moieties:'gsrs.module.substance.importers.importActionFactories.StructureExtractorActionFactory'," +
                "code_import:'gsrs.module.substance.importers.importActionFactories.CodeExtractorActionFactory'," +
                "common_name:'gsrs.module.substance.importers.importActionFactories.NameExtractorActionFactory'}",
})
public class SdFileTests {

    @Value("#{${ix.gsrs.sdfActions}}")
    Map<String, String> values;

    @Test
    public void testSdfInstructions1() {
        SDFImportAdaptorFactory importAdaptorFactory = new SDFImportAdaptorFactory();

        List<String> fieldNames = Arrays.asList("CAS", "select_name", "alpha code");
        Map<String, NCATSFileUtils.InputFieldStatistics> map = new HashMap<>();
        fieldNames.forEach(fn -> map.put(fn, null));
        JsonNode importInfo = importAdaptorFactory.createDefaultSdfFileImport(map);
        String json = importInfo.toPrettyString();
        System.out.println(json);
        Assertions.assertTrue(json.length() > 0);
        List<JsonNode> nodes = importInfo.findValues("actionName");
        Assertions.assertEquals(5, nodes.size());
        Assertions.assertEquals(1, nodes.stream().filter(n -> n.textValue().equals("common_name")).count());
    }

    @Test
    public void predictSettingsTest() throws IOException {
        String fileName = "testSDF/structures.molV2.sdf";
        File dataFile = new ClassPathResource(fileName).getFile();
        log.trace("using dataFile.getAbsoluteFile(): " + dataFile.getAbsoluteFile());

        InputStream fis = new FileInputStream(dataFile.getAbsoluteFile());
        SDFImportAdaptorFactory sDFImportAdaptorFactory = new SDFImportAdaptorFactory();
        AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics settings = sDFImportAdaptorFactory.predictSettings(fis);
        fis.close();

        JsonNode adapter = settings.getAdapterSettings();
        log.trace("adapter: ");
        log.trace(adapter.toPrettyString());
        JsonNode schema = settings.getAdapterSchema();
        log.trace("schema: ");
        log.trace(schema.toPrettyString());
        Assertions.assertNotNull(adapter);
        Assertions.assertNotNull(schema);
    }

    @Test
    public void createSubstanceStreamTest() throws IOException {
        String fileName = "testSDF/structures.molV2.sdf";
        File dataFile = new ClassPathResource(fileName).getFile();
        log.trace("using dataFile.getAbsoluteFile(): " + dataFile.getAbsoluteFile());

        InputStream fis = new FileInputStream(dataFile.getAbsoluteFile());
        SDFImportAdaptorFactory sDFImportAdaptorFactory = new SDFImportAdaptorFactory();
        AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics settings = sDFImportAdaptorFactory.predictSettings(fis);
        fis.close();
        JsonNode adapter = settings.getAdapterSettings();
        log.trace("adapter: ");
        log.trace(adapter.toPrettyString());
        AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> importAdapter = sDFImportAdaptorFactory.createAdapter(adapter);
        InputStream fisRead = new FileInputStream(dataFile.getAbsoluteFile());
        Stream<Substance> substanceStream = importAdapter.parse(fisRead);
        substanceStream.forEach(s -> {
            Assertions.assertTrue(s.substanceClass.toString().contains("chemical"));
            Assertions.assertEquals(1, s.names.size());
            log.trace("full substance: ");
            log.trace(s.toFullJsonNode().toPrettyString());
        });
        fisRead.close();
    }

    @Test
    public void createSubstanceStreamTestWithMultipleNames() throws IOException {
        SDFImportAdaptorFactory sDFImportAdaptorFactory = new SDFImportAdaptorFactory();
        Chemical c = Chemical.parse("CCCCCCCC");
        c.setName("MY_NAME");
        c.setProperty("NAME", "NAME_0\r\n\r\nNAME_1\r\nNAME_2\r\nIBUPROFEN\r\nNAME_3");
        ByteArrayInputStream bais = new ByteArrayInputStream(c.toSd().getBytes());
        AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics settings = sDFImportAdaptorFactory.predictSettings(bais);
        JsonNode adapter = settings.getAdapterSettings();
        log.trace("adapter: ");
        log.trace(adapter.toPrettyString());
        AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> importAdapter = sDFImportAdaptorFactory.createAdapter(adapter);
        bais = new ByteArrayInputStream(c.toSd().getBytes());
        Stream<Substance> substanceStream = importAdapter.parse(bais);
        substanceStream.forEach(s -> {
            log.trace("full substance: ");
            log.trace(s.toFullJsonNode().toPrettyString());
            Assertions.assertTrue(s.substanceClass.toString().contains("chemical"));
            Assertions.assertEquals(5, s.names.size());
        });
    }


    @Test
   public void parseUUIDTest() throws IOException {
        SDFImportAdaptorFactory sDFImportAdaptorFactory = new SDFImportAdaptorFactory();

        String uuidSyntax1 = "[[UUID_1]]";
        String uuidSyntax2 = "[[UUID_2]]";
        Chemical c = Chemical.parse("CCCCCCCC");

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        String res1_first = sDFImportAdaptorFactory.resolveParameter(ctx, uuidSyntax1);
        String res1_again = sDFImportAdaptorFactory.resolveParameter(ctx, uuidSyntax1);
        String res2_first = sDFImportAdaptorFactory.resolveParameter(ctx, uuidSyntax2);
        assertEquals(res1_first, res1_again);

        assertNotEquals(res1_first, res2_first);
        UUID uid1 = UUID.fromString(res1_first);
        UUID uid2 = UUID.fromString(res2_first);
        assertNotNull(uid1);
        assertNotNull(uid2);
    }

    @Test
    public void parsePropertyTest() throws IOException {
        SDFImportAdaptorFactory sDFImportAdaptorFactory = new SDFImportAdaptorFactory();

        String prop1 = "{{FOO}}";
        Chemical c = Chemical.parse("CCCCCCCC");
        c.setProperty("FOO", "BAR");

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        String res1_first = sDFImportAdaptorFactory.resolveParameter(ctx, prop1);
        assertEquals("BAR", res1_first);
    }

    @Test
    public void parseMolfileTest() throws IOException {
        SDFImportAdaptorFactory sDFImportAdaptorFactory = new SDFImportAdaptorFactory();

        String prop1 = "{{molfile}}";
        Chemical c = Chemical.parse("CCCCCCCC");

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        String res1_first = sDFImportAdaptorFactory.resolveParameter(ctx, prop1);
        assertTrue(res1_first.contains("V2000"));
    }

    @Test
    public void parseMolfileNameTest() throws IOException {
        SDFImportAdaptorFactory sDFImportAdaptorFactory = new SDFImportAdaptorFactory();

        String prop1 = "{{molfile_name}}";
        Chemical c = Chemical.parse("CCCCCCCC");
        c.setName("MY_NAME");

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        String res1_first = sDFImportAdaptorFactory.resolveParameter(ctx, prop1);
        assertEquals("MY_NAME", res1_first);
    }

    @Test
    public void parseMapTest() throws Exception {
        SDFImportAdaptorFactory sDFImportAdaptorFactory = new SDFImportAdaptorFactory();

        Chemical c = Chemical.parse("CCCCCCCC");
        c.setName("MY_NAME");
        c.setProperty("FOO", "BAR");
        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);

        Map<String, Object> settings = new HashMap<>();

        settings.put("molfile", "{{molfile}}");
        settings.put("molfile_name", "{{molfile_name}}");
        settings.put("foo_property", "{{FOO}}");
        settings.put("uuid1", "[[UUID_1]]");
        settings.put("uuid1again", "[[UUID_1]]");
        settings.put("uuid2", "[[UUID_2]]");

        Map<String, Object> settingsResolved = sDFImportAdaptorFactory.resolveParametersMap(ctx, settings);


        assertEquals("MY_NAME", settingsResolved.get("molfile_name"));
        assertEquals("BAR", settingsResolved.get("foo_property"));
        assertEquals(settingsResolved.get("uuid1"), settingsResolved.get("uuid1again"));
        assertNotEquals(settingsResolved.get("uuid1"), settingsResolved.get("uuid2"));

        assertTrue(settingsResolved.get("molfile").toString().contains("V2000"));
        assertNotNull(UUID.fromString(settingsResolved.get("uuid1").toString()));
        assertNotNull(UUID.fromString(settingsResolved.get("uuid2").toString()));
    }

    @Test
    public void testActionsFromConfig() throws IOException {
        String fieldName = "registry";
        SDFImportAdaptorFactory sDFImportAdaptorFactory = new SDFImportAdaptorFactory();
        try {
            Field defValuesField = sDFImportAdaptorFactory.getClass().getDeclaredField("defaultImportActions");
            defValuesField.setAccessible(true);
            defValuesField.set(sDFImportAdaptorFactory, values);
            sDFImportAdaptorFactory.init();
            java.lang.reflect.Field registryField = sDFImportAdaptorFactory.getClass().getDeclaredField(fieldName);
            registryField.setAccessible(true);
            Map<String, MappingActionFactory<Substance, SDRecordContext>> reg =
                    (Map<String, MappingActionFactory<Substance, SDRecordContext>>)
                            registryField.get(sDFImportAdaptorFactory);

            Assertions.assertEquals(3, reg.size());
            Assertions.assertTrue(reg.containsKey("structure_and_moieties"));
            Assertions.assertTrue(reg.containsKey("common_name"));
            Assertions.assertTrue(reg.containsKey("code_import"));
            System.out.println("it works!");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Error accessing field: " + e.getMessage());
            e.printStackTrace();
            Assertions.fail("Error fails test");
        }
    }
}
