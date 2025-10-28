package ix.ginas.utils.validation.validators;

import gsrs.security.GsrsSecurityUtils;
import gsrs.security.UserRoleConfiguration;
import gsrs.services.PrivilegeService;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Role;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by katzelda on 5/14/18.
 */
public class NewSubstanceNonBatchLoadValidator extends AbstractValidatorPlugin<Substance> {

    @Autowired
    PrivilegeService privilegeService;

    private final String REQUIRED_PRIVILEGE_FOR_PUBLIC_DATA = "Edit Public Data";

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {

        if (objnew.isPublic()) {
            //if (!GsrsSecurityUtils.hasAnyRoles(Role.Admin,Role.SuperDataEntry)) {
            if(privilegeService.canUserPerform(REQUIRED_PRIVILEGE_FOR_PUBLIC_DATA) != UserRoleConfiguration.PermissionResult.MayPerform) {
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("Only superDataEntry users can make a substance public"));
            }
        }
        if(objnew.approvalID!=null){
            callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("Cannot give an approvalID to a new substance"));
        }
    }

    @Override
    public boolean supports(Substance newValue, Substance oldValue, ValidatorConfig.METHOD_TYPE methodType) {
        return methodType != ValidatorConfig.METHOD_TYPE.BATCH && oldValue ==null;
    }

}
