package ix.ginas.utils;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author mitch
 */
public class CASUtilities {

    public static boolean isValidCas(String candidate) {
        String clean = candidate.trim().replace("-", "");
        char[] chars = clean.toCharArray();
        if (chars.length == 0) {
            return false;
        }

        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        //calculate the expected value of the check digit
        ArrayUtils.reverse(chars);
        AtomicInteger count = new AtomicInteger(0);
        for (int i = 1; i < chars.length; i++) {
            count.getAndAdd(i * Character.getNumericValue(chars[i]));
        }
        //verify that it matches the actual
        return Character.getNumericValue(chars[0]) == (count.get() % 10);
    }
}
