package gsrs.module.substance.misc.emasmsfhir;

import ix.core.models.Group;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExporterUtilities Tests")
class ExporterUtilitiesTest {

    @Test
    @DisplayName("Replaces Unix newlines with pipes")
    void replaceLinefeed_unixNewline() {
        assertEquals("a|b|c", ExporterUtilities.replaceAllLinefeedsWithPipes("a\nb\nc"));
    }

    @Test
    @DisplayName("Replaces Windows CRLF with pipes")
    void replaceLinefeed_windowsNewline() {
        assertEquals("a|b", ExporterUtilities.replaceAllLinefeedsWithPipes("a\r\nb"));
    }

    @Test
    @DisplayName("Replaces mixed newlines with pipes")
    void replaceLinefeed_mixedNewlines() {
        assertEquals("a|b|c", ExporterUtilities.replaceAllLinefeedsWithPipes("a\nb\r\nc"));
    }

    @Test
    @DisplayName("Returns unchanged string when no newlines present")
    void replaceLinefeed_noNewlines() {
        assertEquals("hello world", ExporterUtilities.replaceAllLinefeedsWithPipes("hello world"));
    }

    @Test
    @DisplayName("Returns empty string for empty input")
    void replaceLinefeed_emptyString() {
        assertEquals("", ExporterUtilities.replaceAllLinefeedsWithPipes(""));
    }

    @Test
    @DisplayName("Consecutive newlines each become a pipe")
    void replaceLinefeed_consecutiveNewlines() {
        assertEquals("a||b", ExporterUtilities.replaceAllLinefeedsWithPipes("a\n\nb"));
    }

    @Test
    @DisplayName("Returns empty string for empty group set")
    void makeAccessGroupString_emptySet() {
        assertEquals("", ExporterUtilities.makeAccessGroupString(new HashSet<>()));
    }

    @Test
    @DisplayName("Returns single group name when set contains one group")
    void makeAccessGroupString_singleGroup() {
        Set<Group> groups = new HashSet<>();
        groups.add(group("admin"));
        assertEquals("admin", ExporterUtilities.makeAccessGroupString(groups));
    }

    @Test
    @DisplayName("Returns sorted, comma-separated group names for multiple groups")
    void makeAccessGroupString_multipleGroupsSorted() {
        Set<Group> groups = new LinkedHashSet<>();
        groups.add(group("Protected"));
        groups.add(group("admin"));
        groups.add(group("Beta"));
        String result = ExporterUtilities.makeAccessGroupString(groups);
        assertEquals("Beta, Protected, admin", result);
    }

    @Test
    @DisplayName("Group names are sorted alphabetically")
    void makeAccessGroupString_twoGroupsSorted() {
        Set<Group> groups = new LinkedHashSet<>();
        groups.add(group("Gamma"));
        groups.add(group("Alpha"));
        String result = ExporterUtilities.makeAccessGroupString(groups);
        assertEquals("Alpha, Gamma", result);
    }

    private static Group group(String name) {
        Group g = new Group();
        g.name = name;
        return g;
    }
}

