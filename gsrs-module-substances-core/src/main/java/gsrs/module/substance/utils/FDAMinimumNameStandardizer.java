package gsrs.module.substance.utils;

import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author tyler
 */
public class FDAMinimumNameStandardizer implements NameStandardizer{

    @Override
    public ReplacementResult standardize(String input) {
        return NameUtilities.getInstance().standardizeMinimally(input);
    }
    
}
