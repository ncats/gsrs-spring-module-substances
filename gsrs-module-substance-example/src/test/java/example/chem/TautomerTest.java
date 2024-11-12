package example.chem;

import gov.nih.ncats.molwitch.Chemical;
import gsrs.module.substance.utils.TautomerUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class TautomerTest {

    @Test
    public void testTautomers() throws IOException {
        String input = "OC1=C2NC=NC2=NC(=N)N1";
        TautomerUtils tu = new TautomerUtils();
        Chemical chemical = Chemical.createFromSmilesAndComputeCoordinates(input);
        List<String> tautomers = tu.getTautomerSmiles(chemical);
        System.out.printf("Tautomers: %s\n", tautomers);
        Assertions.assertTrue(tautomers.size() > 0);
    }
}
