package gsrs.module.substance.standardizer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;


/**
 *
 * @author Egor Puzanov
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public abstract class AbstractNameStandardizer implements NameStandardizer{

    private Pattern[] search;
    private String[] replace;

    public Pattern[] getSearch() {
        return this.search;
    }

    public void setSearch(Pattern[] searchList) {
        this.search = searchList;
        log.debug("NameStandardizer search list: " + Arrays.toString(this.search));
    }

    @JsonProperty
    public void setSearch(Map<Integer, String> m) {
        setSearch(m.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e->{
                        String s = e.getValue();
                        if (s == null || s.isEmpty()) return null;
                        try{
                            return Pattern.compile(s);
                        }catch(Exception ex){
                            return null;
                        }
                    })
                    .toArray(Pattern[]::new));
    }

    public String[] getReplace() {
        return this.replace;
    }

    public void setReplace(String[] replaceList) {
        this.replace = replaceList;
        log.debug("NameStandardizer replacement list: " + Arrays.toString(this.replace));
    }

    @JsonProperty
    public void setReplace(Map<Integer, String> m) {
        setReplace(m.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e->e.getValue())
                    .toArray(String[]::new));
    }

    public void setOverrides(Map<Integer, Map<Integer, String>> m) {
        List<String> searchStrings = Arrays.stream(this.search).map(Pattern::toString).collect(Collectors.toList());
        List<String> replaceStrings = Arrays.asList(this.replace);
        List<Map<Integer, String>> overrides = (List<Map<Integer, String>>) m.entrySet()
                                            .stream()
                                            .sorted(Map.Entry.comparingByKey())
                                            .map(e->e.getValue())
                                            .collect(Collectors.toList());
        for (Map<Integer, String> entry : overrides) {
            int idx = searchStrings.indexOf(entry.get(0));
            if (idx > -1) {
                replaceStrings.set(idx, entry.get(1));
            } else {
                searchStrings.add(entry.get(0));
                replaceStrings.add(entry.get(1));
            }
        }
        this.search = searchStrings.stream()
                                .map(s->{
                                    if (s == null || s.isEmpty()) return null;
                                    try{
                                        return Pattern.compile(s);
                                    }catch(Exception ex){
                                        return null;
                                    }
                                })
                                .toArray(Pattern[]::new);
        this.replace = replaceStrings.stream().toArray(String[]::new);
    }

    public static ReplacementResult replaceFromLists(String input, String[] searchList, String[] replaceList) {
        List<ReplacementNote> notes = new ArrayList<>();
        ReplacementResult result = new ReplacementResult(input, notes);
        if (input != null && input.length() != 0) {
            StringBuilder sb = new StringBuilder(input);
            for (int i = 0; i < searchList.length; i++) {
                String key = searchList[i];
                String value = replaceList[i];
                if ("".equals(key) || value == null) {
                    continue;
                }
                int start = sb.indexOf(key, 0);
                while (start > -1) {
                    int end = start + key.length();
                    int nextSearchStart = start + value.length();
                    sb.replace(start, end, value);
                    notes.add(new ReplacementNote(start, value));
                    start = sb.indexOf(key, nextSearchStart);
                }
            }
            result.setResult(sb.toString());
        }
        return result;
    }

    public ReplacementResult replaceRegexLists(String input) {
        return replaceRegexLists(input, search, replace);
    }

    public static ReplacementResult replaceRegexLists(String input, Pattern[] searchList, String[] replaceList) {
        List<ReplacementNote> notes = new ArrayList<>();
        ReplacementResult result = new ReplacementResult(input, notes);
        if (input != null && input.length() != 0) {
            for (int i = 0; i < searchList.length; i++) {
                Pattern pat = searchList[i];
                if (pat == null || replaceList[i] == null) {
                    continue;
                }
                Matcher matcher = pat.matcher(result.getResult());
                if (matcher.find()) {
                    notes.add(new ReplacementNote(matcher.start(), replaceList[i]));
                    result.setResult(matcher.replaceAll(replaceList[i]));
                }
            }
        }
        return result;
    }

    public static ReplacementResult replaceRegexLists(String input, String[] searchList, String[] replaceList) {
        Pattern[] patList = Arrays.stream(searchList)
                                .map(s->{
                                    if (s != null && !s.isEmpty()) return null;
                                    try{
                                        return Pattern.compile(s);
                                    }catch(Exception ex){
                                        return null;
                                    }
                                })
                                .toArray(Pattern[]::new);
        return replaceRegexLists(input, patList, replaceList);
    }

    public String nkfdNormalizations(String inputString) {
        log.trace(inputString);
        log.trace("Length:" + inputString.length());

        String normalized = Normalizer.normalize(inputString, Normalizer.Form.NFKD);
        log.trace(normalized);
        log.trace("Length:" + normalized.length());

        normalized = normalized.replaceAll("\\p{Mn}+", "");
        log.trace(normalized);
        log.trace("Length:" + normalized.length());

        normalized = normalized.replaceAll("[^\\p{ASCII}]", "?");
        log.trace(normalized);
        log.trace("Length:" + normalized.length());
        return normalized;
    }
}