package example.exports.comparators;

import gsrs.module.substance.comparators.ExportingSubstanceComparator;
import ix.core.models.Keyword;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.PolymerSubstanceBuilder;
import ix.ginas.modelBuilders.StructurallyDiverseSubstanceBuilder;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExportingSubstanceComparatorTest {

    //case 1: no dependencies - expect result 0
    @Test
    public void testSameOrder() {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder1 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder1.setStructureWithDefaultReference("c1ccccc1C");
        chemicalSubstanceBuilder1.setDisplayName("Toluene");
        ChemicalSubstance chemical1 = chemicalSubstanceBuilder1.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("c1ccccc1CC");
        chemicalSubstanceBuilder2.setDisplayName("Ethyl benzene");
        ChemicalSubstance chemical2 = chemicalSubstanceBuilder2.build();

        ExportingSubstanceComparator comparator = new ExportingSubstanceComparator();
        int result = comparator.compare(chemical1, chemical2);
        Assertions.assertEquals(0, result);
    }

    //case 1: no dependencies - expect result 0 (Assign IDs)
    @Test
    public void testSameOrderWithIds() {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder1 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder1.setStructureWithDefaultReference("c1ccccc1C");
        chemicalSubstanceBuilder1.setDisplayName("Toluene");
        chemicalSubstanceBuilder1.setUUID(UUID.randomUUID());
        ChemicalSubstance chemical1 = chemicalSubstanceBuilder1.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("c1ccccc1CC");
        chemicalSubstanceBuilder2.setDisplayName("Ethyl benzene");
        chemicalSubstanceBuilder2.setUUID(UUID.randomUUID());
        ChemicalSubstance chemical2 = chemicalSubstanceBuilder2.build();

        ExportingSubstanceComparator comparator = new ExportingSubstanceComparator();
        int result = comparator.compare(chemical1, chemical2);
        Assertions.assertEquals(0, result);
    }

    @Test
    public void testDep1() {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("c1ccccc1C=C");
        chemicalSubstanceBuilder2.setDisplayName("Styrene");
        chemicalSubstanceBuilder2.setUUID(UUID.randomUUID());
        ChemicalSubstance monomerSubstance = chemicalSubstanceBuilder2.build();

        PolymerSubstanceBuilder polymerBuilder = new PolymerSubstanceBuilder();
        polymerBuilder.addName("Polystyrene");
        polymerBuilder.setUUID(UUID.randomUUID());

        Polymer innerPolymer = new Polymer();
        Material monomer = new Material();
        monomer.monomerSubstance = new SubstanceReference();
        monomer.monomerSubstance.wrappedSubstance = monomerSubstance;
        monomer.monomerSubstance.refuuid = monomerSubstance.getUuid().toString();
        monomer.defining = true;
        innerPolymer.monomers.add(monomer);
        innerPolymer.displayStructure = new GinasChemicalStructure();
        innerPolymer.displayStructure.molfile = "\n" +
                "  ACCLDraw06052322162D\n" +
                "\n" +
                " 10 10  0  0  0  0  0  0  0  0999 V2000\n" +
                "    6.0090  -11.4714    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.0321  -10.8811    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0513  -11.4708    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0513  -12.6523    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.0347  -13.2418    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.0090  -12.6576    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.0321   -9.6996    0.0000 C   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "    6.0089   -9.1089    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0553   -9.1089    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.0784   -9.6996    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  2  1  2  0  0  0  0\n" +
                "  3  2  1  0  0  0  0\n" +
                "  4  3  2  0  0  0  0\n" +
                "  5  4  1  0  0  0  0\n" +
                "  1  6  1  0  0  0  0\n" +
                "  6  5  2  0  0  0  0\n" +
                "  2  7  1  0  0  0  0\n" +
                "  7  8  1  0  0  0  0\n" +
                "  7  9  1  0  0  0  0\n" +
                "  9 10  1  0  0  0  0\n" +
                "M  STY  1   1 SRU\n" +
                "M  SLB  1   1   1\n" +
                "M  SCN  1   1 HT \n" +
                "M  SAL   1  8   2   1   6   5   4   3   7   9\n" +
                "M  SBL   1  2   8  10\n" +
                "M  SDI   1  4    6.5205   -9.9947    6.5205   -8.8139\n" +
                "M  SDI   1  4    8.5669   -8.8139    8.5669   -9.9947\n" +
                "M  SMT   1 n\n" +
                "M  END\n";
        polymerBuilder.setPolymer(innerPolymer);

        PolymerSubstance polymerSubstance = polymerBuilder.build();

        ExportingSubstanceComparator comparator = new ExportingSubstanceComparator();
        int result = comparator.compare(polymerSubstance, monomerSubstance);
        Assertions.assertEquals(1, result);
    }

    @Test
    public void testDepRev() {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("c1ccccc1C=C");
        chemicalSubstanceBuilder2.setDisplayName("Styrene");
        chemicalSubstanceBuilder2.setUUID(UUID.randomUUID());
        ChemicalSubstance monomerSubstance = chemicalSubstanceBuilder2.build();

        PolymerSubstanceBuilder polymerBuilder = new PolymerSubstanceBuilder();
        polymerBuilder.addName("Polystyrene");
        polymerBuilder.setUUID(UUID.randomUUID());

        Polymer innerPolymer = new Polymer();
        Material monomer = new Material();
        monomer.monomerSubstance = new SubstanceReference();
        monomer.monomerSubstance.wrappedSubstance = monomerSubstance;
        monomer.monomerSubstance.refuuid = monomerSubstance.getUuid().toString();
        monomer.defining = true;
        innerPolymer.monomers.add(monomer);
        innerPolymer.displayStructure = new GinasChemicalStructure();
        innerPolymer.displayStructure.molfile = "\n" +
                "  ACCLDraw06052322162D\n" +
                "\n" +
                " 10 10  0  0  0  0  0  0  0  0999 V2000\n" +
                "    6.0090  -11.4714    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.0321  -10.8811    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0513  -11.4708    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0513  -12.6523    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.0347  -13.2418    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    6.0090  -12.6576    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    7.0321   -9.6996    0.0000 C   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "    6.0089   -9.1089    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    8.0553   -9.1089    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    9.0784   -9.6996    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  2  1  2  0  0  0  0\n" +
                "  3  2  1  0  0  0  0\n" +
                "  4  3  2  0  0  0  0\n" +
                "  5  4  1  0  0  0  0\n" +
                "  1  6  1  0  0  0  0\n" +
                "  6  5  2  0  0  0  0\n" +
                "  2  7  1  0  0  0  0\n" +
                "  7  8  1  0  0  0  0\n" +
                "  7  9  1  0  0  0  0\n" +
                "  9 10  1  0  0  0  0\n" +
                "M  STY  1   1 SRU\n" +
                "M  SLB  1   1   1\n" +
                "M  SCN  1   1 HT \n" +
                "M  SAL   1  8   2   1   6   5   4   3   7   9\n" +
                "M  SBL   1  2   8  10\n" +
                "M  SDI   1  4    6.5205   -9.9947    6.5205   -8.8139\n" +
                "M  SDI   1  4    8.5669   -8.8139    8.5669   -9.9947\n" +
                "M  SMT   1 n\n" +
                "M  END\n";
        polymerBuilder.setPolymer(innerPolymer);

        PolymerSubstance polymerSubstance = polymerBuilder.build();

        ExportingSubstanceComparator comparator = new ExportingSubstanceComparator();
        int result = comparator.compare(monomerSubstance, polymerSubstance);
        Assertions.assertEquals(-1, result);
    }


    @Test
    public void testStrDivDep() {
        StructurallyDiverseSubstanceBuilder wholePlantBuilder = new StructurallyDiverseSubstanceBuilder();
        wholePlantBuilder.addName("Himalayan maple");
        StructurallyDiverse inner1 = new StructurallyDiverse();
        inner1.organismFamily = "Sapindaceae";
        inner1.organismGenus = "Acer";
        inner1.organismSpecies = "caesium";
        inner1.sourceMaterialClass = "ORGANISM";
        inner1.sourceMaterialType = "PLANT";
        inner1.part = new EmbeddedKeywordList();
        inner1.part.add(new Keyword("WHOLE"));
        wholePlantBuilder.setStructurallyDiverse(inner1);
        wholePlantBuilder.setUUID(UUID.randomUUID());
        StructurallyDiverseSubstance wholePlant = wholePlantBuilder.build();

        StructurallyDiverseSubstanceBuilder leafBuilder = new StructurallyDiverseSubstanceBuilder();
        leafBuilder.addName("Maple leaf");
        StructurallyDiverse inner2 = new StructurallyDiverse();
        inner2.part = new EmbeddedKeywordList();
        inner2.part.add(new Keyword("Leaf"));
        inner1.organismFamily = "Sapindaceae";
        inner2.organismGenus = "Acer";
        inner2.organismSpecies = "caesium";
        inner2.sourceMaterialClass = "ORGANISM";
        inner2.sourceMaterialType = "PLANT";
        inner2.parentSubstance = new SubstanceReference();
        inner2.parentSubstance.wrappedSubstance = wholePlant;
        inner2.parentSubstance.refuuid = wholePlant.getUuid().toString();
        leafBuilder.setStructurallyDiverse(inner2);
        leafBuilder.setUUID(UUID.randomUUID());
        StructurallyDiverseSubstance leaf = leafBuilder.build();

        List<Substance> substances = new ArrayList<>();
        substances.add(leaf);
        substances.add(wholePlant);
        ExportingSubstanceComparator comparator = new ExportingSubstanceComparator();
        substances.sort(comparator);
        Assertions.assertEquals(wholePlant, substances.get(0));
    }
}
