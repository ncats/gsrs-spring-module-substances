package ix.ginas.utils.validation.validators;

import gsrs.module.substance.utils.ImageUtilities;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
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
            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE("Substance has more than one image reference!"));
        }
        if(hasImageRefOnRelationship){
            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE("Substance has at least one image reference on a relationship.  This can have unintended consequences!"));
        }
    }

}
