package ix.ginas.utils.validation.validators;

import gsrs.module.substance.repository.ReferenceRepository;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by katzelda on 5/14/18.
 */
public class SSSG1Validator extends AbstractValidatorPlugin<Substance> {

    @Autowired
    private ReferenceRepository referenceRepository;
    
    private final String SSSG1ValidatorComponentError = "SSSG1ValidatorComponentError";
    private final String SSSG1ValidatorConstituentError1 = "SSSG1ValidatorConstituentError1";
    private final String SSSG1ValidatorConstituentError2 = "SSSG1ValidatorConstituentError2";

    public ReferenceRepository getReferenceRepository() {
        return referenceRepository;
    }

    public void setReferenceRepository(ReferenceRepository referenceRepository) {
        this.referenceRepository = referenceRepository;
    }

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        SpecifiedSubstanceGroup1Substance cs = (SpecifiedSubstanceGroup1Substance) objnew;
        if (cs.specifiedSubstance == null) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE(SSSG1ValidatorComponentError, "Specified substance must have a specified substance component"));
            return;
        }

        if (cs.specifiedSubstance.constituents== null || cs.specifiedSubstance.constituents.isEmpty()) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE(SSSG1ValidatorConstituentError1, "Specified substance must have at least 1 constituent"));
        } else {
            cs.specifiedSubstance.constituents.stream()
                    .filter(c->c.substance==null)
                    .findAny()
                    .ifPresent(missingSubstance->{
                        callback.addMessage(GinasProcessingMessage
                                .ERROR_MESSAGE(SSSG1ValidatorConstituentError2, "Specified substance constituents must have an associated substance record"));
                    });
            ValidationUtils.validateReference(cs, cs.specifiedSubstance, callback, ValidationUtils.ReferenceAction.FAIL, referenceRepository);

        }


    }
}
