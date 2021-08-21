package ix.ginas.utils.validation.validators;

import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.security.GsrsSecurityUtils;
import ix.core.models.Role;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import ix.ginas.utils.validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Mitch Miller
 */
@Slf4j
public class SubstanceUniquenessValidator extends AbstractValidatorPlugin<Substance> {

	private static final List<String> SUBSTANCE_CLASSES_HANDLED = Arrays.asList("chemical", "mixture",
					"structurallyDiverse", "polymer", "concept", "specifiedSubstanceG1");

    @Autowired(required = true)
    private DefinitionalElementFactory definitionalElementFactory;


	@Override
	public void validate(Substance testSubstance, Substance oldSubstance, ValidatorCallback callback) {
		log.trace(String.format("starting in SubstanceUniquenessValidator. substance type: <%s>", testSubstance.substanceClass));
        ValidationUtils validationUtils = new ValidationUtils();
		if( !SUBSTANCE_CLASSES_HANDLED.stream().anyMatch(s->s.equalsIgnoreCase(testSubstance.substanceClass.toString()))){
			log.debug("skipping this substance because of class");
			return;
		}
		if(definitionalElementFactory.computeDefinitionalElementsFor(testSubstance).getElements().isEmpty()){
			return;
		}
		
		List<Substance> fullMatches = validationUtils.findFullDefinitionalDuplicateCandidates(testSubstance);
		log.debug("total fullMatches " + fullMatches.size());
		if (fullMatches.size() > 0) {
			for (int i = 0; i < fullMatches.size(); i++) {
				Substance possibleMatch = fullMatches.get(i);

				String messageText = String.format("Substance %s (ID: %s) appears to be a full duplicate\n",
								possibleMatch.getName(), possibleMatch.uuid);
				GinasProcessingMessage mes;
				if( oldSubstance == null && !GsrsSecurityUtils.hasAnyRoles(Role.SuperUpdate, Role.SuperDataEntry)) {
					mes= GinasProcessingMessage.ERROR_MESSAGE(messageText);
				}else{
					mes= GinasProcessingMessage.WARNING_MESSAGE(messageText);
				}
                mes.addLink(createSubstanceLink(possibleMatch));
				//.createSubstanceLink((possibleMatch));
				callback.addMessage(mes);
			}
		}
		else {
			List<Substance> matches = validationUtils.findDefinitionaLayer1lDuplicateCandidates(testSubstance);
			log.debug("substance of type " + testSubstance.substanceClass.name() + " total matches: " + matches.size());
			if (matches.size() > 0) {
				for (int i = 0; i < matches.size(); i++) {
					Substance possibleMatch = matches.get(i);
					String message = String.format("Substance %s (ID: %s) is a possible duplicate\n",
									possibleMatch.getName(), possibleMatch.uuid);
					log.debug("in SubstanceUniquenessValidator, creating warning with message " + message);
					GinasProcessingMessage mes = GinasProcessingMessage.WARNING_MESSAGE(message);
					log.debug("in SubstanceUniquenessValidator after message creation");
                    mes.addLink(createSubstanceLink(possibleMatch));
					//mes.addLink(GinasUtils.createSubstanceLink(possibleMatch));
					callback.addMessage(mes);
				}
			}
		}
	}
    
    public static GinasProcessingMessage.Link createSubstanceLink(Substance s){
		GinasProcessingMessage.Link l = new GinasProcessingMessage.Link();
        //fake this for now.
		l.href= "substances/"+  s.getLinkingID();
        //todo: get the  real link
        //l.href=ix.ginas.controllers.routes.GinasApp.substance(s.getLinkingID())+"";
		l.text="[" + s.getApprovalIDDisplay() + "]" + s.getName();

		return l;
	}

}
