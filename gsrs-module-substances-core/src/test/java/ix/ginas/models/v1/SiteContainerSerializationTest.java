package ix.ginas.models.v1;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SiteContainerSerializationTest {

    @Test
    void sitesShouldPersistAsCompactShorthandWithoutExpandedJson() {
        SiteContainer container = new SiteContainer(Linkage.class.getName());
        List<Site> sites = new ArrayList<>();
        for (int residue = 2; residue <= 20; residue++) {
            sites.add(new Site(1, residue));
        }

        container.setSites(sites);

        assertEquals("1_2-1_20", container.sitesShortHand);
        assertEquals("[]", container.sitesJSON);
        assertEquals(19, container.siteCount);
        assertEquals(sites, container.getSites());
    }

    @Test
    void legacyExpandedJsonShouldStillBeReadableWithoutShorthand() {
        SiteContainer container = new SiteContainer(Linkage.class.getName());
        container.sitesJSON = "[{\"subunitIndex\":1,\"residueIndex\":2}]";

        assertEquals(List.of(new Site(1, 2)), container.getSites());
    }
}
