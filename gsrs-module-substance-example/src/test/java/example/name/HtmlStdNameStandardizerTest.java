package example.name;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import gsrs.module.substance.standardizer.NameStandardizer;
import gsrs.module.substance.standardizer.NameStandardizerConfiguration;
import gsrs.module.substance.standardizer.ReplacementNote;
import gsrs.module.substance.standardizer.ReplacementResult;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Egor Puzanov
 */
@Slf4j
public class HtmlStdNameStandardizerTest {

    NameStandardizer standardizer = getStandardizer();

    private NameStandardizer getStandardizer() {
        try {
            return (new NameStandardizerConfiguration()).stdNameStandardizer();
        } catch (Exception ex) {
            return null;
        }
    }

    @Test //Remove a tab
    public void testStandardize16() {
        String input = "A '\t' name";
        String expected = "A '' NAME";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testDashes() {
        String input ="‒1‐2‑3–4—5﹘6﹣7－8-9";
        String expected = "-1-2-3-4-5-6-7-8-9";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test //Remove a newline
    public void testStandardize17() {
        String input = "A '\n' name";
        String expected = "A '' NAME";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test //Remove a carriage return
    public void testStandardize18() {
        String input = "A name with '\r'";
        String expected = "A NAME WITH ''";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test //replace quote-like char    
    public void testQuoteLike1() {
        String input = "\u00B4";
        String expected = "'";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMultipleQuoteLike() {
        String input = "\u00B4\u02B9\u02BC\u02C8\u0301\u2018\u2019\u201B\u2032\u2034\u2037";
        String expected = "'''''''''''";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMultipleHyphenLike() {
        String input = "|\u00AD|\u2010|\u2011|\u2012|\u2013|\u2014|\u2212|\u2015|";
        String expected = "||-|-|-|-|-|-|-|";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual); //, "Because of removing Unprintable characters \u00AD first"
    }

    @Test
    public void testMultipleDoubleQuoteLike() {
        String input = "\u00AB\u00BB\u02BA\u030B\u030E\u201C\u201D\u201E\u201F\u2033\u2036\u3003\u301D\u301E";
        String expected = "\"\"\"\"\"\"\"\"\"\"\"\"\"\"";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testSharpLike() {
        String input = "♯";
        String expected = "#";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMultiplePercentLike() {
        String input = "٪a⁒";
        String expected = "%A%";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testOneHalfLike() {
        String input = "½";
        String expected = "1/2";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testOneFourthLike() {
        String input = "¼";
        String expected = "1/4";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testUnprintables() {
        for (int i = 0; i <= 31; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append((char) i);
            String input = sb.toString();
            String r = standardizer.standardize(input).getResult();
            Assertions.assertEquals("", r);
        }
        //one more
        StringBuilder sb = new StringBuilder();
        sb.append((char) 0x7f);
        String input = sb.toString();
        String r = standardizer.standardize(input).getResult();
        Assertions.assertEquals("", r);
    }

    @Test
    public void testTwoThirds() {
        String input = "⅔";
        String expected = "2/3";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGreekLetterReplacements1() {
        String input = " Α, Β, Γ, Δ, Ε, Ζ, Η, Θ, Ι, Κ, Λ, Μ, Ν, Ξ, Ο, Π, Ρ, Σ, Τ, Υ, Φ, Χ, Ψ, and Ω";
        String expected = ".ALPHA., .BETA., .GAMMA., .DELTA., .EPSILON., .ZETA., .ETA., .THETA., .IOTA., .KAPPA., .LAMBDA., .MU., .NU., .XI., .OMICRON., .PI., .RHO., .SIGMA., .TAU., .UPSILON., .PHI., .CHI., .PSI., AND .OMEGA.";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testHtmlTagsReplacements1() {
        String input = "H<sub>2</sub>O";
        String expected = "HSUB(2)O";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testHtmlTagsReplacements2() {
        String input = "<i>Italic</i> Text";
        String expected = "ITALIC TEXT";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNumericReplacements1() {
        String input = "¹±³";
        String expected = "1+/-3";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    /*
    This unit test fails within IntelliJ but passes at the command line
     */
    @Test
    public void testNkfdNormalization() {
        String input = "𝐸₃éé👍!";
        String expected = "?3EE?!";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual); //, "Because of removing Unprintable characters first"
    }

    @Test
    public void testSmallCaps() {
        String input = "ᴅ glucose";
        String expected = "D GLUCOSE";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual); //, "Must replace small caps characters"
    }

    @Test
    public void testDoublePipeReplace() {
        String input = "\u2016";
        String expected = "||";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual); //, "Must replace double pipe char"
    }

    @Test
    public void test2SmallCaps() {
        String input = "ʟᴅ glucose";
        String expected = "LD GLUCOSE";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual); //, "Must replace small caps characters"
    }

    @Test
    public void testNonAsciiRemoval() {
        String input = "a milligram of glucose©";
        String expected = "A MILLIGRAM OF GLUCOSE?";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNonAsciiRemoval2() {
        String input = "some idea\u200B";
        String expected = "SOME IDEA";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMoreZeroWidthRemoval() {
        String input = "an little\u200B\u200C\u200D sugar";
        String expected = "AN LITTLE SUGAR";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testFull() {
        String input = "a little\u200Bbit of glucose©";
        String expected = "A LITTLEBIT OF GLUCOSE?";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testFull2() {
        String input = " Α, Β, Γ \u200C,an little\u200Cbit of glucose©";
        String expected = ".ALPHA., .BETA., .GAMMA. ,AN LITTLEBIT OF GLUCOSE?";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testFull3() {
        String input = " Α, Β, Γ \u200C,  potassium diclofenac";
        String expected = ".ALPHA., .BETA., .GAMMA. , POTASSIUM DICLOFENAC";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMiscNonAscii() {
        //several chars from https://terpconnect.umd.edu/~zben/Web/CharSet/htmlchars.html
        String input = "pay attention\u00A1 There is \u00A4 to be made. No, \u00AB isn't XML. \u03A7 \u2191 ";
        String expected = "PAY ATTENTION? THERE IS ? TO BE MADE. NO, \" ISN'T XML. .CHI. ?";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testReplaceSerialWhitespace1() {
        String input = "sodium  chloride";
        String expected = "SODIUM CHLORIDE";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testReplaceSerialWhitespace2() {
        String input = "potassium       bromide";
        String expected = "POTASSIUM BROMIDE";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testReplaceSerialWhitespace3() {
        String input = "potassium   bromide";
        String expected = "POTASSIUM BROMIDE";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testReplaceSerialWhitespace4() {
        String input = "sodium chloride";
        String expected = "SODIUM CHLORIDE";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testReplacementResultUpdate() {
        String input = "chemical name";
        List<ReplacementNote> notes = new ArrayList<>();

        ReplacementResult result1;
        result1 = new ReplacementResult(input, notes);

        ReplacementNote note1 = new ReplacementNote(1, "hello");
        List<ReplacementNote> notes2 = new ArrayList<>();
        notes2.add(note1);
        String updatedName = "potassium permanganate";
        result1.update(updatedName, notes2);

        Assertions.assertEquals(updatedName, result1.getResult());
        Assertions.assertEquals(note1.getReplacement(), result1.getReplacementNotes().get(0).getReplacement());
    }

    @Test
    public void testAccentedLetters() {
        String input ="prêt-à-porter";
        String expected = "PRET-A-PORTER";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testHyphenoids(){
        String input = "\u00AD\u2010\u2011\u2012something else\u2013\u2014\u2212\u2015";
        String expected ="---SOMETHING ELSE----";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual); //, "Because of removing Unprintable characters \u00AD first"
    }

    @Test
    public void testSerialSpaceRemovalForCommas() {
        String input = ".ALPHA., .BETA., .GAMMA.";
        String expected = ".ALPHA., .BETA., .GAMMA.";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void TestfullyStandardizeNameWithEarlyBrackets() {
        String input = "[HELLO] THERE";
        String expected = "(HELLO) THERE";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void TestfullyStandardizeNameWithEarlyBracketsAndBraces() {
        String input = "[user] {friendly} software";
        String expected = "(USER) (FRIENDLY) SOFTWARE";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNameCleanup() {
        String input="[HI]THERE [INN][USAN]";
        String expected ="(HI)THERE [INN][USAN]";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNameX1() {
        String input ="species 1 × species 2";
        String expected = "SPECIES 1 X SPECIES 2";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testHtmlCodes() {
        String input ="2>1 & 4<7 &quot;&zwnj;';'&zwnj;&quot;";
        String expected = "2>1 & 4<7 \"';'\"";
        String actual = standardizer.standardize(input).getResult();
        Assertions.assertEquals(expected, actual);
    }
}
