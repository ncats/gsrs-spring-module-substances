package example.substance.polymer;

import gov.nih.ncats.molwitch.Chemical;
import ix.core.chem.PolymerDecode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class PolymerDecodeTest {

    @Test
    public void sruWithAstrickShouldBeCountedAsGarbageNotEndGroup() throws IOException {
        String mol = "\n" +
                "   JSDraw210231917082D\n" +
                "\n" +
                "  3  2  0  0  0  0            999 V2000\n" +
                "   13.1040   -7.1702    0.0000 *   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   14.4550   -6.3902    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   15.8060   -7.1702    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  1  0  0  0  0\n" +
                "M  STY  1   1 SRU\n" +
                "M  SAL   1  1   2\n" +
                "M  SBL   1  2   1   2\n" +
                "M  SMT   1 A\n" +
                "M  SDI   1  4   14.0920   -7.6382   14.0920   -4.7262\n" +
                "M  SDI   1  4   14.8720   -4.7262   14.8720   -7.6382\n" +
                "M  END";


        Set<PolymerDecode.StructuralUnit> srus = PolymerDecode.DecomposePolymerSU(Chemical.parse(mol));
        assertEquals(3, srus.size());
        Map<String, List<PolymerDecode.StructuralUnit>> map = srus.stream().collect(Collectors.groupingBy(s -> s.type));

        assertEquals(3, map.size());
        assertEquals(1, map.get("SRU-BLOCK").size());
        assertEquals(1, map.get("GARBAGE").size());
        assertEquals(1, map.get("END_GROUP").size());

        assertEquals(Arrays.asList(1), map.get("SRU-BLOCK").get(0).amap);
        assertEquals(Arrays.asList(0), map.get("GARBAGE").get(0).amap);
        assertEquals(Arrays.asList(2), map.get("END_GROUP").get(0).amap);

        System.out.println("number of structural units = " + srus.size());
        for (PolymerDecode.StructuralUnit unit : srus) {
            System.out.println(unit.amap);
            System.out.println(unit.type);
            Chemical chem = Chemical.parseMol(unit.structure);

            System.out.println(chem.getMass());
        }
    }

    @Test
    public void sruSortedDefault() throws IOException {
        String filePath = "D:\\temp projects\\ncats\\polymer_molfile_from.dammika.mol";
        File inputFile = new File(filePath);
        String mol = Files.readString(inputFile.toPath());

        Set<PolymerDecode.StructuralUnit> srus = PolymerDecode.DecomposePolymerSU(Chemical.parse(mol));
        assertFalse( srus.isEmpty());

        System.out.println("number of structural units = " + srus.size());
        for (PolymerDecode.StructuralUnit unit : srus) {
            System.out.println(unit.amap);
            System.out.println(unit.type);
            Chemical chem = Chemical.parseMol(unit.structure);

            System.out.println(chem.getMass());
        }
    }

    @Test
    public void sruSortedMw() throws IOException {
        String filePath = "D:\\temp projects\\ncats\\polymer_molfile_from.dammika.mol";
        File inputFile = new File(filePath);
        String mol = Files.readString(inputFile.toPath());
        ReflectionTestUtils.setField(PolymerDecode.class, "DefaultSortType", "ByMw");
        Collection<PolymerDecode.StructuralUnit> srus = PolymerDecode.DecomposePolymerSU(Chemical.parse(mol), true);
        assertFalse( srus.isEmpty());


        System.out.println("number of structural units = " + srus.size());
        for (PolymerDecode.StructuralUnit unit : srus) {
            System.out.printf("type: %s, label: %s%n", unit.type, unit.label);
            Chemical chem = Chemical.parseMol(unit.structure);

            System.out.printf("mw: %f %n", chem.getMass());
        }
    }

    @Test
    public void sruSortedExplicitlyByUsage() throws IOException {
        String filePath = "D:\\temp projects\\ncats\\polymer_molfile_from.dammika.mol";
        File inputFile = new File(filePath);
        String mol = Files.readString(inputFile.toPath());
        ReflectionTestUtils.setField(PolymerDecode.class, "DefaultSortType", "ByUsage");
        Collection<PolymerDecode.StructuralUnit> srus = PolymerDecode.DecomposePolymerSU(Chemical.parse(mol), true);
        assertFalse( srus.isEmpty());


        System.out.println("number of structural units = " + srus.size());
        for (PolymerDecode.StructuralUnit unit : srus) {
            System.out.printf("type: %s, label: %s, mwt: %f %n", unit.type, unit.label, Chemical.parseMol(unit.structure).getMass());
            Chemical chem = Chemical.parseMol(unit.structure);

            System.out.printf("mw: %f %n", chem.getMass());
        }
    }

    @Test
    public void sruSortedExplicitlyByLabel() throws IOException {
        String filePath = "D:\\temp projects\\ncats\\polymer_molfile_from.dammika.mol";
        File inputFile = new File(filePath);
        String mol = Files.readString(inputFile.toPath());
        ReflectionTestUtils.setField(PolymerDecode.class, "DefaultSortType", "ByLabel");
        Collection<PolymerDecode.StructuralUnit> srus = PolymerDecode.DecomposePolymerSU(Chemical.parse(mol), true);
        assertFalse(srus.isEmpty());

        System.out.println("number of structural units = " + srus.size());
        for (PolymerDecode.StructuralUnit unit : srus) {
            System.out.printf("type: %s, label: %s %n", unit.type, unit.label);
            Chemical chem = Chemical.parseMol(unit.structure);

            System.out.printf("mw: %f %n", chem.getMass());
        }
    }
}
