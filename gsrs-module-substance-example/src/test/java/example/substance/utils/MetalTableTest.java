package example.substance.utils;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.utils.MetalTable;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetalTableTest {

    private final int SODIUM_ATOMIC_NUMBER =11;

    @Test
    public void testRemoveMetalsPos() {
        String molfile = "\n" +
                "  ACCLDraw12232114472D\n" +
                "\n" +
                " 17 18  0  0  0  0  0  0  0  0999 V2000\n" +
                "   12.0590  -12.0838    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   11.6967  -10.9682    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.5492  -10.7244    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.1869   -9.6087    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.0634   -9.2436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.0634   -8.0624    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.1869   -7.6975    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   10.8813   -8.6530    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0453   -7.4665    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.0271   -8.0624    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.0271   -9.2422    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.0121   -9.8301    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0453   -9.8261    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   13.2143  -12.3296    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   13.5792  -13.4532    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   14.0497  -11.4941    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   15.1905  -11.7998    0.0000 Na  0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  1  0  0  0  0\n" +
                "  3  4  1  0  0  0  0\n" +
                "  4  5  1  0  0  0  0\n" +
                "  6  5  2  0  0  0  0\n" +
                "  7  6  1  0  0  0  0\n" +
                "  8  7  1  0  0  0  0\n" +
                "  4  8  2  0  0  0  0\n" +
                "  6  9  1  0  0  0  0\n" +
                "  9 10  2  0  0  0  0\n" +
                " 10 11  1  0  0  0  0\n" +
                " 11 12  1  0  0  0  0\n" +
                " 11 13  2  0  0  0  0\n" +
                " 13  5  1  0  0  0  0\n" +
                "  1 14  1  0  0  0  0\n" +
                " 14 15  2  0  0  0  0\n" +
                " 14 16  1  0  0  0  0\n" +
                " 16 17  1  0  0  0  0\n" +
                "M  END\n";
        ChemicalSubstanceBuilder substanceBuilder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemicalSubstance =
                substanceBuilder.setStructureWithDefaultReference(molfile)
                        .addName("Misc")
                        .build();
        Chemical chemical =chemicalSubstance.toChemical();
        int bondCountBefore = chemical.getBondCount();
        int atomsCountBefore = chemical.getAtomCount();
        boolean actual = MetalTable.stripMetals(chemical, true);
        int bondCountAfter = chemical.getBondCount();
        int atomCountAfter = chemical.getAtomCount();
        Assertions.assertTrue(actual);
        Assertions.assertEquals(bondCountAfter+1, bondCountBefore);
        Assertions.assertEquals(atomCountAfter+1, atomsCountBefore);
        long totalSodium = chemical.atoms().filter(a->a.getAtomicNumber()==SODIUM_ATOMIC_NUMBER).count();
        Assertions.assertEquals(0, totalSodium);
    }

    @Test
    public void testRemoveMetalsPosNoDel() {
        String molfile = "\n" +
                "  ACCLDraw12232114472D\n" +
                "\n" +
                " 17 18  0  0  0  0  0  0  0  0999 V2000\n" +
                "   12.0590  -12.0838    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   11.6967  -10.9682    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.5492  -10.7244    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.1869   -9.6087    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.0634   -9.2436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.0634   -8.0624    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   10.1869   -7.6975    0.0000 N   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "   10.8813   -8.6530    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0453   -7.4665    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.0271   -8.0624    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.0271   -9.2422    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.0121   -9.8301    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0453   -9.8261    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   13.2143  -12.3296    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   13.5792  -13.4532    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   14.0497  -11.4941    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   15.1905  -11.7998    0.0000 Na  0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  1  0  0  0  0\n" +
                "  3  4  1  0  0  0  0\n" +
                "  4  5  1  0  0  0  0\n" +
                "  6  5  2  0  0  0  0\n" +
                "  7  6  1  0  0  0  0\n" +
                "  8  7  1  0  0  0  0\n" +
                "  4  8  2  0  0  0  0\n" +
                "  6  9  1  0  0  0  0\n" +
                "  9 10  2  0  0  0  0\n" +
                " 10 11  1  0  0  0  0\n" +
                " 11 12  1  0  0  0  0\n" +
                " 11 13  2  0  0  0  0\n" +
                " 13  5  1  0  0  0  0\n" +
                "  1 14  1  0  0  0  0\n" +
                " 14 15  2  0  0  0  0\n" +
                " 14 16  1  0  0  0  0\n" +
                " 16 17  1  0  0  0  0\n" +
                "M  END\n";
        ChemicalSubstanceBuilder substanceBuilder = new ChemicalSubstanceBuilder();
        ChemicalSubstance chemicalSubstance =
                substanceBuilder.setStructureWithDefaultReference(molfile)
                        .addName("Misc")
                        .build();
        Chemical chemical =chemicalSubstance.toChemical();
        int bondCountBefore = chemical.getBondCount();
        int atomsCountBefore = chemical.getAtomCount();
        boolean actual = MetalTable.stripMetals(chemical, false);
        int bondCountAfter = chemical.getBondCount();
        int atomCountAfter = chemical.getAtomCount();
        Assertions.assertTrue(actual);
        Assertions.assertEquals(bondCountAfter+1, bondCountBefore);
        Assertions.assertEquals(atomCountAfter, atomsCountBefore);
        long totalSodium = chemical.atoms().filter(a->a.getAtomicNumber()==SODIUM_ATOMIC_NUMBER).count();
        Assertions.assertEquals(1, totalSodium);
    }

    @Test
    public void testRemoveMetalsNeg() {
        //molecule with no metal atoms
        String molfile = "\n" +
                "  ACCLDraw12232113542D\n" +
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
        ChemicalSubstance chemicalSubstance =
                substanceBuilder.setStructureWithDefaultReference(molfile)
                        .addName("Misc")
                        .build();
        Chemical chemical =chemicalSubstance.toChemical();
        int atomCountBefore = chemical.getAtomCount();
        boolean actual = MetalTable.stripMetals(chemical, true);
        int atomCountAfter = chemical.getAtomCount();
        Assertions.assertEquals(atomCountAfter, atomCountBefore);
        Assertions.assertFalse(actual);
    }

}
