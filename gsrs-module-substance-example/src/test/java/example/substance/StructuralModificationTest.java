package example.substance;

import ix.ginas.models.v1.StructuralModification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StructuralModificationTest {

    @Test
    public void forceUpdateAllowsAbsentSiteContainer() {
        StructuralModification modification = new StructuralModification();

        Assertions.assertDoesNotThrow(modification::forceUpdate);
        Assertions.assertTrue(modification.getSites().isEmpty());
    }
}
