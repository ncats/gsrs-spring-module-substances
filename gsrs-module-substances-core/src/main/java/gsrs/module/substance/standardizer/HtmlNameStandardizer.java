package gsrs.module.substance.standardizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import gsrs.module.substance.utils.HtmlUtil;
import ix.ginas.utils.validation.validators.tags.TagUtilities;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Egor Puzanov
 */
@Slf4j
public class HtmlNameStandardizer extends AbstractNameStandardizer{

    public boolean plainText = false;
    public boolean upperCase = false;
    public boolean preserveTag = false;
    public boolean nkfdNormalize = false;
    public boolean removeUnprintables = false;

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
            String suffix = "";
            if(!plainText){
                result.update(this.cleanHtml(result.getResult()));
            }
            if (preserveTag) {
                TagUtilities.BracketExtraction extract = TagUtilities.getBracketExtraction(result.getResult());
                String namePart = extract.getNamePart();
                suffix = extract.getTagTerms().stream().map(f->"[" + f + "]").collect(Collectors.joining(""));
                if(suffix.length() > 0 && namePart != null){
                    result.setResult(namePart);
                }
            }
            result.update(this.replaceRegexLists(result.getResult()));
            if(nkfdNormalize){
                result.setResult(this.nkfdNormalizations(result.getResult()));
            }
            if(upperCase){
                result.setResult(result.getResult().toUpperCase());
            }
            if(suffix.length() > 0){
                result.setResult(result.getResult() + " " + suffix);
            }
            if(removeUnprintables){
                result.setResult(result.getResult().replaceAll("\\p{C}", ""));
            }
        }
        return result;
    }
}
