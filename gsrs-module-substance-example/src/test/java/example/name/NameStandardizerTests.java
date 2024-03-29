package example.name;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import gsrs.module.substance.standardizer.FDAFullNameStandardizer;
import gsrs.module.substance.standardizer.NameStandardizer;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author mitch
 */
@Slf4j
public class NameStandardizerTests {

    @Test
    public void testNameWithEarlyBrackets() {
        NameStandardizer ns = new FDAFullNameStandardizer();
        String nameWithEarlyBrackets= "[HELLO] THERE";
        boolean result = ns.isStandardized(nameWithEarlyBrackets);
        Assertions.assertFalse(result);
    }
    
    @Test
    public void testDoNotAllowLateNonBracketTermBrackets() {
        NameStandardizer ns = new FDAFullNameStandardizer();
        String nameWithEarlyBrackets= "1,2-CHLORO [3,5]BENZENE";
        boolean result = ns.isStandardized(nameWithEarlyBrackets);
        Assertions.assertFalse(result);
    }
    
    @Test
    public void testDoAllowLateBracketTermBrackets() {
        NameStandardizer ns = new FDAFullNameStandardizer();
        String nameWithEarlyBrackets= "1,2-CHLORO (3,5)BENZENE [INCI]";
        boolean result = ns.isStandardized(nameWithEarlyBrackets);
        Assertions.assertTrue(result);
    }

    @Test
    public void testDoNotAllowLateBracketWithRealTermBrackets() {
        NameStandardizer ns = new FDAFullNameStandardizer();
        String nameWithEarlyBrackets= "1,2-CHLORO [3,5]BENZENE [INCI]";
        boolean result = ns.isStandardized(nameWithEarlyBrackets);
        Assertions.assertFalse(result);
    }
    @Test
    public void testDoAllowMultipleRealBracketTerm() {
        NameStandardizer ns = new FDAFullNameStandardizer();
        String nameWithEarlyBrackets= "1,2-CHLORO (3,5)BENZENE [USAN][INN]";
        boolean result = ns.isStandardized(nameWithEarlyBrackets);
        Assertions.assertTrue(result);
    }

    @Test
    public void testDoNotAllowLinefeeds() {
        NameStandardizer ns = new FDAFullNameStandardizer();
        String nameWithLinefeeds = "\n\rI AM HUNGRY LIKE A\r\nWOLF";
        boolean result = ns.isStandardized(nameWithLinefeeds);
        Assertions.assertFalse(result);
    }

    @Test
    public void testDoNotAllowLeadingWhitespace() {
        NameStandardizer ns = new FDAFullNameStandardizer();
        String input = " GIMME SOME SPACE";
        boolean result = ns.isStandardized(input);
        Assertions.assertFalse(result);
    }

    @Test
    public void testDoNotAllowTrailingWhitespace() {
        NameStandardizer ns = new FDAFullNameStandardizer();
        String input = "GIMME SOME SPACE ";
        boolean result = ns.isStandardized(input);
        Assertions.assertFalse(result);
    }
}
