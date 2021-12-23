package example.substance.utils;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.utils.SaltTable;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
public class SaltTableTest {

    String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
    private String saltsFileName = currentPath + "/src/main/resources/salts.txt";

    @Test
    public void testInit() {
        SaltTable saltTable = new SaltTable(saltsFileName);
        saltTable.initialize();
        Map<String, String> saltData = saltTable.getSaltInChiKeyToName();
        Assertions.assertFalse(saltData.isEmpty());
    }

    @Test
    public void testSalts() {
        String molfile= "\n" +
                "  ACCLDraw12222122502D\n" +
                "\n" +
                " 13 10  0  0  0  0  0  0  0  0999 V2000\n" +
                "    2.2500   -4.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.0852   -3.4148    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.2264   -3.7206    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    2.7794   -2.2736    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.1021   -9.9624    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.1251   -9.3718    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.1251   -8.1907    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.1479   -7.6001    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.1479   -6.4189    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.1708   -8.1907    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.1021  -11.1436    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    2.0791   -9.3718    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.3438   -2.8750    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  2  0  0  0  0\n" +
                "  2  4  1  0  0  0  0\n" +
                "  5  6  1  0  0  0  0\n" +
                "  6  7  2  0  0  0  0\n" +
                "  7  8  1  0  0  0  0\n" +
                "  8  9  2  0  0  0  0\n" +
                "  8 10  1  0  0  0  0\n" +
                "  5 11  2  0  0  0  0\n" +
                "  5 12  1  0  0  0  0\n" +
                "M  END\n";

        ChemicalSubstanceBuilder substanceBuilder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chem =
        substanceBuilder.setStructureWithDefaultReference(molfile)
                .addName("Misc")
                .build();
        SaltTable utilities = new SaltTable(saltsFileName);
        utilities.initialize();
        List<String> salts= utilities.getSalts(chem);
        salts.forEach(s-> System.out.println(s));
        Assertions.assertTrue(salts.size()>0);
    }

    @Test
    public void testSaltRemoval() throws IOException {
        String molfile = "\n" +
                "  ACCLDraw12222123042D\n" +
                "\n" +
                " 24 22  0  0  0  0  0  0  0  0999 V2000\n" +
                "    2.2500   -4.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.0852   -3.4148    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.2264   -3.7206    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    2.7794   -2.2736    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.1021   -9.9624    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.1251   -9.3718    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.1251   -8.1907    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.1479   -7.6001    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.1479   -6.4189    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.1708   -8.1907    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.1021  -11.1436    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    2.0791   -9.3718    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.3438   -2.8750    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.1250  -15.7188    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.8193  -16.8596    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.6547  -17.6950    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.7959  -17.3892    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.1017  -16.2480    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.2663  -15.4126    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.5721  -14.2714    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.7133  -13.9656    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.5487  -14.8011    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.2429  -15.9423    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0783  -16.7777    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  2  0  0  0  0\n" +
                "  2  4  1  0  0  0  0\n" +
                "  5  6  1  0  0  0  0\n" +
                "  6  7  2  0  0  0  0\n" +
                "  7  8  1  0  0  0  0\n" +
                "  8  9  2  0  0  0  0\n" +
                "  8 10  1  0  0  0  0\n" +
                "  5 11  2  0  0  0  0\n" +
                "  5 12  1  0  0  0  0\n" +
                " 14 15  2  0  0  0  0\n" +
                " 15 16  1  0  0  0  0\n" +
                " 16 17  2  0  0  0  0\n" +
                " 17 18  1  0  0  0  0\n" +
                " 18 19  1  0  0  0  0\n" +
                " 19 14  1  0  0  0  0\n" +
                " 19 20  2  0  0  0  0\n" +
                " 20 21  1  0  0  0  0\n" +
                " 21 22  2  0  0  0  0\n" +
                " 22 23  1  0  0  0  0\n" +
                " 23 18  2  0  0  0  0\n" +
                " 23 24  1  0  0  0  0\n" +
                "M  END\n";

        ChemicalSubstanceBuilder substanceBuilder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chem =
                substanceBuilder.setStructureWithDefaultReference(molfile)
                        .addName("Misc")
                        .build();
        SaltTable utilities = new SaltTable(saltsFileName);
        utilities.initialize();
        Chemical molWitchChemical= utilities.removeSalts(chem);
        log.debug("output mol: " +molWitchChemical.toMol());
        Assertions.assertEquals(11, molWitchChemical.getAtomCount());
    }

    @Test
    public void testSaltRemovalNeg() throws IOException {
        String molfile = "\n" +
                "  ACCLDraw12232113252D\n" +
                "\n" +
                " 13 14  0  0  0  0  0  0  0  0999 V2000\n" +
                "    9.7197   -7.2123    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.7015   -7.7948    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.6684   -7.7988    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.6834   -7.2109    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.6834   -6.0311    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.7015   -5.4353    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.7197   -6.0311    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.8432   -5.6662    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   11.5376   -6.6217    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.8432   -7.5775    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   11.2055   -8.6931    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   12.3529   -8.9370    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   12.7152  -10.0526    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  2  1  1  0  0  0  0\n" +
                "  4  2  2  0  0  0  0\n" +
                "  4  3  1  0  0  0  0\n" +
                "  5  4  1  0  0  0  0\n" +
                "  6  5  2  0  0  0  0\n" +
                "  7  6  1  0  0  0  0\n" +
                "  7  1  2  0  0  0  0\n" +
                "  8  7  1  0  0  0  0\n" +
                "  9  8  1  0  0  0  0\n" +
                " 10  9  2  0  0  0  0\n" +
                " 10  1  1  0  0  0  0\n" +
                " 11 10  1  0  0  0  0\n" +
                " 12 11  1  0  0  0  0\n" +
                " 13 12  1  0  0  0  0\n" +
                "M  END\n";

        ChemicalSubstanceBuilder substanceBuilder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chem =
                substanceBuilder.setStructureWithDefaultReference(molfile)
                        .addName("Misc")
                        .build();
        SaltTable utilities = new SaltTable(saltsFileName);
        utilities.initialize();
        int atomsBefore = chem.toChemical().getAtomCount();
        Chemical chemicalWithoutSalts= utilities.removeSalts(chem);
        log.debug("output mol: " +chemicalWithoutSalts.toMol());
        int atomAfter = chemicalWithoutSalts.getAtomCount();
        Assertions.assertEquals(atomAfter, atomsBefore);
    }


}
