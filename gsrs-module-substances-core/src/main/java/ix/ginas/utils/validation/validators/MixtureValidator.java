package ix.ginas.utils.validation.validators;

import gsrs.module.substance.repository.ReferenceRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Component;
import ix.ginas.models.v1.MixtureSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by katzelda on 5/14/18.
 */
@Data
public class MixtureValidator extends AbstractValidatorPlugin<Substance> {
    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private ReferenceRepository referenceRepository;

    @Override
    @Transactional(readOnly = true)
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        MixtureSubstance cs = (MixtureSubstance) objnew;
        if (cs.mixture == null) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Mixture substance must have a mixture element"));
            return;
        }
        if (cs.mixture.components == null
                || cs.mixture.components.size() < 2) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE("Mixture substance must have at least 2 mixture components"));
        } else {
            Set<String> mixtureIDs = new HashSet<>();
            int oneOfCount=0;
            for (Component c : cs.mixture.components) {
                if (c.substance == null) {
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE("Mixture components must reference a substance record, found:\"null\""));
                }else if(c.type == null || c.type.length()<=0){
                    callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("Mixture components must specify a type"));
                }else {
//                    Substance comp = SubstanceFactory.getFullSubstance(c.substance);
                    if (!substanceRepository.exists(c.substance)) {
                        callback.addMessage(GinasProcessingMessage
                                .WARNING_MESSAGE("Mixture substance references \"%s\" which is not yet registered",
                                        c.substance.getName()));
                    }
                    //add returns false if it's already present so we don't need to do the contains() check before add
                    if (!mixtureIDs.add(c.substance.refuuid)) {

                        callback.addMessage(GinasProcessingMessage
                                .ERROR_MESSAGE("Cannot reference the same mixture substance twice in a mixture:\"%s\"",
                                        c.substance.refPname));
                    }
                }

                if("MAY_BE_PRESENT_ONE_OF".equals(c.type)){
                    oneOfCount++;
                }
            }
            //GSRS-199
            //You should have to have at least two "One of" components in the mixture record.
            if(oneOfCount == 1){
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE("Should have at least two \"One of\" components in the mixture record"));
            }

            ValidationUtils.validateReference(cs, cs.mixture, callback, ValidationUtils.ReferenceAction.FAIL, referenceRepository);

        }

    }
}
