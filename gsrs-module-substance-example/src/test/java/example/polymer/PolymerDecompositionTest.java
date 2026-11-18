package example.polymer;

import gov.nih.ncats.molwitch.Chemical;
import ix.core.chem.PolymerDecode;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PolymerDecompositionTest {

    @Test
    void testPolymerDecomposition() throws IOException {
        //todo: turn into resource
        String dataFileName = "Truxihyaluronate_Sodium.mol";
        File proteinFile = new ClassPathResource("molfiles/" + dataFileName).getFile();
        String polymerMolfile = Files.readString(proteinFile.toPath());
        Chemical polymer = Chemical.parse(polymerMolfile);
        Set<PolymerDecode.StructuralUnit> SRUs = PolymerDecode.DecomposePolymerSU(polymer);
        assertFalse(SRUs.isEmpty());
        SRUs.forEach( sru->{
            System.out.printf("SRU label: %s%n",
                    sru.label );
        });
    }
}
