package gsrs.module.substance.misc.emasmsfhir;

import ix.core.models.Group;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExporterUtilitiesTest {

    @Test
    void replaceAllLinefeedsWithPipes_handlesUnixAndWindowsLineEndings() {
        String input = "line1\nline2\r\nline3";
        assertEquals("line1|line2|line3", ExporterUtilities.replaceAllLinefeedsWithPipes(input));
    }

    @Test
    void makeAccessGroupString_sortsAndJoinsGroupNames() {
        Group admin = new Group();
        admin.name = "admin";
        Group protectedGroup = new Group();
        protectedGroup.name = "Protected";

        Set<Group> groups = new HashSet<>();
        groups.add(admin);
        groups.add(protectedGroup);

        assertEquals("Protected, admin", ExporterUtilities.makeAccessGroupString(groups));
    }
}

