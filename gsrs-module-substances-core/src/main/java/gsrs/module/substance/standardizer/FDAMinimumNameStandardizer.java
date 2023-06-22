package gsrs.module.substance.standardizer;

import gsrs.module.substance.utils.NameUtilities;

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
