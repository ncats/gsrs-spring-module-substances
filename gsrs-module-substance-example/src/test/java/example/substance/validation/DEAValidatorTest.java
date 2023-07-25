package example.substance.validation;

import gsrs.module.substance.utils.DEADataTable;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Note;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.DEAValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Disabled
public class DEAValidatorTest extends AbstractSubstanceJpaEntityTest {

    private static boolean configured = false;

    @Autowired
    private TestGsrsValidatorFactory factory;

    String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
    private String DeaNumberFileName = currentPath + "/src/main/resources/DEA_LIST.txt";

    @BeforeEach
    public void setup() {
        if (!configured) {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(DEAValidator.class);
            config.setNewObjClass(Substance.class);
            Map<String, Object> parms = new HashMap<>();
            parms.put("deaNumberFileName", DeaNumberFileName);
            config.setParameters(parms);
            factory.addValidator("substances", config);
            configured = true;
            System.out.println("configured!");
        }
    }

    @Test
    public void testInit() throws IOException {
        DEADataTable deaDataTable = new DEADataTable(DeaNumberFileName);
        Map<String, String> inchiKeyToDeaNumber= deaDataTable.getInchiKeyToDeaNumber();
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

    @Test
    public void testGetDeaScheduleForChemical() throws Exception {
        //create a substance that's on the list
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical= builder.setStructureWithDefaultReference("CCCCCN1N=C(C(=O)NC23CC4CC(CC(C4)C2)C3)C5=C1C=CC=C5")
                .addName("APINACA and AKB48 N-(1-Adamantyl)-1-pentyl-1Hindazole-3-carboxamide")
                .addCode("UNII", "MHR0400Y84")
                .build();

        DEADataTable deaDataTable = new DEADataTable(DeaNumberFileName);
        String expectedValue = "DEA SCHEDULE I";
        String actualValue =deaDataTable.getDeaScheduleForChemical(chemical);
        Assertions.assertEquals(actualValue, expectedValue);
    }

    @Test
    public void testGetDeaNumberForChemical() throws Exception {
        //create a substance that's on the list
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical= builder.setStructureWithDefaultReference("CCCCCN1N=C(C(=O)NC23CC4CC(CC(C4)C2)C3)C5=C1C=CC=C5")
                .addName("APINACA and AKB48 N-(1-Adamantyl)-1-pentyl-1Hindazole-3-carboxamide")
                .addCode("UNII", "MHR0400Y84")
                .build();
        DEADataTable deaDataTable = new DEADataTable(DeaNumberFileName);
        String expectedValue = "DEA-7048";
        String actualValue =deaDataTable.getDeaNumberForChemical(chemical);
        Assertions.assertEquals(actualValue, expectedValue);
    }

    @Test
    public void testGetDeaNumberForUnlistedChemical() throws Exception {
        //create a substance that's on the list
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical= builder.setStructureWithDefaultReference("CCCCCN1N=C(C(=O)NC23CC4CC(CC(C4)C2)C3)C5=C1C=[Si]C=[As]5")
                .addName("Unknown chemical")
                .build();
        DEADataTable deaDataTable = new DEADataTable(DeaNumberFileName);
        String actualValue =deaDataTable.getDeaNumberForChemical(chemical);
        Assertions.assertEquals(actualValue, null);
    }


    @Test
    public void testGetDeaScheduleForUnlistedChemical() throws Exception {
        //create a substance that's on the list
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical= builder.setStructureWithDefaultReference("CCCCCN1N=C(C(=O)NC23CC4CC(CC(C4)C2)C3)C5=C1C=CC=C5CCCC[Si]")
                .addName("Unknown chemical")
                .build();
        DEADataTable deaDataTable = new DEADataTable(DeaNumberFileName);
        String actualValue =deaDataTable.getDeaScheduleForChemical(chemical);
        Assertions.assertNull(actualValue);
    }

    @Test
    public void testAssignNoteForDeaPos(){
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemical= builder.setStructureWithDefaultReference("CCCCCN1N=C(C(=O)NC23CC4CC(CC(C4)C2)C3)C5=C1C=CC=C5CCCC[Si]")
                .addName("Unknown chemical")
                .build();
        DEADataTable deaDataTable = new DEADataTable(DeaNumberFileName);
        String deaSchedule = deaDataTable.getDeaScheduleForChemical(chemical);
        boolean added = deaDataTable.assignNoteForDea(chemical, deaSchedule);
        Assertions.assertTrue(added);
    }

    @Test
    public void testAssignNoteForDeaNeg(){
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        Note deaScheduleNote = new Note();
        deaScheduleNote.note="WARNING:This substance has DEA schedule: DEA SCHEDULE I";
        ChemicalSubstance chemical= builder.setStructureWithDefaultReference("CCCCCN1N=C(C(=O)NC23CC4CC(CC(C4)C2)C3)C5=C1C=CC=C5")
                .addName("Unknown chemical")
                .addNote(deaScheduleNote)
                .build();
        DEADataTable deaDataTable = new DEADataTable(DeaNumberFileName);
        String deaSchedule = deaDataTable.getDeaScheduleForChemical(chemical);
        boolean added = deaDataTable.assignNoteForDea(chemical, deaSchedule);
        Assertions.assertFalse(added);
    }

}
