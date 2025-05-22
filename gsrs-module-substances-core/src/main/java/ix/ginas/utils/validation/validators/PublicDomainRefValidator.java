package ix.ginas.utils.validation.validators;

import gsrs.validator.ValidatorConfig;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.ValidatorPlugin;

/**
 * Created by katzelda on 5/16/18.
 */
public class PublicDomainRefValidator implements ValidatorPlugin<Substance> {
	
	private final String PublicDomainRefValidatorTagError = "PublicDomainRefValidatorTagError";
	private final String PublicDomainRefValidatorNameError = "PublicDomainRefValidatorNameError";
	
    @Override
    public boolean supports(Substance newValue, Substance oldValue, ValidatorConfig.METHOD_TYPE methodType) {
        return methodType != ValidatorConfig.METHOD_TYPE.BATCH;
    }

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {
        if (objnew.isPublic()){
            boolean allowed = objnew.references.stream()
                    .filter(Reference::isPublic)
                    .filter(Reference::isPublicDomain)
                    .filter(Reference::isPublicReleaseReference)
                    .findAny()
                    .isPresent();
            if (!allowed) {
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE(PublicDomainRefValidatorTagError, "Public records must have a PUBLIC DOMAIN reference with a '" + 
                            		Reference.PUBLIC_DOMAIN_REF + "' tag"));

            }
            objnew.getDisplayName().ifPresent(dn->{
                if(!dn.isPublic()){
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE(PublicDomainRefValidatorNameError, "Display name \"" + dn.getName() + "\" must be public if the full record is public."));
                }
            });
        }
    }
}
