package ix.ginas.utils.validation.validators;

import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;

/**
 * Validator that is usually the only validator that supports
 * {@link ValidatorConfig.METHOD_TYPE#IGNORE}
 *
 * Created by katzelda on 5/15/18.
 */
public class IgnoreValidator extends AbstractValidatorPlugin<Substance> {
    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        callback.setValid();
    }

    @Override
    public boolean supports(Substance newValue, Substance oldValue, ValidatorConfig.METHOD_TYPE methodType) {
        return methodType == ValidatorConfig.METHOD_TYPE.IGNORE;
    }
}
