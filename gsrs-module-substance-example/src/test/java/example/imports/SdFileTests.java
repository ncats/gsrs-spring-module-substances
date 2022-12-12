package example.imports;

import com.fasterxml.jackson.databind.JsonNode;
import example.GsrsModuleSubstanceApplication;
import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterFactory;
import gsrs.imports.ImportAdapterStatistics;
import gsrs.module.substance.controllers.SubstanceController;
import gsrs.module.substance.importers.SDFImportAdapterFactory;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import gsrs.module.substance.utils.NCATSFileUtils;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest
@Slf4j
/*@TestPropertySource(properties = {
        "ix.gsrs.sdfActions={structure_and_moieties:'gsrs.module.substance.importers.importActionFactories.StructureExtractorActionFactory'," +
                "code_import:'gsrs.module.substance.importers.importActionFactories.CodeExtractorActionFactory'," +
                "common_name:'gsrs.module.substance.importers.importActionFactories.NameExtractorActionFactory'}",
})*/
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class SdFileTests extends AbstractSubstanceJpaFullStackEntityTest {

    List<ImportAdapterFactory<AbstractSubstanceBuilder>> factories = Arrays.asList(new SDFImportAdapterFactory());
    private CachedSupplier<List<ImportAdapterFactory<AbstractSubstanceBuilder>>> importAdapterFactories
            = CachedSupplier.of(() -> factories);

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Test
    public void testSdfInstructions1() {
        SDFImportAdapterFactory importAdapterFactory = new SDFImportAdapterFactory();

        List<String> fieldNames = Arrays.asList("CAS", "select_name", "alpha code");
        Map<String, NCATSFileUtils.InputFieldStatistics> map = new HashMap<>();
        fieldNames.forEach(fn -> map.put(fn, null));
        JsonNode importInfo = importAdapterFactory.createDefaultSdfFileImport(map);
        String json = importInfo.toPrettyString();
        System.out.println(json);
        Assertions.assertTrue(json.length() > 0);
        List<JsonNode> nodes = importInfo.findValues("actionName");
        Assertions.assertEquals(5, nodes.size());
        Assertions.assertEquals(1, nodes.stream().filter(n -> n.textValue().startsWith("common_name")).count());
    }

    @Test
    public void predictSettingsTest() throws IOException {
        String fileName = "testSDF/structures.molV2.sdf";
        File dataFile = new ClassPathResource(fileName).getFile();
        log.trace("using dataFile.getAbsoluteFile(): " + dataFile.getAbsoluteFile());

        InputStream fis = new FileInputStream(dataFile.getAbsoluteFile());
        SDFImportAdapterFactory sDFImportAdapterFactory = new SDFImportAdapterFactory();
        ImportAdapterStatistics settings = sDFImportAdapterFactory.predictSettings(fis);
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
        SDFImportAdapterFactory sDFImportAdapterFactory = new SDFImportAdapterFactory();
        ImportAdapterStatistics settings = sDFImportAdapterFactory.predictSettings(fis);

        fis.close();
        JsonNode adapter = settings.getAdapterSettings();
        log.trace("adapter: ");
        log.trace(adapter.toPrettyString());
        ImportAdapter<AbstractSubstanceBuilder> importAdapter = sDFImportAdapterFactory.createAdapter(adapter);
        InputStream fisRead = new FileInputStream(dataFile.getAbsoluteFile());
        Stream<AbstractSubstanceBuilder> substanceStream = importAdapter.parse(fisRead, Charset.defaultCharset().name());
        substanceStream.forEach(s -> {
            //Assertions.assertTrue(s.build().substanceClass.toString().contains("chemical"));
            Assertions.assertTrue(s.build().names.size()>=1);
            Assertions.assertTrue( s.build().codes.size()>=1);

            log.trace("full substance: ");
            log.trace(s.build().toFullJsonNode().toPrettyString());
        });
        fisRead.close();
    }

    @Test
    public void createSubstanceStreamTestWithMultipleNames() throws IOException {
        SDFImportAdapterFactory sDFImportAdapterFactory = new SDFImportAdapterFactory();
        Chemical c = Chemical.parse("CCCCCCCC");
        c.setName("MY_NAME");
        c.setProperty("NAME", "NAME_0\r\n\r\nNAME_1\r\nNAME_2\r\nIBUPROFEN\r\nNAME_3");
        ByteArrayInputStream bais = new ByteArrayInputStream(c.toSd().getBytes());
        sDFImportAdapterFactory.initialize();
        ImportAdapterStatistics settings = sDFImportAdapterFactory.predictSettings(bais);
        JsonNode adapter = settings.getAdapterSettings();
        log.trace("adapter: ");
        log.trace(adapter.toPrettyString());
        ImportAdapter<AbstractSubstanceBuilder> importAdapter = sDFImportAdapterFactory.createAdapter(adapter);
        bais = new ByteArrayInputStream(c.toSd().getBytes());
        Stream<AbstractSubstanceBuilder> substanceStream = importAdapter.parse(bais, Charset.defaultCharset().name());
        substanceStream.forEach(s -> {
            log.trace("full substance: ");
            log.trace(s.build().toFullJsonNode().toPrettyString());
            Assertions.assertTrue(s.build().substanceClass.toString().contains("chemical"));
            Assertions.assertEquals(5, s.build().names.size());
        });
    }

    @Test
   public void parseUUIDTest() throws IOException {
        SDFImportAdapterFactory sDFImportAdapterFactory = new SDFImportAdapterFactory();

        String uuidSyntax1 = "[[UUID_1]]";
        String uuidSyntax2 = "[[UUID_2]]";
        Chemical c = Chemical.parse("CCCCCCCC");

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        String res1_first = sDFImportAdapterFactory.resolveParameter(ctx, uuidSyntax1);
        String res1_again = sDFImportAdapterFactory.resolveParameter(ctx, uuidSyntax1);
        String res2_first = sDFImportAdapterFactory.resolveParameter(ctx, uuidSyntax2);
        assertEquals(res1_first, res1_again);

        assertNotEquals(res1_first, res2_first);
        UUID uid1 = UUID.fromString(res1_first);
        UUID uid2 = UUID.fromString(res2_first);
        assertNotNull(uid1);
        assertNotNull(uid2);
    }

    @Test
    public void parsePropertyTest() throws IOException {
        SDFImportAdapterFactory sDFImportAdapterFactory = new SDFImportAdapterFactory();

        String prop1 = "{{FOO}}";
        Chemical c = Chemical.parse("CCCCCCCC");
        c.setProperty("FOO", "BAR");

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        String res1_first = sDFImportAdapterFactory.resolveParameter(ctx, prop1);
        assertEquals("BAR", res1_first);
    }

    @Test
    public void parseMolfileTest() throws IOException {
        SDFImportAdapterFactory sDFImportAdapterFactory = new SDFImportAdapterFactory();

        String prop1 = "{{molfile}}";
        Chemical c = Chemical.parse("CCCCCCCC");

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        String res1_first = sDFImportAdapterFactory.resolveParameter(ctx, prop1);
        assertTrue(res1_first.contains("V2000"));
    }

    @Test
    public void parseMolfileNameTest() throws IOException {
        SDFImportAdapterFactory sDFImportAdapterFactory = new SDFImportAdapterFactory();

        String prop1 = "{{molfile_name}}";
        Chemical c = Chemical.parse("CCCCCCCC");
        c.setName("MY_NAME");

        ChemicalBackedSDRecordContext ctx = new ChemicalBackedSDRecordContext(c);
        String res1_first = sDFImportAdapterFactory.resolveParameter(ctx, prop1);
        assertEquals("MY_NAME", res1_first);
    }

    @Test
    public void parseMapTest() throws Exception {
        SDFImportAdapterFactory sDFImportAdapterFactory = new SDFImportAdapterFactory();

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

        Map<String, Object> settingsResolved = sDFImportAdapterFactory.resolveParametersMap(ctx, settings);


        assertEquals("MY_NAME", settingsResolved.get("molfile_name"));
        assertEquals("BAR", settingsResolved.get("foo_property"));
        assertEquals(settingsResolved.get("uuid1"), settingsResolved.get("uuid1again"));
        assertNotEquals(settingsResolved.get("uuid1"), settingsResolved.get("uuid2"));

        assertTrue(settingsResolved.get("molfile").toString().contains("V2000"));
        assertNotNull(UUID.fromString(settingsResolved.get("uuid1").toString()));
        assertNotNull(UUID.fromString(settingsResolved.get("uuid2").toString()));
    }

    @Test
    protected void TestPreview() throws Exception {
        String fileName = "testSDF/chembl_30_first_36.sdf";
        File dataFile = new ClassPathResource(fileName).getFile();
        log.trace("using dataFile.getAbsoluteFile(): " + dataFile.getAbsoluteFile());

        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowire(controller);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("adapter", "NSRS SDF Adapter");
        queryParameters.put("entityType", "ix.ginas.models.v1.ChemicalSubstance");
        queryParameters.put("fileEncoding", "UTF-8");
        MultipartFile file = new MultipartFile() {
            @Override
            public String getName() {
                return dataFile.getName();
            }

            @Override
            public String getOriginalFilename() {
                return dataFile.getName();
            }

            @Override
            public String getContentType() {
                return "application/x-sdf";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return dataFile.length();
            }

            @Override
            public byte[] getBytes() throws IOException {
                return Files.readAllBytes(dataFile.toPath());
                //return new byte[0];
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return null;
            }

            @Override
            public void transferTo(File file) throws IOException, IllegalStateException {

            }
        };

        ResponseEntity<Object> responseEntity= controller.handleImport(file, queryParameters);
        Assertions.assertNotNull(responseEntity.getBody());
    }

    @Test
    public void testParseRange() {
        Pattern rangePattern = Pattern.compile("(\\d+\\.?\\d+)\\-(\\d+\\.?\\d+)(.+)");
        String input1 = "159.1-190.6 °C";
        String expectedLower ="159.1";
        String expectedUpper ="190.6";
        Matcher m = rangePattern.matcher(input1);
        String lower="";
        String upper="";
        String units ="";
        if(m.matches())  {
            lower= m.group(1);
            upper = m.group(2);
            units=m.group(3);
        }
        assertEquals(expectedLower, lower);
        assertEquals(expectedUpper, upper);
        System.out.println("units: " + units);
    }

    @Test
    public void testParseRange2() {
        Pattern rangePattern = Pattern.compile("(\\d+\\.?\\d+)\\±(\\d+\\.?\\d+)(.+)");
        String input1 =     "722.2±60.0 °C    Press: 760 Torr";
                    input1= "722.2±60.0 °C    Press: 760 Torr";
        String expectedBase ="722.2";
        String expectedVariation ="60.0";
        Matcher m = rangePattern.matcher(input1);
        String base="";
        String range="";
        String unitsAndConditions ="";
        if(m.matches())  {
            base= m.group(1);
            range = m.group(2);
            unitsAndConditions=m.group(3);
        }
        assertEquals(expectedBase, base);
        assertEquals(expectedVariation, range);
        System.out.println("unitsAndConditions: " + unitsAndConditions);
    }
/*    @Test
    public void testActionsFromConfig() throws IOException {
        String fieldName = "registry";
        SDFImportAdapterFactory SDFImportAdapterFactory = new SDFImportAdapterFactory();
        try {
            Field defValuesField = SDFImportAdapterFactory.getClass().getDeclaredField("defaultImportActions");
            defValuesField.setAccessible(true);
            defValuesField.set(SDFImportAdapterFactory, values);
            SDFImportAdapterFactory.initialize();
            java.lang.reflect.Field registryField = SDFImportAdapterFactory.getClass().getDeclaredField(fieldName);
            registryField.setAccessible(true);
            Map<String, MappingActionFactory<Substance, SDRecordContext>> reg =
                    (Map<String, MappingActionFactory<Substance, SDRecordContext>>)
                            registryField.get(SDFImportAdapterFactory);

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
    }*/

}
