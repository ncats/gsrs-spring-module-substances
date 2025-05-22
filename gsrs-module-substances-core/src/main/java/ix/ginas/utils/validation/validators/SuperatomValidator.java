package ix.ginas.utils.validation.validators;

import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.SGroup;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;

/**
 * Validator to error out if a mol file
 * contains SUP s-groups (Super atoms).
 */
public class SuperatomValidator extends AbstractValidatorPlugin<Substance> {
	
	private final String SuperatomValidatorError1 = "SuperatomValidatorError1";
	private final String SuperatomValidatorError2 = "SuperatomValidatorError2";
	
    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        if (s.substanceClass == Substance.SubstanceClass.chemical) {
            ChemicalSubstance chemicalSubstance = (ChemicalSubstance) s;
            if(chemicalSubstance.getStructure() ==null){
                return;
            }
            Chemical chem = chemicalSubstance.getStructure().toChemical();
            for (SGroup sgroup : chem.getSGroups()) {
                if (sgroup.getType() == SGroup.SGroupType.SUPERATOM_OR_ABBREVIATION) {
                    if (sgroup.getSuperatomLabel().isPresent()) {
                        callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(SuperatomValidatorError1, 
                        		"Super Atoms are not allowed please remove: '" + sgroup.getSuperatomLabel().get() + "'"));
                    }else{
                        callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(SuperatomValidatorError2,"Super Atoms are not allowed please remove"));

                    }
                }
            }
        }
    }
}
