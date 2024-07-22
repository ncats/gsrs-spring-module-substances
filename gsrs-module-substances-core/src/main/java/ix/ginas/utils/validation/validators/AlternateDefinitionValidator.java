package ix.ginas.utils.validation.validators;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;

import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by katzelda on 5/14/18.
 */
public class AlternateDefinitionValidator extends AbstractValidatorPlugin<Substance> {
    @Autowired
    private SubstanceRepository substanceRepository;

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        if (s.substanceClass == Substance.SubstanceClass.concept) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Alternative definitions cannot be \"concepts\""));
        }
        if (s.names != null && !s.names.isEmpty()) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Alternative definitions cannot have names"));
        }
        if (s.codes != null && !s.codes.isEmpty()) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Alternative definitions cannot have codes"));
        }
        if (s.relationships == null || s.relationships.isEmpty()) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Alternative definitions must specify a primary substance"));
        } else {
            if (s.relationships.size() > 1) {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE("Alternative definitions may only have 1 relationship (to the parent definition), found:%s",
                                s.relationships.size()));
            } else {
                SubstanceReference sr = s.getPrimaryDefinitionReference();
                if (sr == null) {
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE("Alternative definitions must specify a primary substance"));
                } else {
                    Substance subPrimary = substanceRepository.findBySubstanceReference(sr);
                    if (subPrimary == null) {
                        callback.addMessage(GinasProcessingMessage
                                .ERROR_MESSAGE("Primary definition for '%s' (%s) not found",
                                        sr.refPname, sr.refuuid));
                    } else {
                        if (subPrimary.definitionType != Substance.SubstanceDefinitionType.PRIMARY) {
                            callback.addMessage(GinasProcessingMessage
                                    .ERROR_MESSAGE("Cannot add alternative definition for '%s' (%s). That definition is not primary.",
                                            sr.refPname, sr.refuuid));
                        } else {
                            if (subPrimary.substanceClass == Substance.SubstanceClass.concept) {
                                callback.addMessage(GinasProcessingMessage
                                        .ERROR_MESSAGE("Cannot add alternative definition for '%s' (%s). That definition is not definitional substance record.",
                                                sr.refPname, sr.refuuid));
                            } else {
                             //Everything is okay   
                            }
                        }
                    }
                }
            }
        }

    }
}
