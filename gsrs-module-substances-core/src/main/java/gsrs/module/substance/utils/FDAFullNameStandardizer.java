package gsrs.module.substance.utils;

import gsrs.springUtils.AutowireHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 *
 * @author tyler
 */
@Slf4j
public class FDAFullNameStandardizer implements NameStandardizer{

    @Autowired
    NameUtilities nameUtilities;

    @Override
    public ReplacementResult standardize(String input) {
        Objects.requireNonNull(nameUtilities, "autowiring of  NameUtilities must work");
        return nameUtilities.fullyStandardizeName(input);
    }
    
}
