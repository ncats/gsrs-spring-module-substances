package ix.ginas.utils.validation.validators;

import gsrs.module.substance.utils.NameUtilities;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

/**
 * apply a minimal standardization (remove serial white space and non-printable characters) to the main name
 * and a full standardization to the stdName
 * @author mitch
 */
@Slf4j
public class StandardNameValidator extends AbstractValidatorPlugin<Substance> {

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        if (s == null) {
            log.warn("Substance is null");
            return;
        }
        if (s.names == null || s.names.isEmpty()) {
            //do not expect this to happen -- substance will be tested for no names
            log.warn("Substance has no names!");
        }

        NameUtilities nameUtils = new NameUtilities();
        s.names.forEach(n -> {

            NameUtilities.ReplacementResult minimallyStandardizedName = nameUtils.standardizeMinimally(n.name);
            if (!minimallyStandardizedName.getResult().equals(n.name) || minimallyStandardizedName.getReplacementNotes().size() > 0) {
                GinasProcessingMessage mes = GinasProcessingMessage.INFO_MESSAGE(String.format("Name %s minimally standardized to %s",
                        n.name, minimallyStandardizedName.getResult()));
                callback.addMessage(mes);
            }
            NameUtilities.ReplacementResult fullStandardizedName = nameUtils.fullyStandardizeName(n.name);
            if (n.stdName == null || n.stdName.length() == 0) {
                n.stdName = fullStandardizedName.getResult();
                GinasProcessingMessage mes = GinasProcessingMessage.INFO_MESSAGE(String.format("Name %s fully standardized to %s",
                        n.name, fullStandardizedName.getResult()));
                callback.addMessage(mes);

            }

        });
    }
}
