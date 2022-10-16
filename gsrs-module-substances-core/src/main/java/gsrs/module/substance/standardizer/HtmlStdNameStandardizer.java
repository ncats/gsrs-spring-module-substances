package gsrs.module.substance.standardizer;

import ix.ginas.utils.validation.validators.tags.TagUtilities;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Egor Puzanov
 */
@Slf4j
public class HtmlStdNameStandardizer extends HtmlNameStandardizer{

    public boolean upperCase = true;
    public boolean preserveTag = true;
    public boolean nkfdNormalize = true;
    public boolean removeUnprintables = true;
    public Pattern[] search = {Pattern.compile("\u2190"), Pattern.compile("\u2192"),
        Pattern.compile("±"), Pattern.compile("\u00B1"), Pattern.compile("\\×"),
        Pattern.compile("\u00B9"), Pattern.compile("\u00B2"), Pattern.compile("\u00B3"),
        Pattern.compile("\u2070"), Pattern.compile("\u2071"), Pattern.compile("\u2072"),
        Pattern.compile("\u2073"), Pattern.compile("\u2074"), Pattern.compile("\u2075"),
        Pattern.compile("\u2076"), Pattern.compile("\u2077"), Pattern.compile("\u2078"),
        Pattern.compile("\u2079"), Pattern.compile("\u207A"), Pattern.compile("\u207B"),
        Pattern.compile("\u2080"), Pattern.compile("\u2081"), Pattern.compile("\u2082"),
        Pattern.compile("\u2083"), Pattern.compile("\u2084"), Pattern.compile("\u2085"),
        Pattern.compile("\u2086"), Pattern.compile("\u2087"), Pattern.compile("\u2088"),
        Pattern.compile("\u2089"), Pattern.compile("\u208A"), Pattern.compile("\u208B"),
        Pattern.compile("ʟ"), Pattern.compile("ᴅ"), Pattern.compile("[\u200B\u200C\u200D\u2060\uFEFF]"),
        Pattern.compile("[\u00B4\u02B9\u02BC\u02C8\u0301\u2018\u2019\u201B\u2032\u2034\u2037]"),
        Pattern.compile("[\u00AB\u00BB\u02BA\u030B\u030E\u201C\u201D\u201E\u201F\u2033\u2036\u3003\u301D\u301E]"),
        Pattern.compile("[\u00AD\u2010\u2011\u2012\u2013\u2014\u2212\u2015]"),
        Pattern.compile("[\u01C3\u2762]"), Pattern.compile("[\u266F]"), Pattern.compile("[\u066A\u2052]"),
        Pattern.compile("[\u066D\u204E\u2217\u2731\u00D7]"), Pattern.compile("[\u201A\uFE51\uFF64\u3001]"),
        Pattern.compile("[\u00F7\u0338\u2044\u2215]"), Pattern.compile("[\u0589\u05C3\u2236]"),
        Pattern.compile("[\u203D]"), Pattern.compile("[\u27E6]"), Pattern.compile("[\u20E5\u2216]"),
        Pattern.compile("[\u301B]"), Pattern.compile("[\u02C4\u02C6\u0302\u2038\u2303]"),
        Pattern.compile("[\u02CD\u0331\u0332\u2017]"), Pattern.compile("[\u02CB\u0300\u2035]"),
        Pattern.compile("[\u2983]"), Pattern.compile("[\u01C0\u05C0\u2223\u2758]"),
        Pattern.compile("[\u2016]"), Pattern.compile("[\u02DC\u0303\u2053\u223C\u301C]"),
        Pattern.compile("[\u2039\u2329\u27E8\u3008]"), Pattern.compile("[\u2264\u2266]"),
        Pattern.compile("[\u203A\u232A\u27E9\u3009]"), Pattern.compile("[\u2265\u2267]"),
        Pattern.compile("[\uFEFF]"), Pattern.compile("\u2153"), Pattern.compile("\u2154"),
        Pattern.compile("\u2155"), Pattern.compile("\u2156"), Pattern.compile("\u2157"),
        Pattern.compile("\u2158"), Pattern.compile("\u2159"), Pattern.compile("\u215A"),
        Pattern.compile("\u215B"), Pattern.compile("\u215C"), Pattern.compile("\u215D"),
        Pattern.compile("\u215E"), Pattern.compile("\u00BC"), Pattern.compile("\u00BD"),
        Pattern.compile("\u2026"), Pattern.compile("﹘"), Pattern.compile("\\["),
        Pattern.compile("\\{"), Pattern.compile("\\]"), Pattern.compile("\\}"),
        Pattern.compile("\\s{2,}"), Pattern.compile("\u03B1"), Pattern.compile("\u03B2"),
        Pattern.compile("\u03B3"), Pattern.compile("\u03B4"), Pattern.compile("\u03B5"),
        Pattern.compile("\u03B6"), Pattern.compile("\u03B7"), Pattern.compile("\u03B8"),
        Pattern.compile("\u03B9"), Pattern.compile("\u03BA"), Pattern.compile("\u03BB"),
        Pattern.compile("\u03BC"), Pattern.compile("\u03BD"), Pattern.compile("\u03BE"),
        Pattern.compile("\u03BF"), Pattern.compile("\u03C0"), Pattern.compile("\u03C1"),
        Pattern.compile("\u03C2"), Pattern.compile("\u03C3"), Pattern.compile("\u03C4"),
        Pattern.compile("\u03C5"), Pattern.compile("\u03C6"), Pattern.compile("\u03C7"),
        Pattern.compile("\u03C8"), Pattern.compile("\u03C9"), Pattern.compile("\u0391"),
        Pattern.compile("\u0392"), Pattern.compile("\u0393"), Pattern.compile("\u0394"),
        Pattern.compile("\u0395"), Pattern.compile("\u0396"), Pattern.compile("\u0397"),
        Pattern.compile("\u0398"), Pattern.compile("\u0399"), Pattern.compile("\u039A"),
        Pattern.compile("\u039B"), Pattern.compile("\u039C"), Pattern.compile("\u039D"),
        Pattern.compile("\u039E"), Pattern.compile("\u039F"), Pattern.compile("\u03A0"),
        Pattern.compile("\u03A1"), Pattern.compile("\u03A3"), Pattern.compile("\u03A4"),
        Pattern.compile("\u03A5"), Pattern.compile("\u03A6"), Pattern.compile("\u03A7"),
        Pattern.compile("\u03A8"), Pattern.compile("\u03A9"), Pattern.compile("<i>"),
        Pattern.compile("</i>"), Pattern.compile("</sup><sup>"), Pattern.compile("</sub><sub>"),
        Pattern.compile("<sup>"), Pattern.compile("</sup>"), Pattern.compile("<sub>"),
        Pattern.compile("</sub>")};

    public String[] replace = {"<-", "->", "+/-", "+/-", "X", "1", "2", "3", "0", "1", "2",
        "3", "4", "5", "6", "7", "8", "9", "+", "-", "0" , "1", "2", "3", "4", "5", "6",
        "7", "8", "9", "+", "-", "L", "D", "", "'", "\"", "-", "!", "#", "%", "*", ",",
        "/", ":", "?", "[", "\\", "]", "^", "_", "`", "(", "|", "||", "~", "<", "<=", ">",
        ">=", " ", "1/3", "2/3", "1/5", "2/5", "3/5", "4/5", "1/6", "5/6", "1/8", "3/8",
        "5/8", "7/8", "1/4", "1/2", "...", "-", "(", "(", ")", ")",
        " ", ".ALPHA.", ".BETA.", ".GAMMA.", ".DELTA.", ".EPSILON.",
        ".ZETA.", ".ETA.", ".THETA.", ".IOTA.", ".KAPPA.", ".LAMBDA.", ".MU.", ".NU.",
        ".XI.", ".OMICRON.", ".PI.", ".RHO.", ".SIGMA.", ".SIGMA.", ".TAU.", ".UPSILON.",
        ".PHI.", ".CHI.", ".PSI.", ".OMEGA.", ".ALPHA.", ".BETA.", ".GAMMA.", ".DELTA.",
        ".EPSILON.", ".ZETA.", ".ETA.", ".THETA.", ".IOTA.", ".KAPPA.", ".LAMBDA.",
        ".MU.", ".NU.", ".XI.", ".OMICRON.", ".PI.", ".RHO.", ".SIGMA.",
        ".TAU.", ".UPSILON.", ".PHI.", ".CHI.", ".PSI.", ".OMEGA.", "", "", "", "",
        "SUP(", ")", "SUB(", ")"};

    @Override
    public ReplacementResult standardize(String input) {
        ReplacementResult result = new ReplacementResult(input.trim(), new ArrayList<>());
        if (input != null && input.length() != 0) {
            String suffix = "";
            result.update(this.cleanHtml(result.getResult()));
            if (preserveTag) {
                TagUtilities.BracketExtraction extract = TagUtilities.getBracketExtraction(result.getResult());
                String namePart = extract.getNamePart();
                suffix = extract.getTagTerms().stream().map(f->"[" + f + "]").collect(Collectors.joining(""));
                if(suffix.length() > 0 && namePart != null){
                    result.setResult(namePart);
                }
            }
            result.update(this.replaceRegexLists(result.getResult(), search, replace));
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
