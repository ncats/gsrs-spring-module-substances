package gsrs.module.substance.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Egor Puzanov
 */
public abstract class AbstractNameStandardizer implements NameStandardizer{

    public static ReplacementResult replaceFromLists(String input, String[] searchList, String[] replaceList) {
        List<ReplacementNote> notes = new ArrayList<>();
        ReplacementResult result = new ReplacementResult(input, notes);
        if (input != null && input.length() != 0) {
            StringBuilder sb = new StringBuilder(input);
            for (int i = 0; i < searchList.length; i++)
            {
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

    public static ReplacementResult replaceRegex(String input, Pattern pat, String value) {
        List<ReplacementNote> notes = new ArrayList<>();
        ReplacementResult result = new ReplacementResult(input, notes);
        if (input != null && input.length() != 0) {
            Matcher matcher = pat.matcher(input);
            if (matcher.find()) {
                notes.add(new ReplacementNote(matcher.start(), value));
                String cleaned = matcher.replaceAll(value);
                result.setResult(cleaned);
            }
        }
        return result;
    }

    public static ReplacementResult replaceRegex(String input, String pat, String value) {
        return replaceRegex(input, Pattern.compile(pat), value);
    }
}