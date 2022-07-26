package example.name;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import gsrs.module.substance.utils.FDAFullNameStandardizer;
import gsrs.module.substance.utils.NameStandardizer;
import gsrs.module.substance.utils.NameUtilities;
import gsrs.module.substance.utils.ReplacementNote;
import gsrs.module.substance.utils.ReplacementResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mitch
 */
@Slf4j
public class NameUtilitiesTest {

    @Autowired
    NameUtilities nameUtilities;
    
    @Test //Remove a tab
    public void testStandardize16() {
        String inputName = "A '\t' name";
        String expected = "A '' name";
        String actual = NameUtilities.replaceUnprintables(inputName).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testDashes() {
        String input ="‒1‐2‑3–4—5﹘6﹣7－8-9";
        String expected = "-1-2-3-4-5-6-7-8-9";
        ReplacementResult result =  nameUtilities.fullyStandardizeName(input);
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual);
    }
    
    @Test //Remove a newline
    public void testStandardize17() {
        String inputName = "A '\n' name";
        String expected = "A '' name";
        String actual = NameUtilities.replaceUnprintables(inputName).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test //Remove a carriage return
    public void testStandardize18() {
        String inputName = "A name with '\r'";
        String expected = "A name with ''";
        String actual = NameUtilities.replaceUnprintables(inputName).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMinimalStandardization() {
        String inputName = "Substance name   2 \u0005derivative";
        String expected = "Substance name 2 derivative";
        Assertions.assertEquals(expected, nameUtilities.standardizeMinimally(inputName).getResult());
    }

    @Test
    public void testMinimalStandardization2() {
        String inputName = "Chemical \u0005\u0004material";
        String expected = "Chemical material";
        Assertions.assertEquals(expected, nameUtilities.standardizeMinimally(inputName).getResult());
    }

    @Test
    public void testMinimalStandardization3() {
        String inputName = "alpha-linolenic acid";
        String expected = "alpha-linolenic acid";
        Assertions.assertEquals(expected, nameUtilities.standardizeMinimally(inputName).getResult());
    }

    @Test //replace quote-like char    
    public void testQuoteLike1() {

        String inputName = "\u00B4";
        String expected = "'";
        String actual = NameUtilities.symbolsToASCII(inputName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMultipleQuoteLike() {
        String inputName = "\u00B4\u02B9\u02BC\u02C8\u0301\u2018\u2019\u201B\u2032\u2034\u2037";
        String expected = "'''''''''''";
        String actual = NameUtilities.symbolsToASCII(inputName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMultipleHyphenLike() {
        String inputName = "\u00AD\u2010\u2011\u2012\u2013\u2014\u2212\u2015";
        String expected = "--------";
        String actual = NameUtilities.symbolsToASCII(inputName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMultipleDoubleQuoteLike() {
        String inputName = "\u00AB\u00BB\u02BA\u030B\u030E\u201C\u201D\u201E\u201F\u2033\u2036\u3003\u301D\u301E";
        String expected = "\"\"\"\"\"\"\"\"\"\"\"\"\"\"";
        String actual = NameUtilities.symbolsToASCII(inputName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testSharpLike() {
        String inputName = "♯";
        String expected = "#";
        String actual = NameUtilities.symbolsToASCII(inputName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMultiplePercentLike() {
        String inputName = "٪a⁒";
        String expected = "%a%";
        String actual = NameUtilities.symbolsToASCII(inputName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testOneHalfLike() {
        String inputName = "½";
        String expected = "1/2";
        String actual = NameUtilities.symbolsToASCII(inputName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testOneFourthLike() {
        String inputName = "¼";
        String expected = "1/4";
        String actual = NameUtilities.symbolsToASCII(inputName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testUnprintables() {
        for (int i = 0; i <= 31; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append((char) i);
            String input = sb.toString();
            ReplacementResult r = NameUtilities.replaceUnprintables(input);
            Assertions.assertEquals("", r.getResult());
        }
        //one more
        StringBuilder sb = new StringBuilder();
        sb.append((char) 0x7f);
        String input = sb.toString();
        ReplacementResult r = NameUtilities.replaceUnprintables(input);
        Assertions.assertEquals("", r.getResult());
    }

    @Test
    public void testTwoThirds() {
        String inputName = "⅔";
        String expected = "2/3";
        String actual = NameUtilities.symbolsToASCII(inputName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGreekLetterReplacements1() {
        String input = " Α, Β, Γ, Δ, Ε, Ζ, Η, Θ, Ι, Κ, Λ, Μ, Ν, Ξ, Ο, Π, Ρ, Σ, Τ, Υ, Φ, Χ, Ψ, and Ω";
        String expected = " .ALPHA., .BETA., .GAMMA., .DELTA., .EPSILON., .ZETA., .ETA., .THETA., .IOTA., .KAPPA., .LAMBDA., .MU., .NU., .XI., .OMICRON., .PI., .RHO., .SIGMA., .TAU., .UPSILON., .PHI., .CHI., .PSI., and .OMEGA.";
        ReplacementResult result = nameUtilities.makeSpecificReplacements(input);
//        result.getReplacementNotes().forEach(n -> {
//            System.out.println(n);
//        });
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNumericReplacements1() {
        String input = "¹±³";
        String expected = "1+/-3";
        ReplacementResult result = nameUtilities.makeSpecificReplacements(input);
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNkfdNormalization() {
        String input = "𝐸₃éé👍!";
        String expected = "E3ee?!";
        String actual = nameUtilities.nkfdNormalizations(input);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testSmallCaps() {
        String input = "ᴅ glucose";
        String expected = "D glucose";
        ReplacementResult result = nameUtilities.makeSpecificReplacements(input);
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual); //, "Must replace small caps characters"
    }
    
    @Test
    public void testDoublePipeReplace() {
        String input = "\u2016";
        String expected = "||";
        String actual = nameUtilities.symbolsToASCII(input);
        Assertions.assertEquals(expected, actual); //, "Must replace double pipe char"
    }

    @Test
    public void test2SmallCaps() {
        String input = "ʟᴅ glucose";
        String expected = "LD glucose";
        ReplacementResult result = nameUtilities.makeSpecificReplacements(input);
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual); //, "Must replace small caps characters"
    }

    @Test
    public void testNonAsciiRemoval() {
        String input = "a milligram of glucose©";
        String expected = "a milligram of glucose?";
        String actual = nameUtilities.nkfdNormalizations(input);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNonAsciiRemoval2() {
        String input = "some idea\u200B";
        String expected = "some idea";
        ReplacementResult result = nameUtilities.removeZeroWidthChars(input);
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNonAsciiRemoval3() {
        String input = "an little\u200Bbit of glucose©";
        String expected = "an littlebit of glucose?";
        ReplacementResult result = nameUtilities.removeZeroWidthChars(input);
        String actual = nameUtilities.nkfdNormalizations(result.getResult());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMoreZeroWidthRemoval() {
        String input = "an little\u200B\u200C\u200D sugar";
        String expected = "an little sugar";
        ReplacementResult result = nameUtilities.removeZeroWidthChars(input);
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual);
        result.getReplacementNotes().forEach(n -> log.trace(String.format("replaced char at %d (%s)", n.getPosition(), n.getReplacement())));
    }

    @Test
    public void testFull() {
        String input = "a little\u200Bbit of glucose©";
        String expected = "A LITTLEBIT OF GLUCOSE?";
        ReplacementResult result = nameUtilities.fullyStandardizeName(input);
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testFull2() {
        String input = " Α, Β, Γ \u200C,an little\u200Cbit of glucose©";
        String expected = ".ALPHA., .BETA., .GAMMA. ,AN LITTLEBIT OF GLUCOSE?";
        ReplacementResult result = nameUtilities.fullyStandardizeName(input);
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testFull3() {
        String input = " Α, Β, Γ \u200C,  potassium diclofenac";
        String expected = ".ALPHA., .BETA., .GAMMA. , POTASSIUM DICLOFENAC";
        ReplacementResult result = nameUtilities.fullyStandardizeName(input);
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testMiscNonAscii() {
        //several chars from https://terpconnect.umd.edu/~zben/Web/CharSet/htmlchars.html
        String input = "pay attention\u00A1 There is \u00A4 to be made. No, \u00AB isn't XML. \u03A7 \u2191 ";
        String expected = "PAY ATTENTION? THERE IS ? TO BE MADE. NO, \" ISN'T XML. .CHI. ?";
        ReplacementResult result = nameUtilities.fullyStandardizeName(input);
        System.out.println("initial result: " + result.getResult());
        String actual = nameUtilities.nkfdNormalizations( result.getResult());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testReplaceSerialWhitespace1() {
        String input = "sodium  chloride";
        String expected = "sodium chloride";
        ReplacementResult result = nameUtilities.removeSerialSpaces(input);
        Assertions.assertEquals(expected, result.getResult());
    }

    @Test
    public void testReplaceSerialWhitespace2() {
        String input = "potassium       bromide";
        String expected = "potassium bromide";
        ReplacementResult result = nameUtilities.removeSerialSpaces(input);
        Assertions.assertEquals(expected, result.getResult());
    }

    @Test
    public void testReplaceSerialWhitespace3() {
        String input = "potassium   bromide";
        String expected = "potassium bromide";
        ReplacementResult result = nameUtilities.removeSerialSpaces(input);
        Assertions.assertEquals(expected, result.getResult());
    }

    @Test
    public void testReplaceSerialWhitespace4() {
        String input = "sodium chloride";
        String expected = "sodium chloride";
        ReplacementResult result = nameUtilities.removeSerialSpaces(input);
        Assertions.assertEquals(expected, result.getResult());
    }

    @Test
    public void testReplacementResultUpdate() {
        String inputName = "chemical name";
        List<ReplacementNote> notes = new ArrayList<>();

        ReplacementResult result1;
        result1 = new ReplacementResult(inputName, notes);

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
        String inputName ="prêt-à-porter";
        String expected = "PRET-A-PORTER";
        
        ReplacementResult result= nameUtilities.fullyStandardizeName(inputName);
        Assertions.assertEquals(expected, result.getResult());
    }
    
    @Test
    public void testHyphenoids(){
        String input = "\u00AD\u2010\u2011\u2012something else\u2013\u2014\u2212\u2015";
        String expected ="----something else----";
        String actual = NameUtilities.symbolsToASCII(input);
        Assertions.assertEquals(expected, actual);
    }
    
    @Test
    public void testSerialSpaceRemovalForCommas() {
        String input = ".ALPHA., .BETA., .GAMMA.";
        String expected = ".ALPHA., .BETA., .GAMMA.";
        String actual = nameUtilities.removeZeroWidthChars(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    
    @Test
    public void TestfullyStandardizeNameWithEarlyBrackets() {
        String nameWithEarlyBrackets= "[HELLO] THERE";
        String expected = "(HELLO) THERE";
        ReplacementResult result= nameUtilities.fullyStandardizeName(nameWithEarlyBrackets);
        String actual=result.getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void TestfullyStandardizeNameWithEarlyBracketsAndBraces() {
        String nameWithEarlyBrackets= "[user] {friendly} software";
        String expected = "(USER) (FRIENDLY) SOFTWARE";
        ReplacementResult result= nameUtilities.fullyStandardizeName(nameWithEarlyBrackets);
        String actual=result.getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNameCleanup() {
        String nameInput="[HI]THERE [INN][USAN]";
        String expected ="(HI)THERE [INN][USAN]";
        ReplacementResult result=nameUtilities.fullyStandardizeName(nameInput);
        String actual = result.getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNameX1() {
        String inputName ="species 1 × species 2";
        String expected = "SPECIES 1 X SPECIES 2";

        ReplacementResult result= nameUtilities.fullyStandardizeName(inputName);
        Assertions.assertEquals(expected, result.getResult());
    }


    @Test
    public void testRemoveHtml() {
        String input="<i>must</i> <b>remove</b>";
        String expected = "must remove";
        String actual = nameUtilities.removeHtmlUsingJsoup(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testRemoveIllFormedHtml() {
        String input="<i>must <b>remove";
        String expected = "must remove";
        String actual = nameUtilities.removeHtmlUsingJsoup(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testRemoveDangerousHtml() {
        String input="Some simple <script>performMischief()</script>text";
        String expected = "Some simple text";
        String actual = nameUtilities.removeHtmlUsingJsoup(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testRemoveNestedHtml() {
        String input="Some <div class=\"x\">text that <emphasis>contains</emphasis> a message for us</div>";
        String expected = "Some text that contains a message for us";
        String actual = nameUtilities.removeHtmlUsingJsoup(input).getResult();
        Assertions.assertEquals(expected, actual);
    }

}
