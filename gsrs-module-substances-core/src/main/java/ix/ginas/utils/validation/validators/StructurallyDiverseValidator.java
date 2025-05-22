package ix.ginas.utils.validation.validators;

import gsrs.module.substance.repository.ReferenceRepository;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by katzelda on 5/14/18.
 */
public class StructurallyDiverseValidator extends AbstractValidatorPlugin<Substance> {
    @Autowired
    private ReferenceRepository referenceRepository;
    
    private final String StructurallyDiverseValidatorError1 = "StructurallyDiverseValidatorError1";
    private final String StructurallyDiverseValidatorError2 = "StructurallyDiverseValidatorError2";
    private final String StructurallyDiverseValidatorError3 = "StructurallyDiverseValidatorError3";
    private final String StructurallyDiverseValidatorError4 = "StructurallyDiverseValidatorError4";
    private final String StructurallyDiverseValidatorError5 = "StructurallyDiverseValidatorError5";
    private final String StructurallyDiverseValidatorWarning = "StructurallyDiverseValidatorWarning";

    public ReferenceRepository getReferenceRepository() {
        return referenceRepository;
    }

    public void setReferenceRepository(ReferenceRepository referenceRepository) {
        this.referenceRepository = referenceRepository;
    }

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        StructurallyDiverseSubstance cs = (StructurallyDiverseSubstance)objnew;
        if (cs.structurallyDiverse == null) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE(StructurallyDiverseValidatorError1,
                    		"Structurally diverse substance must have a structurally diverse element"));
            return;
        }

        if (cs.structurallyDiverse.sourceMaterialClass == null
                || cs.structurallyDiverse.sourceMaterialClass.equals("")) {

            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE(StructurallyDiverseValidatorError2, "Structurally diverse substance must specify a sourceMaterialClass"));
        } else {
            if (cs.structurallyDiverse.sourceMaterialClass
                    .equals("ORGANISM")) {
                boolean hasParent = false;
                boolean hasTaxon = false;
                if (cs.structurallyDiverse.parentSubstance != null) {
                    hasParent = true;
                }
                if (cs.structurallyDiverse.organismFamily != null
                        && !cs.structurallyDiverse.organismFamily
                        .equals("")) {
                    hasTaxon = true;
                }
                if (cs.structurallyDiverse.part == null
                        || cs.structurallyDiverse.part.isEmpty()) {
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE(StructurallyDiverseValidatorError3, "Structurally diverse organism substance must specify at least one (1) part"));
                }
                if (!hasParent && !hasTaxon) {
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE(StructurallyDiverseValidatorError4, "Structurally diverse organism substance must specify a parent substance, or a family"));
                }
                if (hasParent && hasTaxon) {
                    callback.addMessage(GinasProcessingMessage
                            .WARNING_MESSAGE(StructurallyDiverseValidatorWarning, "Structurally diverse organism substance typically should not specify both a parent and taxonomic information"));
                }
                if (cs.structurallyDiverse.sourceMaterialType == null
                        || cs.structurallyDiverse.sourceMaterialType.equals("")) {
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE(StructurallyDiverseValidatorError5, "Organism Structurally diverse substance must specify a sourceMaterialType"));
                }
            }
           
        }
       
        if(!cs.structurallyDiverse.part.isEmpty()) {
            ValidationUtils.validateReference(cs, cs.structurallyDiverse, callback, ValidationUtils.ReferenceAction.FAIL, referenceRepository);
        }


    }
}
