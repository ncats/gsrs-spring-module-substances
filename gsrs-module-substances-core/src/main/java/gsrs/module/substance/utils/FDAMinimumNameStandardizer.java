package gsrs.module.substance.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 *
 * @author tyler
 */
@Component
public class FDAMinimumNameStandardizer implements NameStandardizer{

    @Autowired
    NameUtilities nameUtilities;

    @Override
    public ReplacementResult standardize(String input) {
        Objects.requireNonNull(nameUtilities, "autowiring of NameUtilities must work");
        return nameUtilities.standardizeMinimally(input);
    }
    
}
