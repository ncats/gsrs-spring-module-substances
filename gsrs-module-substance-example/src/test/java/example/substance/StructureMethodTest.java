package example.substance;

import ix.core.models.Structure;
import ix.ginas.models.v1.GinasChemicalStructure;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class StructureMethodTest {

    @Test
    void testGetInchiKeySet() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/SV1ATP0KYY.mol").getFile());
        Structure  testStructure = new GinasChemicalStructure();
        testStructure.opticalActivity = Structure.Optical.PLUS_MINUS;
        testStructure.molfile = Files.readString(molfile.toPath());
        List<String> inchiKeys=testStructure.getInChIKeysAndThrow();
        Assertions.assertEquals(2, inchiKeys.size());
        inchiKeys.forEach(r-> System.out.printf("inchikey: %s\n", r));
    }

    @Test
    void testGetInchiKeySetNoDuplicates() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/XP9911I1WL.mol").getFile());
        Structure  testStructure = new GinasChemicalStructure();
        testStructure.stereoChemistry = Structure.Stereo.EPIMERIC;
        testStructure.opticalActivity = Structure.Optical.PLUS_MINUS;
        testStructure.molfile = Files.readString(molfile.toPath());
        List<String> inchiKeys=testStructure.getInChIKeysAndThrow();
        Assertions.assertEquals(4, inchiKeys.size());
        HashSet<String> unique = new HashSet<>();
        unique.addAll(inchiKeys);
        Assertions.assertEquals(unique.size(), inchiKeys.size());
        inchiKeys.forEach(r-> System.out.printf("inchikey: %s\n", r));
    }

    @Test
    void testGetInchiKeySetSingle() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/MDK48L6W8Q.mol").getFile());
        Structure  testStructure = new GinasChemicalStructure();
        testStructure.stereoChemistry = Structure.Stereo.ACHIRAL;
        testStructure.opticalActivity = Structure.Optical.NONE;
        testStructure.molfile = Files.readString(molfile.toPath());
        List<String> inchiKeys=testStructure.getInChIKeysAndThrow();
        Assertions.assertEquals(1, inchiKeys.size());
    }


    @Test
    void testGetInchiSet() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/SV1ATP0KYY.mol").getFile());
        Structure  testStructure = new GinasChemicalStructure();
        testStructure.opticalActivity = Structure.Optical.PLUS_MINUS;
        testStructure.molfile = Files.readString(molfile.toPath());
        List<String> inchis=testStructure.getInChIsAndThrow();
        Assertions.assertEquals(2, inchis.size());
        inchis.forEach(r-> System.out.printf("inchikey: %s\n", r));
    }

    @Test
    void testGetInchiSetNoDuplicates() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/XP9911I1WL.mol").getFile());
        Structure  testStructure = new GinasChemicalStructure();
        testStructure.stereoChemistry = Structure.Stereo.EPIMERIC;
        testStructure.opticalActivity = Structure.Optical.PLUS_MINUS;
        testStructure.molfile = Files.readString(molfile.toPath());
        List<String> inchis=testStructure.getInChIsAndThrow();
        Assertions.assertEquals(4, inchis.size());
        HashSet<String> unique = new HashSet<>();
        unique.addAll(inchis);
        Assertions.assertEquals(unique.size(), inchis.size());
        inchis.forEach(r-> System.out.printf("inchi: %s\n", r));
    }

    @Test
    void testGetInchiSetSingle() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/MDK48L6W8Q.mol").getFile());
        Structure  testStructure = new GinasChemicalStructure();
        testStructure.stereoChemistry = Structure.Stereo.ACHIRAL;
        testStructure.opticalActivity = Structure.Optical.NONE;
        testStructure.molfile = Files.readString(molfile.toPath());
        List<String> inchis=testStructure.getInChIsAndThrow();
        Assertions.assertEquals(1, inchis.size());
    }


}
