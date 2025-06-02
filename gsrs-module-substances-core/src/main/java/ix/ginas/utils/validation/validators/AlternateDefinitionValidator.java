package ix.ginas.utils.validation.validators;

import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;

import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.AbstractValidatorPlugin;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by katzelda on 5/14/18.
 */
public class AlternateDefinitionValidator extends AbstractValidatorPlugin<Substance> {
	
	
	private final String AlternateDefinitionValidatorConceptError = "AlternateDefinitionValidatorConceptError";
	private final String AlternateDefinitionValidatorNameError = "AlternateDefinitionValidatorNameError";
	private final String AlternateDefinitionValidatorCodeError = "AlternateDefinitionValidatorCodeError";
	private final String AlternateDefinitionValidatorRelationshipError1 = "AlternateDefinitionValidatorRelationshipError1";
	private final String AlternateDefinitionValidatorRelationshipError2 = "AlternateDefinitionValidatorRelationshipError2";
	private final String AlternateDefinitionValidatorPrimaryError1 = "AlternateDefinitionValidatorPrimaryError1";
	private final String AlternateDefinitionValidatorPrimaryError2 = "AlternateDefinitionValidatorPrimaryError2";
	private final String AlternateDefinitionValidatorPrimaryError3 = "AlternateDefinitionValidatorPrimaryError3";
	private final String AlternateDefinitionValidatorPrimaryError4 = "AlternateDefinitionValidatorPrimaryError4";
	
    @Autowired
    private SubstanceRepository substanceRepository;

    @Override
    public void validate(Substance s, Substance objold, ValidatorCallback callback) {
        if (s.substanceClass == Substance.SubstanceClass.concept) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE(AlternateDefinitionValidatorConceptError,
                    		"Alternative definitions cannot be \"concepts\""));
        }
        if (s.names != null && !s.names.isEmpty()) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE(AlternateDefinitionValidatorNameError,
                    		"Alternative definitions cannot have names"));
        }
        if (s.codes != null && !s.codes.isEmpty()) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE(AlternateDefinitionValidatorCodeError, 
                    		"Alternative definitions cannot have codes"));
        }
        if (s.relationships == null || s.relationships.isEmpty()) {
            callback.addMessage(GinasProcessingMessage
                    .ERROR_MESSAGE(AlternateDefinitionValidatorRelationshipError1,
                    		"Alternative definitions must specify a primary substance"));
        } else {
            if (s.relationships.size() > 1) {
                callback.addMessage(GinasProcessingMessage
                        .ERROR_MESSAGE(AlternateDefinitionValidatorRelationshipError2, 
                        		"Alternative definitions may only have 1 relationship (to the parent definition), found:" + 
                        				s.relationships.size()));
            } else {
                SubstanceReference sr = s.getPrimaryDefinitionReference();
                if (sr == null) {
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE(AlternateDefinitionValidatorPrimaryError1,
                            		"Alternative definitions must specify a primary substance"));
                } else {
                    Substance subPrimary = substanceRepository.findBySubstanceReference(sr);
                    if (subPrimary == null) {
                        callback.addMessage(GinasProcessingMessage
                                .ERROR_MESSAGE(AlternateDefinitionValidatorPrimaryError2, 
                                		"Primary definition for " + sr.refPname +" (" + sr.refuuid + ") not found"));
                    } else {
                        if (subPrimary.definitionType != Substance.SubstanceDefinitionType.PRIMARY) {
                            callback.addMessage(GinasProcessingMessage
                                    .ERROR_MESSAGE(AlternateDefinitionValidatorPrimaryError3,
                                    		"Cannot add alternative definition for " + sr.refPname 
                                    		+ " (" + sr.refuuid +"). That definition is not primary."));
                        } else {
                            if (subPrimary.substanceClass == Substance.SubstanceClass.concept) {
                                callback.addMessage(GinasProcessingMessage
                                        .ERROR_MESSAGE(AlternateDefinitionValidatorPrimaryError4, 
                                        		"Cannot add alternative definition for " + sr.refPname + 
                                        		" (" + sr.refuuid +"). That definition is not definitional substance record."));
                            } else {
                             //Everything is okay   
                            }
                        }
                    }
                }
            }
        }

    }
}
