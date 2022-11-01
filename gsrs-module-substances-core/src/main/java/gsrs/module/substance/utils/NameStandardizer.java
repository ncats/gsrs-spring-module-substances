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
public interface NameStandardizer {
    /**
     * Standardizes an input string and returns an {@link ReplacementResult} which
     * explains what was done to standardize the input string.
     * @param input The input string to be standardized
     * @return A {@link ReplacementResult} whose {@link ReplacementResult#getResult()} method returns the newly formatted name.
     */
   public ReplacementResult standardize(String input);
   
   /**
    * Checks if the supplied name string is already in-line with standardization rules. The basic implementation
    * simply runs {@link #standardize(String)} and compares the result with the input string. If the input and
    * standardized output are the same, this method returns true.
    * @param input The string to test
    * @return true if already standardized, false if not
    */
   public default boolean isStandardized(String input) {
       if(input.equals(standardize(input).getResult())){
           return true;
       }
       return false;
   }
}
