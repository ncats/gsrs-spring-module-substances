package gsrs.module.substance.utils;

import ix.ginas.utils.validation.validators.tags.TagUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 * @author tyler
 */
@Slf4j
@Component
public class FDAFullNameStandardizer implements NameStandardizer{

    @Autowired
    NameUtilities nameUtilities;

    @Override
    public ReplacementResult standardize(String input) {
        Objects.requireNonNull(nameUtilities, "autowiring of NameUtilities must work");
        nameUtilities.assureDependencies();
        return fullyStandardizeName(input, nameUtilities);
    }

    public ReplacementResult fullyStandardizeName(String input, NameUtilities nameUtilities) {

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
        /*
        Remove characters, possibly introduced into the  name accidentally, that cannot be rendered on paper
         */
        ReplacementResult initialResult = nameUtilities.replaceUnprintables(namePart);

        /*
        Replace a specified set of characters with designated others.
         */
        ReplacementResult resultForSpecifics = initialResult.update(nameUtilities.makeSpecificReplacements(initialResult.getResult()));
        String workingString = resultForSpecifics.getResult();

        /*
        Replace a specified set of characters that have ASCII equivalents
         */
        workingString = nameUtilities.symbolsToASCII(workingString);
        ReplacementResult zeroWidthRemovalResult = nameUtilities.removeZeroWidthChars(workingString);
        results.update(zeroWidthRemovalResult);
        workingString = nameUtilities.nkfdNormalizations(results.getResult());
        results.update(nameUtilities.removeSerialSpaces(workingString));
        results.update(nameUtilities.removeHtmlUsingJsoup(results.getResult()));
        results.setResult(results.getResult().toUpperCase() +suffix);
        return results;
    }
}
