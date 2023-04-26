package ix.ginas.utils.validation.validators;

import gsrs.module.substance.importers.model.ImportSubstanceReference;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.validator.*;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/*
This validator is designed for records that are processed using CreateProcessingAction and SubstanceStagingAreaEntityService
If a record has dependent substances required for its definition (such as components of a mixture), make sure the
dependent substances are present
 */
@Slf4j
public class DefinitionalDependencyValidator extends AbstractValidatorPlugin<Substance> {

    public final static String SUBSTANCE_REFERENCE_DEFINITIONAL = "Definitional";

    public final static String SUBSTANCE_REFERENCE_RELATED = "Related";

    public final static String SUBSTANCE_REFERENCE_MEDIATOR = "Mediator";

    private final boolean includeMediatorSubstances=true;

    @Autowired
    SubstanceRepository substanceRepository;

    @Override
    public boolean supportsCategory(Substance newValue, Substance oldValue, ValidatorCategory cat) {
        return super.supportsCategory(newValue, oldValue, cat);
    }

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        ValidationResponse<Substance> response= validate(objnew, objold);
        for(ValidationMessage message : response.getValidationMessages()){
            callback.addMessage(message);
        }
    }

    @Override
    public ValidationResponse<Substance> validate(Substance objnew, Substance objold) {
        List<ImportSubstanceReference> missing= findMissingDefinitionalSubstances(objnew);
        missing.addAll(findMissingRelatedSubstances(objnew));
        if( !missing.isEmpty()){
            ValidationResponse<Substance> response= new ValidationResponse<>();
            missing.forEach(i->{
                ValidationMessage message = GinasProcessingMessage.ERROR_MESSAGE(String.format("Substance %s (ID: %s), listed as %s is missing",
                                i.getRefPname(), i.getRefuuid(), i.getRole().toLowerCase()));
                response.addValidationMessage(message);
            });
                return response;
        }
        return new ValidationResponse<Substance>();
        //return super.validate(objnew, objold);
    }

    private List<ImportSubstanceReference> findMissingDefinitionalSubstances(Substance substance)  {
        log.trace("starting in checkDefinitionalSubstances ");
        List<ImportSubstanceReference> missings = new ArrayList<>();
        for (SubstanceReference ref : substance.getDependsOnSubstanceReferences()) {
            log.trace("before retrieval");
            if(!substanceRepository.exists(ref)) {
                ImportSubstanceReference refToMessing = new ImportSubstanceReference(ref, SUBSTANCE_REFERENCE_DEFINITIONAL);
                missings.add(refToMessing);
            }
        }
        return missings;
    }

    private List<ImportSubstanceReference> findMissingRelatedSubstances(Substance substance) {
        log.trace("starting in findMissingRelatedSubstances");
        List<ImportSubstanceReference> missing = new ArrayList<>();
        for (Relationship rel : substance.relationships) {
            if (!isSameSubstance(substance, rel.relatedSubstance)&& rel.relatedSubstance != null && rel.relatedSubstance.refuuid != null && !substanceRepository.exists(rel.relatedSubstance)) {
                ImportSubstanceReference refToMissing = new ImportSubstanceReference(rel.relatedSubstance, SUBSTANCE_REFERENCE_RELATED);
                missing.add(refToMissing);
            }
            if (includeMediatorSubstances && rel.mediatorSubstance != null) {
                if ((rel.mediatorSubstance.refuuid != null && !substanceRepository.exists(rel.mediatorSubstance))) {
                    ImportSubstanceReference refToMissing = new ImportSubstanceReference(rel.relatedSubstance, SUBSTANCE_REFERENCE_MEDIATOR);
                    missing.add(refToMissing);
                }
            }
        }
        return missing;
    }

    private boolean isSameSubstance(Substance substance, SubstanceReference reference){
        return(substance.uuid!=null && reference.refuuid !=null && substance.uuid.toString().equals(reference.refuuid));
    }
}
