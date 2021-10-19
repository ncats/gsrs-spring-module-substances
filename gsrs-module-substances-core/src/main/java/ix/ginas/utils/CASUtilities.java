package ix.ginas.utils;

import java.util.concurrent.atomic.AtomicInteger;

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
        AtomicInteger count = new AtomicInteger(0);
        for (int i = chars.length-1; i >0; i--) {
            int position = chars.length-i;
            System.out.println("going to multiply " + position + " times " + Character.getNumericValue(chars[i-1]));
            count.getAndAdd(position * Character.getNumericValue(chars[i-1]));
        }
        //verify that it matches the actual
        System.out.println("check digit: " + chars[chars.length-1]);
        return Character.getNumericValue(chars[chars.length-1]) == (count.get() % 10);
    }

}
