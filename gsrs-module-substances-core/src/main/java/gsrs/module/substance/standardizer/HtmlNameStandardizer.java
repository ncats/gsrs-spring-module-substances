package gsrs.module.substance.standardizer;

import gsrs.module.substance.utils.HtmlUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Egor Puzanov
 */
@Slf4j
public class HtmlNameStandardizer extends AbstractNameStandardizer{

    public Pattern[] search = {Pattern.compile("\\p{C}"), Pattern.compile("\\s{2,}"),
        Pattern.compile("\u00B9"), Pattern.compile("\u00B2"), Pattern.compile("\u00B3"),
        Pattern.compile("<-"), Pattern.compile("->"), Pattern.compile("\\+\\/-"),
        Pattern.compile("SUP\\(([^\\)]*)\\)"), Pattern.compile("SUB\\(([^\\)]*)\\)"),
        Pattern.compile("<small>L</small>"), Pattern.compile("<small>D</small>"),
        Pattern.compile("</sup><sup>"), Pattern.compile("</sub><sub>"), Pattern.compile("</i><i>")};

    public String[] replace = {"", " ", "<sup>1</sup>", "<sup>2</sup>", "<sup>3</sup>",
        "\u2190", "\u2192", "\u00B1", "<sup>$1</sup>", "<sub>$1</sub>", "ʟ", "ᴅ", "", "", ""};

    public static ReplacementResult cleanHtml(String input) {
        List<ReplacementNote> notes = new ArrayList<>();
        ReplacementResult result = new ReplacementResult(input, notes);
        if (input != null && input.length() != 0) {
            String cleaned = HtmlUtil.clean(input, "UTF-8");
            int start = StringUtils.indexOfDifference(input, cleaned);
            if (start > -1) {
                notes.add(new ReplacementNote(start, ""));
                result.setResult(cleaned);
            }
        }
        return result;
    }

    @Override
    public ReplacementResult standardize(String input) {
        ReplacementResult result = new ReplacementResult(input.trim(), new ArrayList<>());
        if (input != null && input.length() != 0) {
            result.update(this.replaceRegexLists(result.getResult(), search, replace));
            result.update(this.cleanHtml(result.getResult()));
            result.update(this.replaceRegexLists(result.getResult(), search, replace));
        }
        return result;
    }
}
