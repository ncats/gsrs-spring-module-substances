package ix.ginas.utils.validation.validators;

import gsrs.security.GsrsSecurityUtils;
import gsrs.springUtils.GsrsSpringUtils;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;

/**
 * Created by katzelda on 5/14/18.
 */
public class NewSubstanceNonBatchLoadValidator extends AbstractValidatorPlugin<Substance> {
	
	private final String NewSubstanceNonBatchLoadValidatorRoleError = "NewSubstanceNonBatchLoadValidatorRoleError";
	private final String NewSubstanceNonBatchLoadValidatorApprovalIDError = "NewSubstanceNonBatchLoadValidatorApprovalIDError";
	
    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {

        if (objnew.isPublic()) {
            if (!GsrsSecurityUtils.hasAnyRoles(Role.Admin,Role.SuperDataEntry)) {
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(NewSubstanceNonBatchLoadValidatorRoleError, 
                		"Only superDataEntry users can make a substance public"));
            }
        }
        if(objnew.approvalID!=null){
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(NewSubstanceNonBatchLoadValidatorApprovalIDError, 
            		"Cannot give an approvalID to a new substance"));
        }
    }

    @Override
    public boolean supports(Substance newValue, Substance oldValue, ValidatorConfig.METHOD_TYPE methodType) {
        return methodType != ValidatorConfig.METHOD_TYPE.BATCH && oldValue ==null;
    }

}
