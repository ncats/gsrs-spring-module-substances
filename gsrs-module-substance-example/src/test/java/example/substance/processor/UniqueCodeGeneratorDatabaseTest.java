package example.substance.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.service.GsrsEntityService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestEntityProcessorFactory;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.CodeSequentialGenerator;
import ix.ginas.utils.LegacyCodeSequentialGenerator;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
public class UniqueCodeGeneratorDatabaseTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    @Autowired
    private DefinitionalElementFactory definitionalElementFactory;

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    StructureProcessor structureProcessor;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    private TestEntityProcessorFactory entityProcessorFactory;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void clearIndexers() throws IOException {
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
        }
    }

    @Test
    public void testConstructor1() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "whatever");
        m.put("length", -50);
        m.put("suffix", null);
        m.put("padding", true);
        m.put("max", 10L);
        m.put("codeSystem", "MYCS");

        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(
        (String) m.get("name"),
        (int) m.get("length"),
        (String) m.get("suffix"),
        (boolean) m.get("padding"),
        (Long) m.get("max"),
        (String) m.get("codeSystem")
        );
        assertEquals(2, codeGenerator.getLen());
    }

    @Test
    public void testConstructor2() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "whatever");
        m.put("length", 2);
        m.put("suffix", "XYZ");
        m.put("padding", true);
        m.put("max", 10L);
        m.put("codeSystem", "MYCS");
        String message = "";
        try {
            CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(
            (String) m.get("name"),
            (int) m.get("length"),
            (String) m.get("suffix"),
            (boolean) m.get("padding"),
            (Long) m.get("max"),
            (String) m.get("codeSystem")
            );
        } catch (Exception e)  {
            message = e.getMessage();
        }
        assertTrue(message.contains("The len value should be greater than or equal"));
    }

    @Test
    public void testCheckNextNumberWithinRange() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "whatever");
        m.put("length", 9);
        m.put("suffix", "OO");
        m.put("padding", true);
        m.put("max", 4L);
        m.put("codeSystem", "MYCS");

        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(
        (String) m.get("name"),
        (int) m.get("length"),
        (String) m.get("suffix"),
        (boolean) m.get("padding"),
        (Long) m.get("max"),
        (String) m.get("codeSystem")
        );
        assertTrue(codeGenerator.checkNextNumberWithinRange(3L, (Long) m.get("max")));
        assertTrue(codeGenerator.checkNextNumberWithinRange(4L, (Long) m.get("max")));
        assertTrue(!codeGenerator.checkNextNumberWithinRange(5L, (Long) m.get("max")));
        assertTrue(!codeGenerator.checkNextNumberWithinRange(-1L, (Long) m.get("max")));
        String message1 = "";
        try {
            codeGenerator.checkNextNumberWithinRange(null, (Long) m.get("max"));
        } catch (Exception e)  {
            message1 = e.getMessage();
        }
        assertTrue(message1.contains("nextNumber can not be null"));

        String message2 = "";
        try {
            codeGenerator.checkNextNumberWithinRange(3L, null);
        } catch (Exception e)  {
            message2 = e.getMessage();
        }
        assertTrue(message2.contains("maxNumber can not be null"));
    }

    @Test
    public void testDistinguishDifferentSuffix() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        // if bdnum 0000001AA exists, we should still be able to create 0000001AB
        Map<String, Object> m = new HashMap<>();
        m.put("name", "bdnum");
        m.put("length", 9);
        m.put("suffix", "AB");
        m.put("padding", true);
        m.put("max", 9999999L);
        m.put("codeSystem", "BDNUM");

        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(
        (String) m.get("name"),
        (int)m.get("length"),
        (String)m.get("suffix"),
        (boolean)m.get("padding"),
        (Long)m.get("max"),
        (String)m.get("codeSystem")
        );

        UUID uuid1 = UUID.randomUUID();
        Code code1 = new Code();
        code1.codeSystem="BDNUM";
        code1.type="PRIMARY";
        code1.code="0000001AA";   // Creating a bdnum without generator; note AA suffix is different

        UUID uuid2 = UUID.randomUUID();
        SubstanceBuilder substanceBuilder1 = new SubstanceBuilder()
        .addName("TEST1 ABC", n->n.stdName="TEST1 ABC")
        .setUUID(uuid1)
        .addCode(code1);
        Substance built1 = substanceBuilder1.build();
        loadSubstanceFromJsonString(EntityUtils.EntityWrapper.of(built1).toFullJson());

        // Only use code generator on substance 2
        AutowireHelper.getInstance().autowire(codeGenerator);

        SubstanceBuilder substanceBuilder2 = new SubstanceBuilder()
        .addName("TEST2 ABC", n->n.stdName="TEST2 ABC")
        .setUUID(uuid2);
        Substance built2 = substanceBuilder2.build();
        codeGenerator.addCode(built2);
        loadSubstanceFromJsonString(EntityUtils.EntityWrapper.of(built2).toFullJson());

        Optional<Substance> so1 = substanceEntityService.get(uuid1);
        Substance s1 = so1.get();
        assertTrue(s1.getCodes().stream().anyMatch(c -> c.code.equals("0000001AA")));

        Optional<Substance> so2 = substanceEntityService.get(uuid2);
        Substance s2 = so2.get();
        assertTrue(s2.getCodes().stream().anyMatch(c -> c.code.equals("0000001AB")));
    }

    @Test
    public void testAddTwoSubstances() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "whatever");
        m.put("length", String.valueOf(9999L).length()+2);
        m.put("suffix", "XX");
        m.put("padding", true);
        m.put("max", 9999L);
        m.put("codeSystem", "MYCS");

        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(
            (String) m.get("name"),
            (int)m.get("length"),
            (String)m.get("suffix"),
            (boolean)m.get("padding"),
            (Long)m.get("max"),
            (String)m.get("codeSystem")
        );
        AutowireHelper.getInstance().autowire(codeGenerator);
        UUID uuid1 = UUID.randomUUID();
        Code code1 = new Code();
        code1.codeSystem="CodeSystem1";
        code1.type="PRIMARY";
        code1.code="CODE1";

        UUID uuid2 = UUID.randomUUID();
        Code code2 = new Code();
        code2.codeSystem="CodeSystem2";
        code2.code="CODE2";
        code2.type="PRIMARY";

        SubstanceBuilder substanceBuilder1 = new SubstanceBuilder()
        .addName("TEST1 ABC", n->n.stdName="TEST1 ABC")
        .setUUID(uuid1)
        .addCode(code1);
        Substance built1 = substanceBuilder1.build();
        codeGenerator.addCode(built1);
        loadSubstanceFromJsonString(EntityUtils.EntityWrapper.of(built1).toFullJson());

        SubstanceBuilder substanceBuilder2 = new SubstanceBuilder()
        .addName("TEST2 ABC", n->n.stdName="TEST2 ABC")
        .setUUID(uuid2)
        .addCode(code2);
        Substance built2 = substanceBuilder2.build();
        codeGenerator.addCode(built2);
        loadSubstanceFromJsonString(EntityUtils.EntityWrapper.of(built2).toFullJson());

        Optional<Substance> so1 = substanceEntityService.get(uuid1);
        Substance s1 = so1.get();
        assertTrue(s1.getCodes().stream().anyMatch(c -> c.code.equals("0001XX")));

        Optional<Substance> so2 = substanceEntityService.get(uuid2);
        Substance s2 = so2.get();
        assertTrue(s2.getCodes().stream().anyMatch(c -> c.code.equals("0002XX")));
    }

    @Test
    public void testAddThreeSubstancesMaxTooSmall() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "whatever");
        m.put("length", String.valueOf(2L).length()+2);
        m.put("suffix", "XX");
        m.put("padding", true);
        m.put("max", 2L); // Setting this to a small value so that on 3rd substance we get an exception.
        m.put("codeSystem", "MYCS");

        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(
            (String) m.get("name"),
            (int) m.get("length"),
            (String) m.get("suffix"),
            (boolean) m.get("padding"),
            (Long) m.get("max"),
            (String) m.get("codeSystem")
        );
        AutowireHelper.getInstance().autowire(codeGenerator);

        UUID uuid1 = UUID.randomUUID();
        Code code1 = new Code();
        code1.codeSystem = "CodeSystem1";
        code1.type = "PRIMARY";
        code1.code = "CODE1";
        SubstanceBuilder substanceBuilder1 = new SubstanceBuilder()
        .addName("TEST1 ABC", n -> n.stdName = "TEST1 ABC")
        .setUUID(uuid1)
        .addCode(code1);
        Substance built1 = substanceBuilder1.build();
        codeGenerator.addCode(built1);
        loadSubstanceFromJsonString(EntityUtils.EntityWrapper.of(built1).toFullJson());

        UUID uuid2 = UUID.randomUUID();
        Code code2 = new Code();
        code2.codeSystem = "CodeSystem2";
        code2.code = "CODE2";
        code2.type = "PRIMARY";
        SubstanceBuilder substanceBuilder2 = new SubstanceBuilder()
        .addName("TEST2 ABC", n -> n.stdName = "TEST2 ABC")
        .setUUID(uuid2)
        .addCode(code2);
        Substance built2 = substanceBuilder2.build();
        codeGenerator.addCode(built2);
        loadSubstanceFromJsonString(EntityUtils.EntityWrapper.of(built2).toFullJson());

        UUID uuid3 = UUID.randomUUID();
        Code code3 = new Code();
        code2.codeSystem = "CodeSystem3";
        code2.code = "CODE3";
        code2.type = "PRIMARY";
        SubstanceBuilder substanceBuilder3 = new SubstanceBuilder()
        .addName("TEST3 ABC", n -> n.stdName = "TEST3 ABC")
        .setUUID(uuid3)
        .addCode(code3);
        Substance built3 = substanceBuilder3.build();
        String message = "";
        try {
            codeGenerator.addCode(built3);
        } catch (Exception e)  {
            message = e.getMessage();
        }
        assertTrue(message.contains("The value for nextNumber is out of range."));
    }

    @Test
    public void testUseLegacy() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "BDNUM NAME");
        m.put("length", 9);
        m.put("suffix", "AB");
        m.put("padding", true);
        m.put("codeSystem", "BDNUM");

        LegacyCodeSequentialGenerator codeGenerator = new LegacyCodeSequentialGenerator(
        (String) m.get("name"),
        (int) m.get("length"),
        (String) m.get("suffix"),
        (boolean) m.get("padding"),
        (String) m.get("codeSystem")
        );
        AutowireHelper.getInstance().autowire(codeGenerator);

        UUID uuid1 = UUID.randomUUID();
        Code code1 = new Code();
        code1.codeSystem = "CodeSystem1";
        code1.type = "PRIMARY";
        code1.code = "CODE1";
        SubstanceBuilder substanceBuilder1 = new SubstanceBuilder()
        .addName("TEST1 ABC", n -> n.stdName = "TEST1 ABC")
        .setUUID(uuid1)
        .addCode(code1);
        Substance built1 = substanceBuilder1.build();
        codeGenerator.addCode(built1);
        loadSubstanceFromJsonString(EntityUtils.EntityWrapper.of(built1).toFullJson());

        UUID uuid2 = UUID.randomUUID();
        Code code2 = new Code();
        code2.codeSystem = "CodeSystem2";
        code2.code = "CODE2";
        code2.type = "PRIMARY";
        SubstanceBuilder substanceBuilder2 = new SubstanceBuilder()
        .addName("TEST2 ABC", n -> n.stdName = "TEST2 ABC")
        .setUUID(uuid2)
        .addCode(code2);
        Substance built2 = substanceBuilder2.build();
        codeGenerator.addCode(built2);
        loadSubstanceFromJsonString(EntityUtils.EntityWrapper.of(built2).toFullJson());

        UUID uuid3 = UUID.randomUUID();
        Code code3 = new Code();
        code2.codeSystem = "CodeSystem3";
        code2.code = "CODE3";
        code2.type = "PRIMARY";
        SubstanceBuilder substanceBuilder3 = new SubstanceBuilder()
        .addName("TEST3 ABC", n -> n.stdName = "TEST3 ABC")
        .setUUID(uuid3)
        .addCode(code3);
        Substance built3 = substanceBuilder3.build();
        String message = "";
        assertEquals("0000003AB", codeGenerator.getCode().code);
    }

    @Test
    public void testDontUseLegacy() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "BDNUM NAME");
        m.put("length", 9);
        m.put("suffix", "AB");
        m.put("padding", true);
        m.put("codeSystem", "BDNUM");
        m.put("max", 9999999L);

        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(
        (String) m.get("name"),
        (int) m.get("length"),
        (String) m.get("suffix"),
        (boolean) m.get("padding"),
        (Long) m.get("max"),
        (String) m.get("codeSystem")
        );
        AutowireHelper.getInstance().autowire(codeGenerator);

        UUID uuid1 = UUID.randomUUID();
        Code code1 = new Code();
        code1.codeSystem = "CodeSystem1";
        code1.type = "PRIMARY";
        code1.code = "CODE1";
        SubstanceBuilder substanceBuilder1 = new SubstanceBuilder()
        .addName("TEST1 ABC", n -> n.stdName = "TEST1 ABC")
        .setUUID(uuid1)
        .addCode(code1);
        Substance built1 = substanceBuilder1.build();
        codeGenerator.addCode(built1);
        loadSubstanceFromJsonString(EntityUtils.EntityWrapper.of(built1).toFullJson());

        UUID uuid2 = UUID.randomUUID();
        Code code2 = new Code();
        code2.codeSystem = "CodeSystem2";
        code2.code = "CODE2";
        code2.type = "PRIMARY";
        SubstanceBuilder substanceBuilder2 = new SubstanceBuilder()
        .addName("TEST2 ABC", n -> n.stdName = "TEST2 ABC")
        .setUUID(uuid2)
        .addCode(code2);
        Substance built2 = substanceBuilder2.build();
        codeGenerator.addCode(built2);
        loadSubstanceFromJsonString(EntityUtils.EntityWrapper.of(built2).toFullJson());

        UUID uuid3 = UUID.randomUUID();
        Code code3 = new Code();
        code2.codeSystem = "CodeSystem3";
        code2.code = "CODE3";
        code2.type = "PRIMARY";
        SubstanceBuilder substanceBuilder3 = new SubstanceBuilder()
        .addName("TEST3 ABC", n -> n.stdName = "TEST3 ABC")
        .setUUID(uuid3)
        .addCode(code3);
        Substance built3 = substanceBuilder3.build();
        String message = "";
        assertEquals("0000003AB", codeGenerator.getCode().code);
    }

    public Substance loadSubstanceFromJsonString(String jsonText) {
        Substance substance = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = null;
        try {
            json = mapper.readTree(jsonText);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            GsrsEntityService.CreationResult<Substance> result= substanceEntityService.createEntity(json, false);
            substance = result.getCreatedEntity();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return substance;
    }

}
