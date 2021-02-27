package ix.ginas.utils.validation.validators.fda;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.utils.validation.validators.AbstractValidatorPlugin;
import ix.srs.models.ApplicationSrs;

public class ApplicationValidator extends AbstractValidatorPlugin<ApplicationSrs> {

	@Override
	public void validate(ApplicationSrs obj, ApplicationSrs objold, ValidatorCallback callback) {

		if (obj.center == null || obj.center.isEmpty()) {
			callback.addMessage(GinasProcessingMessage
					.ERROR_MESSAGE("Center is required."));

		}

		if (obj.appType == null || obj.appType.isEmpty()) {
			callback.addMessage(GinasProcessingMessage
					.ERROR_MESSAGE("Application Type is required."));

		}

		if (obj.appNumber == null || obj.appNumber.isEmpty()) {
			callback.addMessage(GinasProcessingMessage
					.ERROR_MESSAGE("Application Number is required."));

		}
	}
}