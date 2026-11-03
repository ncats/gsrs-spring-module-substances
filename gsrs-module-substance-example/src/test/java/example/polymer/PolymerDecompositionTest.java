package example.polymer;

import gov.nih.ncats.molwitch.Chemical;
import ix.core.chem.PolymerDecode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class PolymerDecompositionTest {

    @Test
    void testPolymerDecomposition() throws IOException {
        //todo: turn into resource
        String filePath = "D:\\temp projects\\ncats\\polymer_molfile_from.dammika.mol";
        File molfile= new File(filePath);
        String polymerMolfile = Files.readString(molfile.toPath());
        Chemical polymer = Chemical.parse(polymerMolfile);
        Set<PolymerDecode.StructuralUnit> SRUs = PolymerDecode.DecomposePolymerSU(polymer);
        assertFalse(SRUs.isEmpty());
        SRUs.forEach( sru->{
            System.out.printf("SRU label: %s%n",
                    sru.label );
        });
    }
}
