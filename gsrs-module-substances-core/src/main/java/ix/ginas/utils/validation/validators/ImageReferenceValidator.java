package ix.ginas.utils.validation.validators;

import gsrs.module.substance.utils.ImageUtilities;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.PolymerSubstance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

/*
Iterate over references.
Looking for these potential issues:
1) more than one 'image reference' (tag of 'SUBSTANCE IMAGE' and file attachment)
2) image reference attached to relationship
 */
@Slf4j
public class ImageReferenceValidator extends AbstractValidatorPlugin<Substance> {
	
	private String ImageReferenceValidatorError1 = "ImageReferenceValidatorError1";
	private String ImageReferenceValidatorError2 = "ImageReferenceValidatorError2";
	private String ImageReferenceValidatorError3 = "ImageReferenceValidatorError3";
	
	
    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        log.trace("starting in ImageReferenceValidator");
        int totalImageRefs = 0;
        boolean hasImageRefOnRelationship = false;
        for(Reference reference : objnew.references) {
            if(ImageUtilities.isImageReference(reference)){
                log.trace("found image reference");
                totalImageRefs++;
                if( objnew.relationships.stream().anyMatch(r->r.getReferences().stream().anyMatch(ref->ref.getValue().equals(reference.getUuid().toString())))){
                    hasImageRefOnRelationship=true;
                }
            }
        }
        if(totalImageRefs>1){
            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(ImageReferenceValidatorError1, 
            		"Substance has more than one image reference!"));
        }
        if(hasImageRefOnRelationship){
            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(ImageReferenceValidatorError2, 
            		"Substance has at least one image reference on a relationship.  This can have unintended consequences!"));
        }
        if( totalImageRefs>0 && (objnew instanceof ChemicalSubstance || objnew instanceof PolymerSubstance)) {
            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(ImageReferenceValidatorError3, 
            		"It is not customary to set a customized image for Chemicals or Polymers!"));
        }
    }

}
