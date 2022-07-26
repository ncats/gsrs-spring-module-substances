package gsrs.module.substance.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
        /*
        Tested out the strategy below BUT, calling the constructor directly leads to an object that is not
        correctly initialized
        if( nameUtilities == null) {
            nameUtilities= new NameUtilities();
            AutowireHelper.getInstance().autowireAndProxy(nameUtilities);
        }*/
        Objects.requireNonNull(nameUtilities, "autowiring of NameUtilities must work");
        return nameUtilities.fullyStandardizeName(input);
    }
    
}
