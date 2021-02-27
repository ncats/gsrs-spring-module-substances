package ix.ginas.utils.validation.validators.fda;

import ix.core.validator.ValidatorCallback;
import ix.ginas.utils.validation.validators.AbstractValidatorPlugin;
import ix.srs.models.Product;

public class ProductValidator extends AbstractValidatorPlugin<Product> {

	@Override
	public void validate(Product obj, Product objold, ValidatorCallback callback) {

		/*
		if (obj.countryCode == null || obj.countryCode.isEmpty()) {
			callback.addMessage(GinasProcessingMessage
					.ERROR_MESSAGE("Country Code is required."));

		}
		*/
	}
}