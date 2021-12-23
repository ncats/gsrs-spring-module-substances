package example.substance.validation;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.DEAValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class DEAValidatorTest extends AbstractSubstanceJpaEntityTest {

    private static boolean configured = false;

    @Autowired
    private TestGsrsValidatorFactory factory;

    String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
    private String DeaScheduleFileName = currentPath + "/src/main/resources/DEA_SCHED_LIST.txt";
    private String DeaNumberFileName= currentPath+ "/src/main/resources/DEA_LIST.txt";

    @BeforeEach
    public void setup() {
        if (!configured) {
            File f = new File(DeaNumberFileName);
            log.trace("DeaNumberFileName: " + DeaNumberFileName + "; exists: " + f.exists());
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(DEAValidator.class);
            config.setNewObjClass(Substance.class);
            Map<String, Object> parms = new HashMap<>();
            parms.put("deaScheduleFileName", DeaScheduleFileName);
            parms.put("deaNumberFileName", DeaNumberFileName);
            config.setParameters(parms);
            factory.addValidator("substances", config);
            configured = true;
            System.out.println("configured!");
        }
    }

    @Test
    public void testInit() throws IOException {
        DEAValidator validator = new DEAValidator();
        validator.setDeaNumberFileName(DeaNumberFileName);
        validator.setDeaScheduleFileName(DeaScheduleFileName);
        validator.initialize();
        Map<String, String> inchiKeyToDeaNumber= validator.getInchiKeyToDeaNumber();
        Assertions.assertTrue(inchiKeyToDeaNumber.size()>0);
    }

    @Test
    public void testSubstance1() throws Exception {
        //create a substance that's on the list
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical= builder.setStructureWithDefaultReference("COc1ccc(CC(C)N)cc1")
                .addName("d")
                .addCode("UNII", "OVB8F8P39Q")
                .build();
        ValidationResponse response = substanceEntityService.validateEntity(chemical.toFullJsonNode());
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        Assertions.assertTrue( s1.anyMatch(m->
                m.getMessageType()== ValidationMessage.MESSAGE_TYPE.WARNING && m.getMessage().contains("This substance has DEA schedule:")));
    }

    @Test
    public void testSubstance2() throws Exception {
        //create a substance that's on the list
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical= builder.setStructureWithDefaultReference("CCCCCN1N=C(C(=O)NC23CC4CC(CC(C4)C2)C3)C5=C1C=CC=C5")
                .addName("APINACA and AKB48 N-(1-Adamantyl)-1-pentyl-1Hindazole-3-carboxamide")
                .addCode("UNII", "MHR0400Y84")
                .build();
        ValidationResponse response = substanceEntityService.validateEntity(chemical.toFullJsonNode());
        Substance sub = (Substance) response.getNewObject();
        String expectedValue = "DEA-7048";
        Assertions.assertTrue( sub.codes.stream().anyMatch(c->c.codeSystem.equals(DEAValidator.DEA_NUMBER_CODE_SYSTEM) &&
                c.code.equals(expectedValue)));
    }

}
