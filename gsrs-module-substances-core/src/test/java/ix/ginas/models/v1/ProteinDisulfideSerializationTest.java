package ix.ginas.models.v1;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProteinDisulfideSerializationTest {

    @Test
    void setDisulfideLinksShouldPersistCompactBackwardCompatibleJson() {
        Protein protein = new Protein();
        DisulfideLink link = new DisulfideLink();
        link.setSites(Arrays.asList(new Site(1, 22), new Site(1, 96)));

        protein.setDisulfideLinks(Collections.singletonList(link));

        assertEquals("[{\"sitesShorthand\":\"1_22;1_96\"}]", protein.disulfJSON);
        assertFalse(protein.disulfJSON.contains("createdBy"));
        assertFalse(protein.disulfJSON.contains("lastEditedBy"));
        assertFalse(protein.disulfJSON.contains("\"sites\""));
        assertFalse(protein.disulfJSON.contains("_matchContext"));

        DisulfideLink restored = protein.getDisulfideLinks().get(0);
        assertEquals("1_22;1_96", restored.getSitesShorthand());
        assertEquals(Arrays.asList(new Site(1, 22), new Site(1, 96)), restored.getSites());
    }

    @Test
    void setDisulfideLinksShouldHandleNullAsEmptyPayload() {
        Protein protein = new Protein();

        protein.setDisulfideLinks(null);

        assertEquals("[]", protein.disulfJSON);
        assertTrue(protein.getDisulfideLinks().isEmpty());
    }
}
