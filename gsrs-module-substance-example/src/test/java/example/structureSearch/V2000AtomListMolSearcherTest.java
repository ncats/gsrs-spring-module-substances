package example.structureSearch;

import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.search.MolSearcher;
import gov.nih.ncats.molwitch.search.MolSearcherFactory;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2000AtomListMolSearcherTest {

    private static String atomListMolfile() {
        return "\n" +
                "  Ketcher  8132615362D 1   1.00000     0.00000     0\n" +
                "\n" +
                "  4  3  0  0  0  0            999 V2000\n" +
                "    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    1.5000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.0000    0.0000    0.0000 L   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.5000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  1  0  0  0  0\n" +
                "  3  4  1  0  0  0  0\n" +
                "M  ALS   3  2 F C   N   \n" +
                "M  END";
    }

    @Test
    public void chemicalMolSearcherShouldRespectV2000AtomList() throws Exception {
        Chemical query = Chemical.parse(atomListMolfile());

        assertTrue(query.hasQueryAtoms());
        assertTrue(query.getAtom(2).isQueryAtom());

        Optional<MolSearcher> searcher = MolSearcherFactory.create(query);
        assertTrue(searcher.isPresent());
        assertTrue(searcher.get().search(Chemical.parse("CCNC")).isPresent());
        assertTrue(searcher.get().search(Chemical.parse("CCCC")).isPresent());
        assertFalse(searcher.get().search(Chemical.parse("CCOC")).isPresent());
    }
}
