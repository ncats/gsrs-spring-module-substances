package ix.ginas.utils.validation.validators;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.utils.validation.AbstractValidatorPlugin;

/**
 * Created by katzelda on 5/7/18.
 */
public class NullCheckValidator<T> extends AbstractValidatorPlugin<T> {
	
	private final String NullCheckValidatorError = "NullCheckValidatorError";
	
    @Override
    public void validate(T objnew, T objold, ValidatorCallback callback) {
        if(objnew ==null){
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(NullCheckValidatorError, "Substance cannot be parsed"));
            callback.haltProcessing();
        }

    }
}
