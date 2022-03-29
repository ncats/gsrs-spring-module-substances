package gsrs.module.substance.utils;

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
