package gsrs.module.substance.utils;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author tyler
 */
@Slf4j
public class FDAFullNameStandardizer implements NameStandardizer{

    @Override
    public ReplacementResult standardize(String input) {
        return NameUtilities.getInstance().fullyStandardizeName(input);
    }
    
}
