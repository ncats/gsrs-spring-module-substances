package gsrs.module.substance.standardizer;

import gsrs.module.substance.utils.NameUtilities;

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
