package ix.ginas.utils.validation.validators;

import gsrs.module.substance.utils.NameUtilities;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

/**
 * apply a minimal standardization (remove serial white space and non-printable characters) to the main name
 * @author mitch
 */
@Slf4j
public class BasicNameValidator extends AbstractValidatorPlugin<Substance> {

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        log.trace("starting in validate");
        if (s == null) {
            log.warn("Substance is null");
            return;
        }
        if (s.names == null || s.names.isEmpty()) {
            //do not expect this to happen -- substance will be tested for no names
            log.warn("Substance has no names!");
        }

        s.names.forEach(n -> {

            NameUtilities.ReplacementResult minimallyStandardizedName = NameUtilities.getInstance().standardizeMinimally(n.name);
            String debugMessage = String.format("name: %s; minimallyStandardizedName: %s", n.name, 
                    minimallyStandardizedName.getResult());
            log.trace(debugMessage);
            
            if (!minimallyStandardizedName.getResult().equals(n.name) || minimallyStandardizedName.getReplacementNotes().size() > 0) {
                GinasProcessingMessage mes = GinasProcessingMessage.WARNING_MESSAGE(String.format("Name %s minimally standardized to %s",
                        n.name, minimallyStandardizedName.getResult()));
                callback.addMessage(mes, ()->n.name=minimallyStandardizedName.getResult());
            }

        });
    }
}
