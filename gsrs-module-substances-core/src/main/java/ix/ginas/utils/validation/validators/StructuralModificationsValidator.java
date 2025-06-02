package ix.ginas.utils.validation.validators;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.StructuralModification;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

/**
 * Created by peryeata on 7/16/18.
 */
public class StructuralModificationsValidator extends AbstractValidatorPlugin<Substance> {
    private static final String CV_AMINO_ACID_SUBSTITUTION = "AMINO_ACID_SUBSTITUTION";
    private final String StructuralModificationsValidatorNullWarning = "StructuralModificationsValidatorNullWarning";
    private final String StructuralModificationsValidatorTypeError = "StructuralModificationsValidatorTypeError";
    private final String StructuralModificationsValidatorRecordError = "StructuralModificationsValidatorRecordError";

	@Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {

        Iterator<StructuralModification> strModIter = Optional.ofNullable(s.getModifications())
        		                                              .map(m->m.structuralModifications)
        		                                              .orElse(Collections.emptyList())
        		                                              .iterator();
        while(strModIter.hasNext()){
        	StructuralModification mod = strModIter.next();
        	if (mod == null) {
        		GinasProcessingMessage mes = GinasProcessingMessage
        				.WARNING_MESSAGE(StructuralModificationsValidatorNullWarning, "Null modifications objects are not allowed")
        				.appliableChange(true);
        		callback.addMessage(mes, ()->strModIter.remove());
        		continue;
        	}
        	if (ValidationUtils.isEffectivelyNull(mod.structuralModificationType)) {
        		GinasProcessingMessage mes = GinasProcessingMessage
        				.ERROR_MESSAGE(StructuralModificationsValidatorTypeError,
        						"Structural Modifications must specify a type")
        				.appliableChange(true);
        		callback.addMessage(mes, ()-> mod.structuralModificationType=CV_AMINO_ACID_SUBSTITUTION);
        	}

        	if (ValidationUtils.isEffectivelyNull(mod.molecularFragment) ||
        			ValidationUtils.isEffectivelyNull(mod.molecularFragment.refuuid)) {
        		GinasProcessingMessage mes = GinasProcessingMessage
        				.ERROR_MESSAGE(StructuralModificationsValidatorRecordError,
        						"Must specify a record to be used for a structural modification.")
        				.appliableChange(true);
        		callback.addMessage(mes);
        	}
        }
    }
}
