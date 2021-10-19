package ix.ginas.utils.validation.validators;

import ix.core.models.Keyword;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.CASUtilities;

/**
 * Git Issue CAS-specific validation rule should be refactored into its own
 * isolated validation rule
 *
 * @author mitch
 */
public class CASCodeValidator extends AbstractValidatorPlugin<Substance> {

    private boolean performFormatCheck = true;
    private boolean performStnReferenceCheck = false;

    @Override
    public void validate(Substance substance, Substance oldSubstance, ValidatorCallback callback) {
        for (Code cd : substance.codes) {

            if ("CAS".equals(cd.codeSystem)) {
                if (performFormatCheck && !CASUtilities.isValidCas(cd.code)) {
                    GinasProcessingMessage mesWarn = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    String.format("CAS Number %s does not have the expected format. (Verify the check digit.)", cd.code));

                    callback.addMessage(mesWarn);
                }

                if (performStnReferenceCheck) {
                    boolean found = false;
                    for (Keyword keywords : cd.getReferences()) {
                        Reference ref = substance.getReferenceByUUID(keywords.term);
                        if ("STN (SCIFINDER)".equalsIgnoreCase(ref.docType)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        GinasProcessingMessage mes = GinasProcessingMessage
                                .WARNING_MESSAGE(
                                        "Must specify STN reference for CAS");

                        callback.addMessage(mes, () -> {
                            Reference newRef = new Reference();
                            newRef.citation = "STN";
                            newRef.docType = "STN (SCIFINDER)";
                            newRef.publicDomain = true;

                            cd.addReference(newRef, substance);
                        });
                    }
                }
            }
        }
    }

    public void setPerformFormatCheck(boolean performFormatCheck) {
        this.performFormatCheck = performFormatCheck;
    }

    public void setPerformStnReferenceCheck(boolean performStnReferenceCheck) {
        this.performStnReferenceCheck = performStnReferenceCheck;
    }

}
