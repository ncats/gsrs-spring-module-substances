package ix.ginas.utils.validation.validators.tags;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
/*

@alx652 can you rewrite tagutils to have a method called List<String> getBracketTerms
And use that instead of the one that returns optional
And use essentially the same regex as #3?
If there's a better regex tho, that's fine

public static class BracketExtraction{
    private String namePart;
    private List<String> tagTerms;
}
       And return BracketExtraction
        The reason is that we also need to detect these terms as part of name standardization to avoid changing the [ that are part of a tag to (
        So the name standardizer could standardize only the namePart part, and then re-add any tagTerms
        You can also have another version of the method that calls this base method and just returns the content of tagTerms

Input	Expected 1	Expected 2
ibuprofen [INN]	INN
ibuprofen[INN]	<nothing>
ibuprofen [INN][USAN]	INN	USAN
ibuprofen[INN][USAN]	<nothing>
1,2-dimethyl[something-or-other]	<nothing>
ibuprofen [WHO-DD]	WHO-DD
1,2-dimethyl[something-or-other] [INN]	INN

*/

public class TagUtilities{

    // Assumes there is at most one tag term per name, and it's at the end of the name.
    public final static Pattern bracketTermRegex = Pattern.compile("\\[([^\\[\\]]*)\\]\\s*$");

    public static Optional<String> getBracketTerm(String name){
        // Return empty if no match, otherwise return the bracket term
        Objects.requireNonNull(name, "The name parameter in method getBracketTerm must not be null.");
        Optional<String> tagTerm = Optional.empty();
        Matcher regexMatcher = bracketTermRegex.matcher(name);
        if (regexMatcher.find()) {
            tagTerm = Optional.of(regexMatcher.group(1));
        }
        return tagTerm;
    }

    public static Optional<String> getBracketTerm(Name name){
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

    public static List<String> sortTagsHashSet(Set<String> tags){
        List<String> sorted = new ArrayList<>(tags);
        Collections.sort(sorted);
        return sorted;
    }
}