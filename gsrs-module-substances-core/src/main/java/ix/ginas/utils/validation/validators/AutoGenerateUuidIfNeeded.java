package ix.ginas.utils.validation.validators;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;

import java.util.UUID;

/**
 * Created by katzelda on 5/7/18.
 */
public class AutoGenerateUuidIfNeeded extends AbstractValidatorPlugin<Substance> {
	
	 private final String AutoGenerateUuidIfNeededNoUUIDInfo = "AutoGenerateUuidIfNeededNoUUIDInfo"; 
	 
    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        if (s.getUuid() == null) {
        	UUID uuid = UUID.randomUUID();
            callback.addMessage(GinasProcessingMessage.INFO_MESSAGE(AutoGenerateUuidIfNeededNoUUIDInfo,
            		"Substance has no UUID, will generate uuid:" + uuid),
                    ()->s.setUuid(uuid));
        }
    }
}
