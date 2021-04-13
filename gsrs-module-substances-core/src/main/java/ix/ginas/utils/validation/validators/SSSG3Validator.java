package ix.ginas.utils.validation.validators;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.SpecifiedSubstanceGroup3Substance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;

public class SSSG3Validator extends AbstractValidatorPlugin<Substance> {
    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        SpecifiedSubstanceGroup3Substance cs = (SpecifiedSubstanceGroup3Substance) objnew;
        if (cs.specifiedSubstanceG3 == null) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Specified substance group 3 must have a specified substance component"));
            return;
        }

        if (cs.specifiedSubstanceG3.getGrade() == null) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Specified substance group 3 must have at least 1 grade"));
        } else {
            if (cs.specifiedSubstanceG3.getGrade() != null) {
                if (cs.specifiedSubstanceG3.getGrade().getName() == null) {
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE("Grade Name is required."));
                }

                if (cs.specifiedSubstanceG3.getGrade().getType() == null) {
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE("Grade Type is required."));
                }
            }
        }

    }
}
