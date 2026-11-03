package ix.ginas.utils.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PeptideModificationNamesTest {
    @Test
    void modificationLabelsAreLocalToEachMoleculeAndReversible() {
        PeptideInterpreter.ModificationNames first = new PeptideInterpreter.ModificationNames();
        PeptideInterpreter.ModificationNames second = new PeptideInterpreter.ModificationNames();

        assertEquals("X1", first.nameFor("modified residue one"));
        assertEquals("X1", first.nameFor("modified residue one"));
        assertEquals("X2", first.nameFor("modified residue two"));
        assertEquals("modified residue two", first.smilesFor("X2"));
        assertEquals("X1", second.nameFor("different molecule residue"));
        assertEquals("different molecule residue", second.smilesFor("X1"));
        assertNull(second.smilesFor("X2"));
        assertEquals("modified residue one", first.smilesFor("X1"));
    }
}
