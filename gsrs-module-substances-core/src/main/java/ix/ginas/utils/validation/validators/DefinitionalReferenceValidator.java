package ix.ginas.utils.validation.validators;

import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.GinasAccessReferenceControlled;
import ix.ginas.models.GinasSubstanceDefinitionAccess;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/*
Make sure that a substance that is marked as public has at least one definitional reference 
(a reference on the part of the object that make it what it is, for example, structure for a chemical)
that is also marked public.
 */
@Slf4j
public class DefinitionalReferenceValidator extends AbstractValidatorPlugin<Substance>{
	
	private String DefinitionalReferenceValidatorReferenceError = "DefinitionalReferenceValidatorReferenceError";

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        GinasAccessControlled definitionalPart = (objnew instanceof GinasSubstanceDefinitionAccess)
                ? ((GinasSubstanceDefinitionAccess) objnew).getDefinitionElement() : null;
        //we don't expect definitionalPart to be null but including a check just to be sure
        if (objnew.isPublic() && !objnew.substanceClass.equals(Substance.SubstanceClass.concept)
                && definitionalPart != null && definitionalPart.getAccess().isEmpty()
                ) {
            log.trace("in DefinitionalReferenceValidator with public substance.  class: " + objnew.substanceClass);
            Stream<Reference> defRefs = getDefinitionalReferences(objnew);
            boolean allowed = defRefs
                            .filter(Reference::isPublic)
                            .filter(Reference::isPublicDomain)
                            .findAny()
                            .isPresent();
            log.trace("     allowed: " + allowed);
            if (!allowed) {
                callback.addMessage(GinasProcessingMessage
                                .ERROR_MESSAGE(DefinitionalReferenceValidatorReferenceError, 
                                		"Public substance definitions require a public definitional reference.  Please add one."));
            }
        }
    }
    private Stream<Reference> getDefinitionalReferences(Substance sub) {
        if (sub == null) {
            log.debug("substance is null");
            return Stream.empty();
        }
        Set<UUID> referenceIds = null;
        GinasAccessControlled definitionalPart = (sub instanceof GinasSubstanceDefinitionAccess)
                        ? ((GinasSubstanceDefinitionAccess) sub).getDefinitionElement() : null;
        if (definitionalPart != null && definitionalPart instanceof GinasAccessReferenceControlled) {
            log.trace(" definitionalPart not null");
            referenceIds = ((GinasAccessReferenceControlled) definitionalPart).getReferencesAsUUIDs();
        }
        else {
            log.warn("  definitionalPart not usable for references. ");
        }
        if (referenceIds != null && !referenceIds.isEmpty() && sub.references != null && !sub.references.isEmpty()) {
            log.trace("in DefinitionalReferenceValidator.getDefinitionalReferences found some definitional references");
            final Set<UUID> finalReferenceIds = referenceIds;
            return sub.references.stream().filter(r -> finalReferenceIds.contains(r.uuid));
        }
        return Stream.empty();
    }
}
