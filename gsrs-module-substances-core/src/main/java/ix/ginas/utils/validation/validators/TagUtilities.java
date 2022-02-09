package ix.ginas.utils.validation.validators;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j

public class TagUtilities{

    // Assumes there is at most one tag term per name, and it's at the end of the name.
    public final static Pattern bracketTermRegex = Pattern.compile("\\[([^\\[\\]]*)\\]\\s*$");

    public static Optional<String> getBracketTerm(String name){
        Optional<String> tagTerm = Optional.empty();
        // Is this the right way to handle NPE?
        if (name == null) {
            log.warn("The name string passed to getBracketTerm was null.");
            return Optional.empty();
        }
        Matcher regexMatcher = bracketTermRegex.matcher(name);
        if (regexMatcher.find()) {
            tagTerm = Optional.of(regexMatcher.group(1));
        }
        return tagTerm;
    }

    public static Optional<String> getBracketTerm(Name name){
        // port over the regex stuff here, return empty if no match, otherwise return the bracket term
        return getBracketTerm(name.getName());
    }

    public static Set<String> extractExplicitTags(Substance s){
        return s.tags
                .stream()
                .map(ss->ss.getValue())
                .collect(Collectors.toSet());
    }


    public static Set<String> extractBracketNameTags(Substance s){
        return s.getAllNames().stream()
                .map(nn->getBracketTerm(nn))
                .filter(op->op.isPresent())
                .map(op->op.get())
                .collect(Collectors.toSet());
    }

    public static Set<String> getSetAExcludesB(Set<String> setA, Set<String> setB){
        LinkedHashSet<String> newSet = new LinkedHashSet<>();
        newSet.addAll(setA);
        newSet.removeAll(setB);
        return newSet;
    }
    public static List<String> sortTagsHashSet(Set<String> tags) {
        List<String> sorted = new ArrayList<>(tags);
        Collections.sort(sorted);
        return sorted;
    }

}

