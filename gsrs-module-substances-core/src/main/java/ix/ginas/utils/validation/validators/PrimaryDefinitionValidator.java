package ix.ginas.utils.validation.validators;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by katzelda on 5/14/18.
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class PrimaryDefinitionValidator extends AbstractValidatorPlugin<Substance> {

    @Autowired
    private SubstanceRepository substanceRepository;


    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        SubstanceReference sr = s.getPrimaryDefinitionReference();
        if (sr != null) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Primary definitions cannot be alternative definitions for other Primary definitions"));
        }
        for (SubstanceReference relsub : s
                .getAlternativeDefinitionReferences()) {
//            Substance subAlternative = SubstanceFactory.getFullSubstance(relsub);
            //TODO katzelda Feb 2021 : we should havethe repository return an object with basic info like id and is primary without doing all the joins
            Substance subAlternative = substanceRepository.findBySubstanceReference(relsub);
            if(subAlternative ==null){
                //does not exist
                callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE("Alternative definition not found %s", relsub.refPname));
            }else if (subAlternative.isPrimaryDefinition()) {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE("Primary definitions cannot be alternative definitions for other Primary definitions"));
            }
        }

    }
}
