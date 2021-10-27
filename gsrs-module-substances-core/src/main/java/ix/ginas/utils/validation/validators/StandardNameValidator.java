package ix.ginas.utils.validation.validators;

import gsrs.module.substance.utils.NameUtilities;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

/**
*
* @author mitch
*/
@Slf4j
public class StandardNameValidator extends AbstractValidatorPlugin<Substance>{

     @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        if( s==null) {
            log.warn("Substance is null");
        }
        
        NameUtilities nameUtils = new NameUtilities();
        /*s.names.forEach(n->{
            String minimallyCleanedName = nameUtils.
        });*/
        
    }
}

