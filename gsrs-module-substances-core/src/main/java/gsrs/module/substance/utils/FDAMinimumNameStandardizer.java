package gsrs.module.substance.utils;

import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author tyler
 */
public class FDAMinimumNameStandardizer implements NameStandardizer{

    @Autowired
    NameUtilities nameUtilities;

    @Override
    public ReplacementResult standardize(String input) {
        return nameUtilities.standardizeMinimally(input);
    }
    
}
