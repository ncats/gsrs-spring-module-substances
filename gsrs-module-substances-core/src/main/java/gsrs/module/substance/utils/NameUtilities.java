package gsrs.module.substance.utils;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author mitch
 */
@Slf4j
public class NameUtilities {

    public NameUtilities() {
        initReplacers();
    }

    private static final String REPLACEMENT_SOURCE_GREEK = "\u03B1;.ALPHA.;\u03B2;.BETA.;\u03B3;.GAMMA.;\u03B4;.DELTA.;\u03B5;.EPSILON.;\u03B6;.ZETA.;\u03B7;.ETA.;\u03B8;.THETA.;\u03B9;.IOTA.;\u03BA;.KAPPA.;\u03BB;.LAMBDA.;\u03BC;.MU.;\u03BD;.NU.;\u03BE;.XI.;\u03BF;.OMICRON.;\u03C0;.PI.;\u03C1;.RHO.;\u03C2;.SIGMA.;\u03C3;.SIGMA.;\u03C4;.TAU.;\u03C5;.UPSILON.;\u03C6;.PHI.;\u03C7;.CHI.;\u03C8;.PSI.;\u03C9;.OMEGA.;\u0391;.ALPHA.;\u0392;.BETA.;\u0393;.GAMMA.;\u0394;.DELTA.;\u0395;.EPSILON.;\u0396;.ZETA.;\u0397;.ETA.;\u0398;.THETA.;\u0399;.IOTA.;\u039A;.KAPPA.;\u039B;.LAMBDA.;\u039C;.MU.;\u039D;.NU.;\u039E;.XI.;\u039F;.OMICRON.;\u03A0;.PI.;\u03A1;.RHO.;\u03A3;.SIGMA.;\u03A4;.TAU.;\u03A5;.UPSILON.;\u03A6;.PHI.;\u03A7;.CHI.;\u03A8;.PSI.;\u03A9;.OMEGA.";
    private static final String REPLACEMENT_SOURCE_NUMERIC = "\u2192;->;\\xB1;+/-;±;+/-;\u2190;<-;\\xB2;2;\\xB3;3;\\xB9;1;\u2070;0;\u2071;1;\u2072;2;\u2073;3;\u2074;4;\u2075;5;\u2076;6;\u2077;7;\u2078;8;\u2079;9;\u207A;+;\u207B;-;\u2080;0;\u2081;1;\u2082;2;\u2083;3;\u2084;4;\u2085;5;\u2086;6;\u2087;7;\u2088;8;\u2089;9;\u208A;+;\u208B;-";
    private static final String REPLACEMENT_SOURCE_SMALL_CAPS = "ʟ;L;ᴅ;D";
    private final List<Replacer> replacers = new ArrayList<>();

    public ReplacementResult standardizeMinimally(String input) {
        if( input == null || input.length() == 0) {
            return new ReplacementResult( input, new ArrayList<>());
        }
        ReplacementResult replacementResult = removeSerialSpaces(input);
        replacementResult.update(replaceUnprintables(replacementResult.getResult()));
        return replacementResult;
    }

    public ReplacementResult removeSerialSpaces(String input) {
        Pattern multipleWhiteSpace = Pattern.compile("\\s+");
        List<ReplacementNote> notes = new ArrayList<>();
        ReplacementResult result = new ReplacementResult(input, notes);
        if (input == null || input.length() == 0) {
            return result;
        }
        Matcher matcher = multipleWhiteSpace.matcher(input);
        if (matcher.find()) {
            notes.add(new ReplacementNote(matcher.start(), " "));
            String cleaned =matcher.replaceAll(" ");
            result.setResult(cleaned);
        }
        return result;
    }

    public ReplacementResult fullyStandardizeName(String input) {
        ReplacementResult results = new ReplacementResult(input, new ArrayList<>());
        if (input == null || input.length() == 0) {
            return results;
        }
        ReplacementResult initialResult = replaceUnprintables(input.trim());
        
        ReplacementResult resultForSpecifics = initialResult.update(makeSpecificReplacements(initialResult.getResult()));
        String workingString= resultForSpecifics.getResult();
        workingString = symbolsToASCII(workingString);
        workingString = nkfdNormalizations(workingString);
        results = replaceNonAscii(workingString);
        results.update(removeSerialSpaces(results.getResult()));
        results.setResult(results.getResult().toUpperCase());
        return results;
    }

    public static class Replacer {

        Pattern p;
        String replace;
        String message = "String \"$0\" matches forbidden pattern";
        String postFixMessage = "";

        public Replacer(String regex, String replace) {
            this.p = Pattern.compile(regex);
            this.replace = replace;
        }

        public boolean matches(String test) {

            return this.p.matcher(test).find();
        }

        public String fix(String test, AtomicInteger firstMatch) {
            Matcher m = p.matcher(test);
            if (m.find() && firstMatch != null) {
                firstMatch.set(m.start());
            }
            postFixMessage = getMessage(m.group());
            return test.replaceAll(p.pattern(), replace);
        }

        public Replacer message(String msg) {
            this.message = msg;
            return this;
        }

        public String getMessage(String test) {
            return message.replace("$0", test);
        }

        public String getPostFixMessage() {
            return postFixMessage;
        }
    }

    public static class ReplacementResult {

        private List<ReplacementNote> replacementNotes = new ArrayList<>();
        private String result;

        public ReplacementResult(String result, List<ReplacementNote> notes) {
            this.result = result;
            replacementNotes = notes;
        }

        public void update(String updatedResult, List<ReplacementNote> additionalNotes) {
            this.result=updatedResult;
            this.replacementNotes.addAll(additionalNotes);
        }

        public ReplacementResult update(ReplacementResult newResult) {
            this.result=newResult.getResult();
            this.replacementNotes.addAll(newResult.getReplacementNotes());
            return this;
        }
        
        public List<ReplacementNote> getReplacementNotes() {
            return replacementNotes;
        }

        public void setReplacementNotes(List<ReplacementNote> replacementNotes) {
            this.replacementNotes = replacementNotes;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        @Override
        public String toString() {
            return String.format("result: %s", this.result);
        }
    }

    public static class ReplacementNote {

        private int position;
        private String replacement;

        public ReplacementNote(int position, String replacement) {
            this.position = position;
            this.replacement = replacement;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public String getReplacement() {
            return replacement;
        }

        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }

        @Override
        public String toString() {
            return String.format("position: %d; replacement: %s", position, replacement);
        }
    }

    public static ReplacementResult replaceUnprintables(String source) {
        ReplacementResult defaultResult = new ReplacementResult(source, new ArrayList<>());
        if (source == null || source.length() == 0) {
            return defaultResult;
        }
        Pattern unprintables = Pattern.compile("\\p{C}");
        Matcher matcher = unprintables.matcher(source);
        if( matcher.find()){
            ReplacementNote note1= new ReplacementNote(matcher.start(), source.substring(matcher.start(), matcher.start()+1));
            List<ReplacementNote> notes = new ArrayList<>();
            notes.add(note1);
            return new ReplacementResult(matcher.replaceAll(""), notes);
        }
        return defaultResult;
    }

    private void initReplacers() {
        String[] GreekReplacementTokens = REPLACEMENT_SOURCE_GREEK.split(";");
        for (int i = 0; i < GreekReplacementTokens.length; i = i + 2) {
            replacers.add(new Replacer(GreekReplacementTokens[i], GreekReplacementTokens[i + 1])
                    .message("Replaced Greek character \"$0\" with standard form"));
        }

        String[] numericReplacementTokens = REPLACEMENT_SOURCE_NUMERIC.split(";");
        for (int i = 0; i < numericReplacementTokens.length; i = i + 2) {
            replacers.add(new Replacer(numericReplacementTokens[i], numericReplacementTokens[i + 1])
                    .message("Replaced numeric character \"$0\" with standard form"));
        }

        String[] smallCapsReplacementTokens = REPLACEMENT_SOURCE_SMALL_CAPS.split(";");
        for (int i = 0; i < smallCapsReplacementTokens.length; i = i + 2) {
            replacers.add(new Replacer(smallCapsReplacementTokens[i], smallCapsReplacementTokens[i + 1])
                    .message("Replaced small caps character \"$0\" with standard form"));
        }

    }

    public static String chr(int t) {
        return Character.toString((char) t);
    }

    public static String symbolsToASCII(String input) {
        input = input.replaceAll("[\u00B4\u02B9\u02BC\u02C8\u0301\u2018\u2019\u201B\u2032\u2034\u2037]", chr(39));
        /* apostrophe (') */
        input = input.replaceAll("[\u00AB\u00BB\u02BA\u030B\u030E\u201C\u201D\u201E\u201F\u2033\u2036\u3003\u301D\u301E]", chr(34));
        /* quotation mark (") */
        input = input.replaceAll("[\u00AD\u2010\u2011\u2012\u2013\u2014\u2212\u2015]", chr(45));
        /* hyphen (-) */
        input = input.replaceAll("[\u01C3\u2762]", chr(33));
        /* exclamation mark (!) */
        input = input.replaceAll("[\u266F]", chr(35));
        /* music sharp sign (#) */
        input = input.replaceAll("[\u066A\u2052]", chr(37));
        /* percent sign (%) */
        input = input.replaceAll("[\u066D\u204E\u2217\u2731\u00D7]", chr(42));
        /* asterisk (*) */
        input = input.replaceAll("[\u201A\uFE51\uFF64\u3001]", chr(44));
        /* comma (,) */
        input = input.replaceAll("[\u00F7\u0338\u2044\u2215]", chr(47));
        /* slash (/) */
        input = input.replaceAll("[\u0589\u05C3\u2236]", chr(58));
        /* colon (:) */
        input = input.replaceAll("[\u203D]", chr(63));
        /* question mark (?) */
        input = input.replaceAll("[\u27E6]", chr(91));
        /* opening square bracket ([) */
        input = input.replaceAll("[\u20E5\u2216]", chr(92));
        /* backslash (\) */
        input = input.replaceAll("[\u301B]", chr(93));
        /* closing square bracket ([) */
        input = input.replaceAll("[\u02C4\u02C6\u0302\u2038\u2303]", chr(94));
        /* caret (^) */
        input = input.replaceAll("[\u02CD\u0331\u0332\u2017]", chr(95));
        /* underscore (_) */
        input = input.replaceAll("[\u02CB\u0300\u2035]", chr(96));
        /* grave accent (`) */

        input = input.replaceAll("[\u2983]", chr(123));
        /* opening curly bracket ({) */

        input = input.replaceAll("[\u01C0\u05C0\u2223\u2758]", chr(124));
        /* vertical bar / pipe (|) */

        input = input.replaceAll("[\u2016]", "#chr(124)##chr(124)#");
        /* double vertical bar / double pipe (||) */

        input = input.replaceAll("[\u02DC\u0303\u2053\u223C\u301C]", chr(126));
        /* tilde (~) */

        input = input.replaceAll("[\u2039\u2329\u27E8\u3008]", chr(60));
        /* less-than sign (<) */

        input = input.replaceAll("[\u2264\u2266]", "#chr(60)##chr(61)#");
        /* less-than equal-to sign (<=) */

        input = input.replaceAll("[\u203A\u232A\u27E9\u3009]", chr(62));
        /* greater-than sign (>) */

        input = input.replaceAll("[\u2265\u2267]", "#chr(62)##chr(61)#");
        /* greater-than equal-to sign (>=) */

        input = input.replaceAll("[\u200B\u2060\uFEFF]", chr(32));
        /* space ( ) */
        input = input.replaceAll("\u2153", "1/3");
        input = input.replaceAll("\u2154", "2/3");
        input = input.replaceAll("\u2155", "1/5");
        input = input.replaceAll("\u2156", "2/5");
        input = input.replaceAll("\u2157", "3/5");
        input = input.replaceAll("\u2158", "4/5");
        input = input.replaceAll("\u2159", "1/6");
        input = input.replaceAll("\u215A", "5/6");
        input = input.replaceAll("\u215B", "1/8");
        input = input.replaceAll("\u215C", "3/8");
        input = input.replaceAll("\u215D", "5/8");
        input = input.replaceAll("\u215E", "7/8");
        input = input.replaceAll("\u2026", "...");
        input = input.replaceAll("\u00BC", "1/4");
        input = input.replaceAll("\u00BD", "1/2");
        input = input.replaceAll("⅔", "2/3");

        return input;
    }

    public ReplacementResult makeSpecificReplacements(String inputString) {
        List<ReplacementNote> messages = new ArrayList<>();
        String value = inputString;
        AtomicInteger firstMatchPosition = new AtomicInteger();
        for (Replacer r : replacers) {
            if (r.matches(value)) {
                value = r.fix(value, firstMatchPosition);
                messages.add(new ReplacementNote(firstMatchPosition.get(), r.getPostFixMessage()));
            }
        }
        ReplacementResult result = new ReplacementResult(value, messages);
        return result;
    }

    public String nkfdNormalizations(String inputString) {
        //String entered = "𝐸₃éé👍!";
        String entered = inputString;

        log.trace(entered);
        log.trace("Length:" + entered.length());

        //output: 𝐸₃éé👍!
        //Length:8
        String normalized = Normalizer
                .normalize(entered, Normalizer.Form.NFKD);

        log.trace(normalized);
        log.trace("Length:" + normalized.length());

        //output: E3éé👍!
        //Length:9
        String removedCombined = normalized.replaceAll("\\p{Mn}+", "");
        log.trace(removedCombined);
        log.trace("Length:" + removedCombined.length());

        //E3ee👍!
        //Length:7
        //TODO: if a character is found, store it somewhere for possible
        //warning of removal
        String removeAllOtherUnknowns = removedCombined
                .replaceAll("[^\\p{ASCII}]", "?");

        log.trace(removeAllOtherUnknowns);

        log.trace("Length:" + removeAllOtherUnknowns.length());
        return removeAllOtherUnknowns;
    }

    //todo: add notes about characters replaced
    public ReplacementResult replaceNonAscii(String input) {

        List<ReplacementNote> notes = new ArrayList();
        ReplacementResult replacementResult = new ReplacementResult(input, notes);
        if (input == null || input.length() == 0) {
            return replacementResult;
        }
        Pattern nonAscii = Pattern.compile("[^\\p{ASCII}]");
        String NON_ASCII_REPLACEMENT = "?";
        Matcher matcher = nonAscii.matcher(input);
        if (!matcher.find()) {
            return replacementResult;
        }
        StringBuffer sb = new StringBuffer();
        matcher = nonAscii.matcher(input);
        while (matcher.find()) {
            String charToEval = matcher.group();
            int width = getCharacterWidth(charToEval.charAt(0));
            String replacement = "";
            if (width > 0) {
                notes.add(new ReplacementNote(matcher.start(), input.substring(matcher.start(), matcher.start() + 1)));
                replacement = NON_ASCII_REPLACEMENT;
            }
            matcher.appendReplacement(sb, replacement);
        }
        log.trace("before appendTail: '" + sb.toString() + "'");
        matcher.appendTail(sb);
        replacementResult.setResult(sb.toString());
        return replacementResult;
    }

    private final String commonFontName = "Arial";

    private int getCharacterWidth(char c) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] allFonts = ge.getAllFonts();

        Toolkit toolkit = Toolkit.getDefaultToolkit();

        int width = 0;
        boolean foundFont = false;
        for (Font font : allFonts) {
            FontMetrics fontMetrics = toolkit.getFontMetrics(font);
            //log.trace(font.getFontName());
            //log.trace(fontMetrics.charWidth(c));
            width = fontMetrics.charWidth(c);
            if (font.getFontName().startsWith(commonFontName)) {
                foundFont = true;
                break;
            }
        }
        if (!foundFont) {
            FontMetrics fontMetrics = toolkit.getFontMetrics(allFonts[0]);
            width = fontMetrics.charWidth(c);
            log.trace("warning: using first font (" + allFonts[0].getFontName() + ")");
        }
        return width;
    }

}
