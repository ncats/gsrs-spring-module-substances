package example.exports.comparators;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.comparators.ExportingSubstanceComparator;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.models.Keyword;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.MixtureSubstanceBuilder;
import ix.ginas.modelBuilders.PolymerSubstanceBuilder;
import ix.ginas.modelBuilders.StructurallyDiverseSubstanceBuilder;
import ix.ginas.models.EmbeddedKeywordList;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class ExportingSubstanceComparatorTest extends AbstractSubstanceJpaFullStackEntityTest {

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
        Assertions.assertEquals(chemical1.getName().compareTo(chemical2.getName()) , result);
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
        Assertions.assertEquals(chemical1.getUuid().compareTo(chemical2.getUuid()), result);
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

    @Test
    public void testMixtureSort() {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder1 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder1.setStructureWithDefaultReference("c1ccccc1C");
        UUID chemical1Id = UUID.randomUUID();
        chemicalSubstanceBuilder1.setDisplayName("Toluene");
        chemicalSubstanceBuilder1.setUUID(chemical1Id);
        ChemicalSubstance chemical1 = chemicalSubstanceBuilder1.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        UUID chemical2Id = UUID.randomUUID();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("c1ccccc1CC");
        chemicalSubstanceBuilder2.setUUID(chemical2Id);
        chemicalSubstanceBuilder2.setDisplayName("Ethylbenzene");
        ChemicalSubstance chemical2 = chemicalSubstanceBuilder2.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder3 = new ChemicalSubstanceBuilder();
        UUID chemical3Id = UUID.randomUUID();
        chemicalSubstanceBuilder3.setStructureWithDefaultReference("c1ccccc1CCC");
        chemicalSubstanceBuilder3.setUUID(chemical3Id);
        chemicalSubstanceBuilder3.setDisplayName("Propylbenzene");
        ChemicalSubstance chemical3 = chemicalSubstanceBuilder3.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder4 = new ChemicalSubstanceBuilder();
        UUID chemical4Id = UUID.randomUUID();
        chemicalSubstanceBuilder4.setStructureWithDefaultReference("c1ccccc1CCCC");
        chemicalSubstanceBuilder4.setUUID(chemical4Id);
        chemicalSubstanceBuilder4.setDisplayName("Butylbenzene");
        ChemicalSubstance chemical4 = chemicalSubstanceBuilder4.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder5 = new ChemicalSubstanceBuilder();
        UUID chemical5Id = UUID.randomUUID();
        chemicalSubstanceBuilder5.setStructureWithDefaultReference("c1ccccc1CCCCC");
        chemicalSubstanceBuilder5.setUUID(chemical5Id);
        chemicalSubstanceBuilder5.setDisplayName("Pentylbenzene");
        ChemicalSubstance chemical5 = chemicalSubstanceBuilder5.build();

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        UUID mixtureId = UUID.randomUUID();
        mixtureSubstanceBuilder.setUUID(mixtureId);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical1);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical2);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical3);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical4);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical5);

        MixtureSubstance mixture= mixtureSubstanceBuilder.build();
        ExportingSubstanceComparator comparator = new ExportingSubstanceComparator();
        List<Substance> substances = new ArrayList<>();
        substances.add(mixture);
        substances.add(chemical5);
        substances.add(chemical4);
        substances.add(chemical3);
        substances.add(chemical2);
        substances.add(chemical1);

        substances.sort(comparator);
        Assertions.assertEquals(mixtureId, substances.get(5).getUuid());
    }

    @Test
    public void testMixtureSort2() {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder1 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder1.setStructureWithDefaultReference("c1ccccc1C");
        UUID chemical1Id = UUID.randomUUID();
        chemicalSubstanceBuilder1.setDisplayName("Toluene");
        chemicalSubstanceBuilder1.setUUID(chemical1Id);
        ChemicalSubstance chemical1 = chemicalSubstanceBuilder1.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        UUID chemical2Id = UUID.randomUUID();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("c1ccccc1CC");
        chemicalSubstanceBuilder2.setUUID(chemical2Id);
        chemicalSubstanceBuilder2.setDisplayName("Ethylbenzene");
        ChemicalSubstance chemical2 = chemicalSubstanceBuilder2.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder3 = new ChemicalSubstanceBuilder();
        UUID chemical3Id = UUID.randomUUID();
        chemicalSubstanceBuilder3.setStructureWithDefaultReference("c1ccccc1CCC");
        chemicalSubstanceBuilder3.setUUID(chemical3Id);
        chemicalSubstanceBuilder3.setDisplayName("Propylbenzene");
        ChemicalSubstance chemical3 = chemicalSubstanceBuilder3.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder4 = new ChemicalSubstanceBuilder();
        UUID chemical4Id = UUID.randomUUID();
        chemicalSubstanceBuilder4.setStructureWithDefaultReference("c1ccccc1CCCC");
        chemicalSubstanceBuilder4.setUUID(chemical4Id);
        chemicalSubstanceBuilder4.setDisplayName("Butylbenzene");
        ChemicalSubstance chemical4 = chemicalSubstanceBuilder4.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder5 = new ChemicalSubstanceBuilder();
        UUID chemical5Id = UUID.randomUUID();
        chemicalSubstanceBuilder5.setStructureWithDefaultReference("c1ccccc1CCCCC");
        chemicalSubstanceBuilder5.setUUID(chemical5Id);
        chemicalSubstanceBuilder5.setDisplayName("Pentylbenzene");
        ChemicalSubstance chemical5 = chemicalSubstanceBuilder5.build();

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        UUID mixtureId = UUID.randomUUID();
        mixtureSubstanceBuilder.setUUID(mixtureId);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical1);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical2);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical3);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical4);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical5);
        mixtureSubstanceBuilder.addName("Mixed aromatic hydrocarbons");
        ChemicalSubstanceBuilder unrelatedChemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        UUID unrelatedChemicalId = UUID.randomUUID();
        unrelatedChemicalSubstanceBuilder.setStructureWithDefaultReference("CCCCC");
        unrelatedChemicalSubstanceBuilder.setUUID(unrelatedChemicalId);
        unrelatedChemicalSubstanceBuilder.setDisplayName("Butane");
        ChemicalSubstance unrelatedChemical = unrelatedChemicalSubstanceBuilder.build();

        MixtureSubstance mixture= mixtureSubstanceBuilder.build();
        ExportingSubstanceComparator comparator = new ExportingSubstanceComparator();
        List<Substance> substances = new ArrayList<>();
        substances.add(mixture);
        substances.add(unrelatedChemical);
        substances.add(chemical5);
        substances.add(chemical4);
        substances.add(chemical3);
        substances.add(chemical2);
        substances.add(chemical1);

        substances.sort(comparator);
        List<String> sortedIds = substances.stream().map(s->s.getUuid().toString()).collect(Collectors.toList());
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical1Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical2Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical3Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical4Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical5Id.toString()));
    }

    @Test
    public void testMixtureSort2Array() {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder1 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder1.setStructureWithDefaultReference("c1ccccc1C");
        UUID chemical1Id = UUID.randomUUID();
        chemicalSubstanceBuilder1.setDisplayName("Toluene");
        chemicalSubstanceBuilder1.setUUID(chemical1Id);
        ChemicalSubstance chemical1 = chemicalSubstanceBuilder1.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        UUID chemical2Id = UUID.randomUUID();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("c1ccccc1CC");
        chemicalSubstanceBuilder2.setUUID(chemical2Id);
        chemicalSubstanceBuilder2.setDisplayName("Ethylbenzene");
        ChemicalSubstance chemical2 = chemicalSubstanceBuilder2.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder3 = new ChemicalSubstanceBuilder();
        UUID chemical3Id = UUID.randomUUID();
        chemicalSubstanceBuilder3.setStructureWithDefaultReference("c1ccccc1CCC");
        chemicalSubstanceBuilder3.setUUID(chemical3Id);
        chemicalSubstanceBuilder3.setDisplayName("Propylbenzene");
        ChemicalSubstance chemical3 = chemicalSubstanceBuilder3.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder4 = new ChemicalSubstanceBuilder();
        UUID chemical4Id = UUID.randomUUID();
        chemicalSubstanceBuilder4.setStructureWithDefaultReference("c1ccccc1CCCC");
        chemicalSubstanceBuilder4.setUUID(chemical4Id);
        chemicalSubstanceBuilder4.setDisplayName("Butylbenzene");
        ChemicalSubstance chemical4 = chemicalSubstanceBuilder4.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder5 = new ChemicalSubstanceBuilder();
        UUID chemical5Id = UUID.randomUUID();
        chemicalSubstanceBuilder5.setStructureWithDefaultReference("c1ccccc1CCCCC");
        chemicalSubstanceBuilder5.setUUID(chemical5Id);
        chemicalSubstanceBuilder5.setDisplayName("Pentylbenzene");
        ChemicalSubstance chemical5 = chemicalSubstanceBuilder5.build();

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        UUID mixtureId = UUID.randomUUID();
        mixtureSubstanceBuilder.setUUID(mixtureId);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical1);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical2);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical3);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical4);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical5);
        mixtureSubstanceBuilder.addName("Mixed aromatic hydrocarbons");
        ChemicalSubstanceBuilder unrelatedChemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        UUID unrelatedChemicalId = UUID.randomUUID();
        unrelatedChemicalSubstanceBuilder.setStructureWithDefaultReference("CCCCC");
        unrelatedChemicalSubstanceBuilder.setUUID(unrelatedChemicalId);
        unrelatedChemicalSubstanceBuilder.setDisplayName("Butane");
        ChemicalSubstance unrelatedChemical = unrelatedChemicalSubstanceBuilder.build();

        MixtureSubstance mixture= mixtureSubstanceBuilder.build();
        ExportingSubstanceComparator comparator = new ExportingSubstanceComparator();
        List<Substance> substances = new ArrayList<>();
        substances.add(mixture);
        substances.add(unrelatedChemical);
        substances.add(chemical5);
        substances.add(chemical4);
        substances.add(chemical3);
        substances.add(chemical2);
        substances.add(chemical1);
        Substance[] substanceArray = substances.toArray(new Substance[]{});
        Arrays.sort(substanceArray, comparator);

        List<String> sortedIds = Arrays.stream(substanceArray).map(s->s.getUuid().toString()).collect(Collectors.toList());
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical1Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical2Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical3Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical4Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical5Id.toString()));
    }

    @Test
    public void testMixtureSort3() {
        ChemicalSubstanceBuilder chemicalSubstanceBuilder1 = new ChemicalSubstanceBuilder();
        chemicalSubstanceBuilder1.setStructureWithDefaultReference("c1ccccc1C");
        UUID chemical1Id = UUID.randomUUID();
        chemicalSubstanceBuilder1.setDisplayName("Toluene");
        chemicalSubstanceBuilder1.setUUID(chemical1Id);
        ChemicalSubstance chemical1 = chemicalSubstanceBuilder1.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder2 = new ChemicalSubstanceBuilder();
        UUID chemical2Id = UUID.randomUUID();
        chemicalSubstanceBuilder2.setStructureWithDefaultReference("c1ccccc1CC");
        chemicalSubstanceBuilder2.setUUID(chemical2Id);
        chemicalSubstanceBuilder2.setDisplayName("Ethylbenzene");
        ChemicalSubstance chemical2 = chemicalSubstanceBuilder2.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder3 = new ChemicalSubstanceBuilder();
        UUID chemical3Id = UUID.randomUUID();
        chemicalSubstanceBuilder3.setStructureWithDefaultReference("c1ccccc1CCC");
        chemicalSubstanceBuilder3.setUUID(chemical3Id);
        chemicalSubstanceBuilder3.setDisplayName("Propylbenzene");
        ChemicalSubstance chemical3 = chemicalSubstanceBuilder3.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder4 = new ChemicalSubstanceBuilder();
        UUID chemical4Id = UUID.randomUUID();
        chemicalSubstanceBuilder4.setStructureWithDefaultReference("c1ccccc1CCCC");
        chemicalSubstanceBuilder4.setUUID(chemical4Id);
        chemicalSubstanceBuilder4.setDisplayName("Butylbenzene");
        ChemicalSubstance chemical4 = chemicalSubstanceBuilder4.build();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder5 = new ChemicalSubstanceBuilder();
        UUID chemical5Id = UUID.randomUUID();
        chemicalSubstanceBuilder5.setStructureWithDefaultReference("c1ccccc1CCCCC");
        chemicalSubstanceBuilder5.setUUID(chemical5Id);
        chemicalSubstanceBuilder5.setDisplayName("Pentylbenzene");
        ChemicalSubstance chemical5 = chemicalSubstanceBuilder5.build();

        MixtureSubstanceBuilder mixtureSubstanceBuilder = new MixtureSubstanceBuilder();
        UUID mixtureId = UUID.randomUUID();
        mixtureSubstanceBuilder.setUUID(mixtureId);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical1);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical2);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical3);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical4);
        mixtureSubstanceBuilder.addComponents("MUST_BE_PRESENT", chemical5);
        mixtureSubstanceBuilder.addName("Mixed aromatic hydrocarbons");
        ChemicalSubstanceBuilder unrelatedChemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        UUID unrelatedChemicalId = UUID.randomUUID();
        unrelatedChemicalSubstanceBuilder.setStructureWithDefaultReference("CCCCC");
        unrelatedChemicalSubstanceBuilder.setUUID(unrelatedChemicalId);
        unrelatedChemicalSubstanceBuilder.setDisplayName("Butane");
        ChemicalSubstance unrelatedChemical = unrelatedChemicalSubstanceBuilder.build();

        MixtureSubstance mixture= mixtureSubstanceBuilder.build();
        ExportingSubstanceComparator comparator = new ExportingSubstanceComparator();
        List<Substance> substances = new ArrayList<>();
        substances.add(mixture);
        substances.add(chemical5);
        substances.add(chemical4);
        substances.add(chemical3);
        substances.add(chemical2);
        substances.add(chemical1);
        substances.add(unrelatedChemical);

        substances.sort(comparator);
        List<String> sortedIds = substances.stream().map(s->s.getUuid().toString()).collect(Collectors.toList());
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical1Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical2Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical3Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical4Id.toString()));
        Assertions.assertTrue( sortedIds.indexOf(mixtureId.toString()) > sortedIds.indexOf(chemical5Id.toString()));
    }

}
