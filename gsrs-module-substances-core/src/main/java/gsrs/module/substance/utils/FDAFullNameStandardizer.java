package gsrs.module.substance.utils;

/**
 *
 * @author tyler
 */
public class FDAFullNameStandardizer implements NameStandardizer{

    @Override
    public ReplacementResult standardize(String input) {
        return NameUtilities.getInstance().fullyStandardizeName(input);
    }
    
}
