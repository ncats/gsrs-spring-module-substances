package example.chem;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.StructureHandlingConfiguration;
import gsrs.module.substance.controllers.SubstanceController;
import gsrs.module.substance.utils.ChemicalUtils;
import ix.core.chem.InchiStandardizer;
import ix.core.chem.InchiStructureHasher;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.*;
import java.util.stream.Stream;

@Slf4j
class SaltRemovalTest {

    private static ChemicalUtils chemicalUtils;
    private static StructureProcessor structureProcessor;

    @BeforeAll
    static void setUpChemistryHelpers() {
        StructureHandlingConfiguration configuration = new StructureHandlingConfiguration();
        configuration.setSaltFilePath("salt_data_public.tsv");

        chemicalUtils = new ChemicalUtils();
        ReflectionTestUtils.setField(chemicalUtils, "structureHandlingConfiguration", configuration);
        ReflectionTestUtils.invokeMethod(chemicalUtils, "setUpSalts");

        structureProcessor = new StructureProcessor(new InchiStandardizer(), new InchiStructureHasher());
    }

    @Test
    void testRemoveSalts() throws IOException {

        String inputSmiles = "C[C@H]1[C@@]2([H])CC[C@@]3([H])[C@]4([H])CC=C5C[C@H](CC[C@]5(C)[C@@]4([H])CC[C@]23CN1C)N(C)C.Br.Br";
        Chemical chemical = Chemical.parse(inputSmiles);
        double massBefore = chemical.getMass();
        Chemical cleaned = chemicalUtils.stripSalts(chemical);
        double massAfter = cleaned.getMass();
        log.warn("formula before: {}; after: {}", chemical.getFormula(), cleaned.getFormula());
        Assertions.assertTrue(massAfter < massBefore, String.format("formula before: %s; after: %s", chemical.getFormula(), cleaned.getFormula()));
    }

    @Test
    void testRemoveSalts1() throws IOException {
        String inputSmiles = "c1ccc(cc1)N.[O-]P(=O)([O-])OP(=O)([O-])[O-]";
        String anilineSmiles= "c1ccc(cc1)N";
        Chemical chemical = Chemical.parse(inputSmiles);
        double massBefore = chemical.getMass();
        Chemical aniline = Chemical.parse(anilineSmiles);
        Chemical cleaned = chemicalUtils.stripSalts(chemical);
        double massAfter = cleaned.getMass();
        log.trace("formula before: {}; after: {}", chemical.getFormula(), cleaned.getFormula());
        Assertions.assertEquals(aniline.getMass(), massAfter);
    }
    @Test
    void testRemoveSaltsToNothing() throws Exception {

        String inputSmiles = "C([C@@H]([C@@H](O)C(=O)O)O)(=O)O";
        Structure structure = structureProcessor.taskFor(inputSmiles)
                .standardize(true)
                .build()
                .instrument()
                .getStructure();
        Structure saltStrippedStructure = controller().stripSalts(structure);
        Assertions.assertEquals("", saltStrippedStructure.formula);
    }

    @Test
    void testRemoveSaltsToNothing2Fragments() throws Exception {

        String inputSmiles = "C([C@@H]([C@@H](O)C(=O)O)O)(=O)[O-].[Na+]";
        Structure structure = structureProcessor.taskFor(inputSmiles)
                .standardize(true)
                .build()
                .instrument()
                .getStructure();
        Structure saltStrippedStructure = controller().stripSalts(structure);
        Assertions.assertEquals("", saltStrippedStructure.formula);
    }

    @ParameterizedTest
    @MethodSource("inputData")
    void testSaltRemoval(String smiles, int atomCountChange) throws IOException {
        Chemical chemical = Chemical.parse(smiles);
        Chemical cleaned = chemicalUtils.stripSalts(chemical);
        Assertions.assertEquals(atomCountChange, (chemical.getAtomCount() - cleaned.getAtomCount()),
                String.format("formula before: %s; after: %s", chemical.getFormula(), cleaned.getFormula()));
    }

    private static Stream<Arguments> inputData() {
        return Stream.of(
                Arguments.of("C[C@H]1[C@@]2([H])CC[C@@]3([H])[C@]4([H])CC=C5C[C@H](CC[C@]5(C)[C@@]4([H])CC[C@]23CN1C)N(C)C.Br.Br", 2),
                Arguments.of("CCC1C(Cc2cncn2C)COC1=O.Cl", 1),
                Arguments.of("c1cc(ccc1C(=C2C=CC(=O)C=C2)c3ccc(cc3)[O-])[O-].[Na+].[Na+]", 2),
                Arguments.of("[F-][Ti+4]([F-])([F-])([F-])([F-])[F-].[Li+].[Li+]", 2),
                Arguments.of("Cc1c(CCN2CCC(=C(c3ccc(cc3)F)c4ccc(cc4)F)CC2)c(=O)n5ccsc5n1.[C@@H]([C@H](C(=O)O)O)(C(=O)O)O", 10),
                Arguments.of("c1ccc(cc1)N.[O-]P(=O)([O-])[O-]", 5),
                Arguments.of("c1ccc(cc1)N.[O-]P(=O)([O-])OP(=O)([O-])[O-]", 9)
        );
    }

    private static SubstanceController controller() {
        SubstanceController controller = new SubstanceController();
        ReflectionTestUtils.setField(controller, "chemicalUtils", chemicalUtils);
        ReflectionTestUtils.setField(controller, "structureProcessor", structureProcessor);
        return controller;
    }
}
