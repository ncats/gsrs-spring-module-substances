package example.substance;

import ix.core.models.Structure;
import ix.ginas.models.v1.GinasChemicalStructure;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class StructureMethodTest {

    @Test
    void testGetInchiSet() throws Exception {
        File molfile =new File(getClass().getResource("/molfiles/SV1ATP0KYY.mol").getFile());
        Structure  testStructure = new GinasChemicalStructure();
        testStructure.stereoChemistry = Structure.Stereo.EPIMERIC;
        testStructure.molfile = Files.readString(molfile.toPath());
        List<String> inchiKeys=testStructure.getInChIKeysAndThrow();
        Assertions.assertEquals(2, inchiKeys.size());
        inchiKeys.forEach(r-> System.out.printf("inchikey: %s\n", r));
    }
}
