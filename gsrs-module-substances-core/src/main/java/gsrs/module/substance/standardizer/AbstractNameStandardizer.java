package gsrs.module.substance.standardizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author Egor Puzanov
 */
public abstract class AbstractNameStandardizer implements NameStandardizer{

    public Pattern[] search;
    public String[] replace;

    public void setSearch(Pattern[] searchList) {
        this.search = searchList;
    }

    public void setSearch(String[] searchList) {
        this.search = Arrays.stream(searchList)
                                .map(s->{
                                    if (s != null && !s.isEmpty()) return null;
                                    try{
                                        return Pattern.compile(s);
                                    }catch(Exception ex){
                                        return null;
                                    }
                                })
                                .toArray(Pattern[]::new);
    }

    public void setSearch(Map<Integer, String> m) {
        setSearch(m.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getValue()).toArray(String[]::new));
    }

    public void setReplace(String[] replaceList) {
        this.replace = replaceList;
    }

    public void setReplace(Map<Integer, String> m) {
        setReplace(m.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getValue()).toArray(String[]::new));
    }

    public void setReplacements(Map<String, String> m) {
        List<String> searchStrings = Arrays.stream(this.search).map(Pattern::toString).collect(Collectors.toList());
        List<String> replaceStrings = Arrays.asList(this.replace);
        for (Map.Entry<String, String> entry : m.entrySet()) {
            int idx = searchStrings.indexOf(entry.getKey());
            if (idx > -1) {
                replaceStrings.set(idx, entry.getValue());
            } else {
                searchStrings.add(entry.getKey());
                replaceStrings.add(entry.getValue());
            }
        }
        this.search = searchStrings.stream()
                                .map(s->{
                                    if (s != null && !s.isEmpty()) return null;
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
                if ("".equals(key)) {
                    continue;
                }
                String value = replaceList[i];

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

    public static ReplacementResult replaceRegexLists(String input, Pattern[] searchList, String[] replaceList) {
        List<ReplacementNote> notes = new ArrayList<>();
        ReplacementResult result = new ReplacementResult(input, notes);
        if (input != null && input.length() != 0) {
            for (int i = 0; i < searchList.length; i++) {
                Pattern pat = searchList[i];
                if (pat == null) {
                    continue;
                }
                Matcher matcher = pat.matcher(input);
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
}