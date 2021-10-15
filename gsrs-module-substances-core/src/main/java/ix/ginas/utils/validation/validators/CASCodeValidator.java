package ix.ginas.utils.validation.validators;

import ix.core.models.Keyword;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Git Issue 275: CAS-specific validation rule should be refactored into its own
 * isolated validation rule
 *
 * @author mitch
 */
public class CASCodeValidator extends AbstractValidatorPlugin<Substance> {

    @Override
    public void validate(Substance substance, Substance oldSubstance, ValidatorCallback callback) {
        for (Code cd : substance.codes) {

            if ("CAS".equals(cd.codeSystem)) {
                if (!isValidCas(cd.code)) {
                    GinasProcessingMessage mesWarn = GinasProcessingMessage
                            .WARNING_MESSAGE(
                                    String.format("CAS Number %s does not have the expected format. (Verify the check digit.)", cd.code))
                            .appliableChange(true);

                    callback.addMessage(mesWarn);
                }

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
                                    "Must specify STN reference for CAS")
                            .appliableChange(true);

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

    public static boolean isValidCas(String candidate) {
        String clean = candidate.trim().replace("-", "");
        char[] chars = clean.toCharArray();
        if (chars.length == 0) {
            return false;
        }

        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        //calculate the expected value of the check digit
        ArrayUtils.reverse(chars);
        AtomicInteger count = new AtomicInteger(0);
        for (int i = 1; i < chars.length; i++) {
            count.getAndAdd(i * Character.getNumericValue(chars[i]));
        }
        //verify that it matches the actual
        return Character.getNumericValue(chars[0]) == (count.get() % 10);
    }
}
