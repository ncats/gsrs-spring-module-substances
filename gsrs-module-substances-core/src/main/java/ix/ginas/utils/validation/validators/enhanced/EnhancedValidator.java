package ix.ginas.utils.validation.validators.enhanced;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.validators.tags.TagUtilities;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@Slf4j
@Data
public class EnhancedValidator extends AbstractValidatorPlugin<Substance> {
    //    @Autowired
    //    private SubstanceRepository substanceRepository;

    private String foo;

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        // This is a TEMPORARY, simple validator for testing.
        if (s.getAllNames().size() > 0) {
            GinasProcessingMessage mes = GinasProcessingMessage
            .WARNING_MESSAGE(String.format("Only one name, please."));
            mes.appliableChange(true);
            callback.addMessage(mes);
        }
    }
}

