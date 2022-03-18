package gsrs.module.substance.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import gsrs.module.substance.services.ConsoleFilterService;
import ix.ginas.utils.validation.validators.tags.TagUtilities;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author mitch
 */
@Slf4j
public class NameUtilities {

    private static final NameUtilities INSTANCE = new NameUtilities();

    private NameUtilities() {
        initReplacers();
    }

    public static NameUtilities getInstance() {
        return INSTANCE;
    }

    private static final String REPLACEMENT_SOURCE_GREEK = "\u03B1;.ALPHA.;\u03B2;.BETA.;\u03B3;.GAMMA.;\u03B4;.DELTA.;\u03B5;.EPSILON.;\u03B6;.ZETA.;\u03B7;.ETA.;\u03B8;.THETA.;\u03B9;.IOTA.;\u03BA;.KAPPA.;\u03BB;.LAMBDA.;\u03BC;.MU.;\u03BD;.NU.;\u03BE;.XI.;\u03BF;.OMICRON.;\u03C0;.PI.;\u03C1;.RHO.;\u03C2;.SIGMA.;\u03C3;.SIGMA.;\u03C4;.TAU.;\u03C5;.UPSILON.;\u03C6;.PHI.;\u03C7;.CHI.;\u03C8;.PSI.;\u03C9;.OMEGA.;\u0391;.ALPHA.;\u0392;.BETA.;\u0393;.GAMMA.;\u0394;.DELTA.;\u0395;.EPSILON.;\u0396;.ZETA.;\u0397;.ETA.;\u0398;.THETA.;\u0399;.IOTA.;\u039A;.KAPPA.;\u039B;.LAMBDA.;\u039C;.MU.;\u039D;.NU.;\u039E;.XI.;\u039F;.OMICRON.;\u03A0;.PI.;\u03A1;.RHO.;\u03A3;.SIGMA.;\u03A4;.TAU.;\u03A5;.UPSILON.;\u03A6;.PHI.;\u03A7;.CHI.;\u03A8;.PSI.;\u03A9;.OMEGA.";
    private static final String REPLACEMENT_SOURCE_NUMERIC = "\u2192;->;\\xB1;+/-;±;+/-;\u2190;<-;\\xB2;2;\\xB3;3;\\xB9;1;\u2070;0;\u2071;1;\u2072;2;\u2073;3;\u2074;4;\u2075;5;\u2076;6;\u2077;7;\u2078;8;\u2079;9;\u207A;+;\u207B;-;\u2080;0;\u2081;1;\u2082;2;\u2083;3;\u2084;4;\u2085;5;\u2086;6;\u2087;7;\u2088;8;\u2089;9;\u208A;+;\u208B;-";
    private static final String REPLACEMENT_SOURCE_SMALL_CAPS = "ʟ;L;ᴅ;D";
    private final List<Replacer> replacers = new ArrayList<>();
    //private static final Pattern NON_ASCII_PATTERN = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern UNPRINTABLES_PATTERN = Pattern.compile("\\p{C}");
    //private static final String NON_ASCII_REPLACEMENT = "?";
    private static final Pattern PATTERN_MULTIPLE_WHITE_SPACE = Pattern.compile("\\s{2,}");
    private static final Pattern PATTERN_ZERO_WIDTH = Pattern.compile("[\u200B\u200C\u200D\u2060\uFEFF]");
    
    private static final Pattern PATTERN_CASE0 = Pattern.compile("[\u00B4\u02B9\u02BC\u02C8\u0301\u2018\u2019\u201B\u2032\u2034\u2037]");
    private static final Pattern PATTERN_CASE1 = Pattern.compile("[\u00AB\u00BB\u02BA\u030B\u030E\u201C\u201D\u201E\u201F\u2033\u2036\u3003\u301D\u301E]");
    private static final Pattern PATTERN_CASE2 = Pattern.compile("[\u00AD\u2010\u2011\u2012\u2013\u2014\u2212\u2015]");
    private static final Pattern PATTERN_CASE3 = Pattern.compile("[\u01C3\u2762]");
    private static final Pattern PATTERN_CASE4 = Pattern.compile("[\u266F]");
    private static final Pattern PATTERN_CASE5 = Pattern.compile("[\u066A\u2052]");
    private static final Pattern PATTERN_CASE6 = Pattern.compile("[\u066D\u204E\u2217\u2731\u00D7]");
    private static final Pattern PATTERN_CASE7 = Pattern.compile("[\u201A\uFE51\uFF64\u3001]");
    private static final Pattern PATTERN_CASE8 = Pattern.compile("[\u00F7\u0338\u2044\u2215]");
    private static final Pattern PATTERN_CASE9 = Pattern.compile("[\u0589\u05C3\u2236]");
    private static final Pattern PATTERN_CASE10 = Pattern.compile("[\u203D]");
    private static final Pattern PATTERN_CASE11 = Pattern.compile("[\u27E6]");
    private static final Pattern PATTERN_CASE12 = Pattern.compile("[\u20E5\u2216]");
    private static final Pattern PATTERN_CASE13 = Pattern.compile("[\u301B]");
    private static final Pattern PATTERN_CASE14 = Pattern.compile("[\u02C4\u02C6\u0302\u2038\u2303]");
    private static final Pattern PATTERN_CASE15 = Pattern.compile("[\u02CD\u0331\u0332\u2017]");
    private static final Pattern PATTERN_CASE16 = Pattern.compile("[\u02CB\u0300\u2035]");
    private static final Pattern PATTERN_CASE17 = Pattern.compile("[\u2983]");
    private static final Pattern PATTERN_CASE18 = Pattern.compile("[\u01C0\u05C0\u2223\u2758]");
    private static final Pattern PATTERN_CASE19 = Pattern.compile("[\u2016]");
    private static final Pattern PATTERN_CASE20 = Pattern.compile("[\u02DC\u0303\u2053\u223C\u301C]");
    private static final Pattern PATTERN_CASE21 = Pattern.compile("[\u2039\u2329\u27E8\u3008]");
    private static final Pattern PATTERN_CASE22 = Pattern.compile("[\u2264\u2266]");
    private static final Pattern PATTERN_CASE23 = Pattern.compile("[\u203A\u232A\u27E9\u3009]");
    private static final Pattern PATTERN_CASE24 = Pattern.compile("[\u2265\u2267]");
    private static final Pattern PATTERN_CASE25 = Pattern.compile("[\uFEFF]");
    private static final Pattern PATTERN_CASE26 = Pattern.compile("\u2153");
    private static final Pattern PATTERN_CASE27 = Pattern.compile("\u2154");
    private static final Pattern PATTERN_CASE28 = Pattern.compile("\u2155");
    private static final Pattern PATTERN_CASE29 = Pattern.compile("\u2156");
    private static final Pattern PATTERN_CASE30 = Pattern.compile("\u2157");
    private static final Pattern PATTERN_CASE31 = Pattern.compile("\u2158");
    private static final Pattern PATTERN_CASE32 = Pattern.compile("\u2159");
    private static final Pattern PATTERN_CASE33 = Pattern.compile("\u215A");
    private static final Pattern PATTERN_CASE34 = Pattern.compile("\u215B");
    private static final Pattern PATTERN_CASE35 = Pattern.compile("\u215C");
    private static final Pattern PATTERN_CASE36 = Pattern.compile("\u215D");
    private static final Pattern PATTERN_CASE37 = Pattern.compile("\u215E");
    private static final Pattern PATTERN_CASE38 = Pattern.compile("\u2026");
    private static final Pattern PATTERN_CASE39 = Pattern.compile("\u00BC");
    private static final Pattern PATTERN_CASE40 = Pattern.compile("\u00BD");
    private static final Pattern PATTERN_CASE41 = Pattern.compile("﹘");
    
    
    public ReplacementResult standardizeMinimally(String input) {
        if (input == null || input.length() == 0) {
            return new ReplacementResult(input, new ArrayList<>());
        }
        ReplacementResult replacementResult = removeSerialSpaces(input);
        replacementResult.update(replaceUnprintables(replacementResult.getResult()));
        return replacementResult;
    }

    public ReplacementResult removeSerialSpaces(String input) {

        List<ReplacementNote> notes = new ArrayList<>();
        ReplacementResult result = new ReplacementResult(input, notes);
        if (input == null || input.length() == 0) {
            return result;
        }
        
        Matcher matcher = PATTERN_MULTIPLE_WHITE_SPACE.matcher(input);
        if (matcher.find()) {
            notes.add(new ReplacementNote(matcher.start(), " "));
            String cleaned = matcher.replaceAll(" ");
            result.setResult(cleaned);
        }
        return result;
    }

    /**
     * modify a text string so that it contains only standard ASCII characters.
     * This method is designed for chemical names.
     *
     * @param input text data
     * @return text data + messages about some of the replacements
     */
    public ReplacementResult fullyStandardizeName(String input) {
        ReplacementResult results = new ReplacementResult(input, new ArrayList<>());
        if (input == null || input.length() == 0) {
            return results;
        }
        TagUtilities.BracketExtraction extract= TagUtilities.getBracketExtraction(input.trim());
        String namePart=extract.getNamePart();
        String suffix = extract.getTagTerms().stream().map(f->"[" + f + "]").collect(Collectors.joining(""));
        if(suffix.length()>0){
            suffix=" " + suffix;
        }
        if(namePart==null) {
            namePart=input.trim();
        }
        ReplacementResult initialResult = replaceUnprintables(namePart);

        ReplacementResult resultForSpecifics = initialResult.update(makeSpecificReplacements(initialResult.getResult()));
        String workingString = resultForSpecifics.getResult();
        workingString = symbolsToASCII(workingString);
        ReplacementResult zeroWidthRemovalResult = removeZeroWidthChars(workingString);
        results.update(zeroWidthRemovalResult);
        workingString = nkfdNormalizations(results.getResult());
        results.update(removeSerialSpaces(workingString));
        results.setResult(results.getResult().toUpperCase() +suffix);
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
            if(test==null) {
                return false;
            }
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

    /**
     * Represents the output of a text transformation. The result field contains
     * the transformed text. The ReplacementNotes contain information about the
     * specific replacements performed.
     */
    public static class ReplacementResult {

        private List<ReplacementNote> replacementNotes = new ArrayList<>();
        private String result;

        public ReplacementResult(String result, List<ReplacementNote> notes) {
            this.result = result;
            replacementNotes = notes;
        }

        public void update(String updatedResult, List<ReplacementNote> additionalNotes) {
            this.result = updatedResult;
            this.replacementNotes.addAll(additionalNotes);
        }

        public ReplacementResult update(ReplacementResult newResult) {
            this.result = newResult.getResult();
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

    /**
     * *
     * A message about a text modification. position is the starting point in
     * the original string where a character to be removed occurred replacement
     * is the character that was replaced
     */
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

    /**
     * Replace a series of specific characters that cannot be rendered in print
     *
     * @param source starting text
     * @return clean text with some messages
     */
    public static ReplacementResult replaceUnprintables(String source) {
        ReplacementResult defaultResult = new ReplacementResult(source, new ArrayList<>());
        if (source == null || source.length() == 0) {
            return defaultResult;
        }

        Matcher matcher = UNPRINTABLES_PATTERN.matcher(source);
        if (matcher.find()) {
            ReplacementNote note1 = new ReplacementNote(matcher.start(), source.substring(matcher.start(), matcher.start() + 1));
            List<ReplacementNote> notes = new ArrayList<>();
            notes.add(note1);
            return new ReplacementResult(matcher.replaceAll(""), notes);
        }
        return defaultResult;
    }

    private void initReplacers() {
        String[] replacementTokensGreek = REPLACEMENT_SOURCE_GREEK.split(";");
        for (int i = 0; i < replacementTokensGreek.length; i = i + 2) {
            replacers.add(new Replacer(replacementTokensGreek[i], replacementTokensGreek[i + 1])
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

        replacers.add(new Replacer("[\\[\\{]","("));
        replacers.add(new Replacer("[\\]\\}]",")"));
    }

    private static String chr(int t) {
        return Character.toString((char) t);
    }

    /**
     * replaces a series of non-ASCII characters with ASCII characters that are
     * functionally equivalent
     *
     * @param input text that may contain non-ASCII characters
     * @return text without non-ASCII characters
     */
    public static String symbolsToASCII(String input) {

        input = PATTERN_CASE0.matcher(input).replaceAll(chr(39));
        /* apostrophe (') */
        input = PATTERN_CASE1.matcher(input).replaceAll(chr(34));
        /* quotation mark (") */
        input = PATTERN_CASE2.matcher(input).replaceAll(chr(45));
        /* hyphen (-) */
        input = PATTERN_CASE3.matcher(input).replaceAll(chr(33));
        /* exclamation mark (!) */
        input = PATTERN_CASE4.matcher(input).replaceAll(chr(35));
        /* music sharp sign (#) */
        input = PATTERN_CASE5.matcher(input).replaceAll(chr(37));
        /* percent sign (%) */
        input = PATTERN_CASE6.matcher(input).replaceAll(chr(42));
        /* asterisk (*) */
        input = PATTERN_CASE7.matcher(input).replaceAll(chr(44));
        /* comma (,) */
        input = PATTERN_CASE8.matcher(input).replaceAll(chr(47));
        /* slash (/) */
        input = PATTERN_CASE9.matcher(input).replaceAll(chr(58));
        /* colon (:) */
        input = PATTERN_CASE10.matcher(input).replaceAll(chr(63));
        /* question mark (?) */
        input = PATTERN_CASE11.matcher(input).replaceAll(chr(91));
        /* opening square bracket ([) */
        input = PATTERN_CASE12.matcher(input).replaceAll(chr(92));
        /* backslash (\) */
        input = PATTERN_CASE13.matcher(input).replaceAll(chr(93));
        /* closing square bracket ([) */
        input = PATTERN_CASE14.matcher(input).replaceAll(chr(94));
        /* caret (^) */
        input = PATTERN_CASE15.matcher(input).replaceAll(chr(95));
        /* underscore (_) */
        input = PATTERN_CASE16.matcher(input).replaceAll(chr(96));
        /* grave accent (`) */

        input = PATTERN_CASE17.matcher(input).replaceAll(chr(123));
        /* opening curly bracket ({) */

        input = PATTERN_CASE18.matcher(input).replaceAll(chr(124));
        /* vertical bar / pipe (|) */

        input = PATTERN_CASE19.matcher(input).replaceAll(chr(124) + chr(124));
        /* double vertical bar / double pipe (||) */

        input = PATTERN_CASE20.matcher(input).replaceAll(chr(126));
        /* tilde (~) */

        input = PATTERN_CASE21.matcher(input).replaceAll(chr(60));
        /* less-than sign (<) */

        input = PATTERN_CASE22.matcher(input).replaceAll("<=");
        /* less-than equal-to sign (<=) */

        input = PATTERN_CASE23.matcher(input).replaceAll(chr(62));
        /* greater-than sign (>) */

        input = PATTERN_CASE24.matcher(input).replaceAll(">=");

        /* greater-than equal-to sign (>=) */

        input = PATTERN_CASE25.matcher(input).replaceAll(chr(32)); //removed \u200B\u2060
        /* space ( ) */
        input = PATTERN_CASE26.matcher(input).replaceAll("1/3");
        input = PATTERN_CASE27.matcher(input).replaceAll("2/3");
        input = PATTERN_CASE28.matcher(input).replaceAll("1/5");
        input = PATTERN_CASE29.matcher(input).replaceAll("2/5");
        input = PATTERN_CASE30.matcher(input).replaceAll("3/5");
        input = PATTERN_CASE31.matcher(input).replaceAll("4/5");
        input = PATTERN_CASE32.matcher(input).replaceAll("1/6");
        input = PATTERN_CASE33.matcher(input).replaceAll("5/6");
        input = PATTERN_CASE34.matcher(input).replaceAll("1/8");
        input = PATTERN_CASE35.matcher(input).replaceAll("3/8");
        input = PATTERN_CASE36.matcher(input).replaceAll("5/8");
        input = PATTERN_CASE37.matcher(input).replaceAll("7/8");
        input = PATTERN_CASE38.matcher(input).replaceAll("...");
        input = PATTERN_CASE39.matcher(input).replaceAll("1/4");
        input = PATTERN_CASE40.matcher(input).replaceAll("1/2");
        input = PATTERN_CASE41.matcher(input).replaceAll("-");
        


        //input = input.replaceAll("⅔", "2/3");
        //input = input.replaceAll("\u2154", "2/3");

        return input;
    }

    /**
     * Remove a series of specific characters, inserting different characters in
     * their place
     *
     * @param inputString starting text
     * @return 'clean' text + a set of messages about the specific characters
     * removed
     */
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

    /**
     * Remove a series of glyph characters
     *
     * @param inputString starting text
     * @return 'clean' text
     */
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

    /**
     * Delete characters that have no screen width
     *
     * @param input starting text
     * @return 'clean' text
     */
    //todo: add notes about characters replaced
    public ReplacementResult removeZeroWidthChars(String input) {

        List<ReplacementNote> notes = new ArrayList();
        ReplacementResult replacementResult = new ReplacementResult(input, notes);
        if (input == null || input.length() == 0) {
            return replacementResult;
        }

        Matcher matcher = PATTERN_ZERO_WIDTH.matcher(input);
        String clean = matcher.replaceAll("");
        replacementResult.setResult(clean);
        return replacementResult;
    }

    public boolean nameHasUnacceptableChar(String name) {
        Pattern asciiPattern = Pattern.compile("\\A\\p{ASCII}*\\z");
        Matcher asciiMatcher = asciiPattern.matcher(name);
        Pattern lowerCasePattern = Pattern.compile("[a-z]+");
        Matcher lowerCaseMatcher = lowerCasePattern.matcher(name);
        List<Character> dirtyChars = Arrays.asList('\t', '\r', '\n', '`', '{', '}');

        if (name == null || name.length() == 0) {
            return false;
        }

        if (lowerCaseMatcher.find()) {
            return true;
        }

        if ((!name.trim().equals(name))) {
            return true;
        }

        if (!asciiMatcher.find()) {
            return true;
        }
        for (char testChar : name.toCharArray()) {
            if (dirtyChars.contains(testChar)) {
                return true;
            }
        }

        //square brackets are OK when they provide a name qualification, as int 'NAME [ORGANIZATION]'
        // but not OK otherwise, as in '[1,4]DICHLOROBENZENE' or 'NAME[SOMETHING]' (without a space before '[')
        int initBracketPos = name.indexOf("[");
        int closeBracketPos = name.indexOf("]");
        return initBracketPos > 0
                && ((name.charAt(initBracketPos - 1) != ' ')
                || (closeBracketPos < (name.length() - 1)));
    }

}
