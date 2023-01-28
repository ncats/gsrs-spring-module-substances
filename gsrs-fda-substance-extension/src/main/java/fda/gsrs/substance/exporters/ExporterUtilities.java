package fda.gsrs.substance.exporters;

import ix.core.models.Group;

import java.util.Set;
import java.util.stream.Collectors;

public class ExporterUtilities {

    public static String replaceAllLinefeedsWithPipes(String s) {
        return s.replaceAll("\r?\n", "|");
    }

    public static String makeAccessGroupString(Set<Group> s) {
        return (String) s.stream().map(o->o.name).sorted().collect(Collectors.joining(", "));
    }


}
