package example.substance;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;

@WithMockUser(username = "admin", roles = "Admin")
public class StructureMethodTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private StructureProcessor structureProcessor;

    @Test
    void testGetInchiKeySet() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/21OG056YM2.mol").getFile());
        Structure structure = structureProcessor.instrument(Files.readString(molfile.toPath()));
        structure.opticalActivity = Structure.Optical.PLUS_MINUS;
        List<String> inchiKeys=structure.getInChIKeysAndThrow();
        Assertions.assertEquals(2, inchiKeys.size());
        inchiKeys.forEach(r-> System.out.printf("inchikey: %s\n", r));
    }

    @Test
    void testGetInchiKeySetNoDuplicates() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/XP9911I1WL.mol").getFile());
        Structure testStructure = structureProcessor.instrument(Files.readString(molfile.toPath()));
        testStructure.stereoChemistry = Structure.Stereo.EPIMERIC;
        testStructure.opticalActivity = Structure.Optical.PLUS_MINUS;
        testStructure.molfile = Files.readString(molfile.toPath());
        List<String> inchiKeys=testStructure.getInChIKeysAndThrow();
        Assertions.assertEquals(1, inchiKeys.size());
        HashSet<String> unique = new HashSet<>();
        unique.addAll(inchiKeys);
        Assertions.assertEquals(unique.size(), inchiKeys.size());
        inchiKeys.forEach(r-> System.out.printf("inchikey: %s\n", r));
    }

    @Test
    void testGetInchiKeySetSingle() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/MDK48L6W8Q.mol").getFile());
        Structure testStructure = structureProcessor.instrument(Files.readString(molfile.toPath()));
        testStructure.stereoChemistry = Structure.Stereo.ACHIRAL;
        testStructure.opticalActivity = Structure.Optical.NONE;
        List<String> inchiKeys=testStructure.getInChIKeysAndThrow();
        Assertions.assertEquals(1, inchiKeys.size());
    }


    @Test
    void testGetInchiSet() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/MX8724VFP3.mol").getFile());
        Structure testStructure = structureProcessor.instrument(Files.readString(molfile.toPath()));
        testStructure.opticalActivity = Structure.Optical.PLUS_MINUS;
        List<String> inchis=testStructure.getInChIsAndThrow();
        Assertions.assertEquals(2, inchis.size());
        inchis.forEach(r-> System.out.printf("inchikey: %s\n", r));
    }

    @Test
    void testGetInchiSetNoDuplicates() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/XP9911I1WL.mol").getFile());
        Structure testStructure = structureProcessor.instrument(Files.readString(molfile.toPath()));
        testStructure.stereoChemistry = Structure.Stereo.EPIMERIC;
        testStructure.opticalActivity = Structure.Optical.PLUS_MINUS;
        List<String> inchis=testStructure.getInChIsAndThrow();
        Assertions.assertEquals(1, inchis.size());
        HashSet<String> unique = new HashSet<>();
        unique.addAll(inchis);
        Assertions.assertEquals(unique.size(), inchis.size());
        inchis.forEach(r-> System.out.printf("inchi: %s\n", r));
    }

    @Test
    void testGetInchiSetSingle() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/MDK48L6W8Q.mol").getFile());
        Structure testStructure = structureProcessor.instrument(Files.readString(molfile.toPath()));
        testStructure.stereoChemistry = Structure.Stereo.ACHIRAL;
        testStructure.opticalActivity = Structure.Optical.NONE;
        List<String> inchis=testStructure.getInChIsAndThrow();
        Assertions.assertEquals(1, inchis.size());
    }


}
