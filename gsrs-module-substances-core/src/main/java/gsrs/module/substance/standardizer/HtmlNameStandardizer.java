package gsrs.module.substance.standardizer;

import gsrs.module.substance.utils.HtmlUtil;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Egor Puzanov
 */
public class HtmlNameStandardizer extends AbstractNameStandardizer{

    private static String[] utfStrings = {"\u00B9", "\u00B2", "\u00B3", "</sup><sup>", "</sub><sub>", "</i><i>"};
    private static String[] htmlStrings = {"<sup>1</sup>", "<sup>2</sup>", "<sup>3</sup>", "", "", ""};

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
        ReplacementResult result = new ReplacementResult(input, new ArrayList<>());
        if (input != null && input.length() != 0) {
            result.update(this.replaceRegex(result.getResult(), "\\p{C}", ""));
            result.update(this.replaceRegex(result.getResult(), "\\s{2,}", " "));
            result.update(this.replaceFromLists(result.getResult(), utfStrings, htmlStrings));
            result.update(this.cleanHtml(result.getResult()));
        }
        return result;
    }
}
