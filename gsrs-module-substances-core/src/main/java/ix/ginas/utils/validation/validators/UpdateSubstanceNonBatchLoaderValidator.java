package ix.ginas.utils.validation.validators;


import gsrs.repository.UserProfileRepository;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.SubstanceApprovalIdGenerator;
import ix.ginas.utils.validation.ValidatorPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;

/**
 * Created by katzelda on 5/16/18.
 */
@Slf4j
public class UpdateSubstanceNonBatchLoaderValidator implements ValidatorPlugin<Substance> {

    @Autowired
    private AuditorAware<Principal> auditorAware;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private SubstanceApprovalIdGenerator substanceApprovalIdGenerator;
    
    private final String UpdateSubstanceNonBatchLoaderValidatorClassWarning = "UpdateSubstanceNonBatchLoaderValidatorClassWarning";
    private final String UpdateSubstanceNonBatchLoaderValidatorVersionError = "UpdateSubstanceNonBatchLoaderValidatorVersionError";
    private final String UpdateSubstanceNonBatchLoaderValidatorRoleError1 = "UpdateSubstanceNonBatchLoaderValidatorRoleError1";
    private final String UpdateSubstanceNonBatchLoaderValidatorRoleError2 = "UpdateSubstanceNonBatchLoaderValidatorRoleError2";
    private final String UpdateSubstanceNonBatchLoaderValidatorApprovalIDWarning1 = "UpdateSubstanceNonBatchLoaderValidatorApprovalIDWarning1";
    private final String UpdateSubstanceNonBatchLoaderValidatorApprovalIDWarning2 = "UpdateSubstanceNonBatchLoaderValidatorApprovalIDWarning2";
    private final String UpdateSubstanceNonBatchLoaderValidatorApprovalIDError1 = "UpdateSubstanceNonBatchLoaderValidatorApprovalIDError1";
    private final String UpdateSubstanceNonBatchLoaderValidatorApprovalIDError2 = "UpdateSubstanceNonBatchLoaderValidatorApprovalIDError2";
    
    @Override
    public boolean supports(Substance newValue, Substance oldValue, ValidatorConfig.METHOD_TYPE methodType) {
        return methodType != ValidatorConfig.METHOD_TYPE.BATCH && oldValue !=null;
    }

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {

        if(!objnew.getClass().equals(objold.getClass())){
            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE(UpdateSubstanceNonBatchLoaderValidatorClassWarning, 
            		"Substance class should not typically be changed"));
        }

        log.debug("old version = " +objold.version +  " new version = " + objnew.version);
        if(!objold.version.equals(objnew.version)){
            callback.addMessage(
                GinasProcessingMessage
                    .ERROR_MESSAGE(UpdateSubstanceNonBatchLoaderValidatorVersionError,
                        "Substance version '" + objnew.version +"', does not match the stored version '" + objold.version + "', record may have been changed while being updated"));
        }
        UserProfile up =auditorAware.getCurrentAuditor()
                .map(p->userProfileRepository.findByUser_UsernameIgnoreCase(p.username))
                .filter(oo->oo!=null)
                .map(oo->oo.standardize())
                .orElseGet(()->UserProfile.GUEST());
        if (objnew.isPublic() && !objold.isPublic()) {


            if (!(up.hasRole(Role.Admin) || up.hasRole(Role.SuperUpdate))) {
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(UpdateSubstanceNonBatchLoaderValidatorRoleError1, 
                		"Only superUpdate users can make a substance public"));
            }
        }


        //Making a change to a validated record
        if (objnew.isValidated()) {
            if (!(up.hasRole(Role.Admin) || up.hasRole(Role.SuperUpdate))) {
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE(UpdateSubstanceNonBatchLoaderValidatorRoleError2,
                		"Only superUpdate users can update approved substances"));
            }
        }


        //Changed approvalID
        if (objold.approvalID != null) {
            if (!objold.approvalID.equals(objnew.approvalID)) {
                // Can't change approvalID!!! (unless admin)
                if (up.hasRole(Role.Admin)) {
                    //GSRS-638 removing an approval ID makes the new id null
                    if (objnew.approvalID == null) {
                        callback.addMessage(GinasProcessingMessage
                                .WARNING_MESSAGE(UpdateSubstanceNonBatchLoaderValidatorApprovalIDWarning1, 
                                		"The approvalID for the record has been removed. Was ('" + objold.approvalID + "'). This is strongly discouraged."));
                    } else {
                        if(!substanceApprovalIdGenerator.isValidId(objnew.approvalID)){
                            callback.addMessage(GinasProcessingMessage
                                    .ERROR_MESSAGE(UpdateSubstanceNonBatchLoaderValidatorApprovalIDError1, "The approvalID for the record has changed. Was ('" + 
                                    		objold.approvalID + "') but now is ('" + objnew.approvalID + "'). This approvalID is either a duplicate or invalid."));
                        } else {
                            callback.addMessage(GinasProcessingMessage
                                    .WARNING_MESSAGE(UpdateSubstanceNonBatchLoaderValidatorApprovalIDWarning2, "The approvalID for the record has changed. Was ('" + 
                                    		objold.approvalID + "') but now is ('" + objnew.approvalID + "'). This is strongly discouraged."));
                        }
                    }
                } else{
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE(UpdateSubstanceNonBatchLoaderValidatorApprovalIDError2, "The approvalID for the record has changed. Was ('" + 
                            		objold.approvalID + "') but now is ('" + objnew.approvalID +"'). This is not allowed, except by an admin."));
                }

            }
        }
    }
}
