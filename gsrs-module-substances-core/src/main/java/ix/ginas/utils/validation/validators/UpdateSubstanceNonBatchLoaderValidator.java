package ix.ginas.utils.validation.validators;


import gsrs.repository.UserProfileRepository;
import gsrs.services.PrivilegeService;
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

    @Autowired
    private PrivilegeService privilegeService;

    @Override
    public boolean supports(Substance newValue, Substance oldValue, ValidatorConfig.METHOD_TYPE methodType) {
        return methodType != ValidatorConfig.METHOD_TYPE.BATCH && oldValue !=null;
    }

    @Override
    public void validate(Substance objnew, Substance objold, ValidatorCallback callback) {

        if(!objnew.getClass().equals(objold.getClass())){
            callback.addMessage(GinasProcessingMessage.WARNING_MESSAGE("Substance class should not typically be changed"));
        }

        log.debug("old version = " +objold.version +  " new version = " + objnew.version);
        if(!objold.version.equals(objnew.version)){
            callback.addMessage(
                GinasProcessingMessage
                    .ERROR_MESSAGE(
                        "Substance version '%s', does not match the stored version '%s', record may have been changed while being updated",
                        objnew.version, objold.version));
        }
        UserProfile up =auditorAware.getCurrentAuditor()
                .map(p->userProfileRepository.findByUser_UsernameIgnoreCase(p.username))
                .filter(oo->oo!=null)
                .map(oo->oo.standardize())
                .orElseGet(()->UserProfile.GUEST());
        if (objnew.isPublic() && !objold.isPublic()) {


            if (!privilegeService.canDo("Make Records Public")) {
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("Only users with the \"Make Records Public\" privilege can make a substance public"));
            }
        }


        //Making a change to a validated record
        if (objnew.isValidated()) {
            if (!privilegeService.canDo("Edit Approved Records")) {
                callback.addMessage(GinasProcessingMessage.ERROR_MESSAGE("Only users with the \"Edit Approved Records\" privilege can update approved substances"));
            }
        }


        //Changed approvalID
        if (objold.approvalID != null) {
            if (!objold.approvalID.equals(objnew.approvalID)) {
                // Can't change approvalID!!! (unless admin)
                if (privilegeService.canDo("Edit Approval IDs")) {
                    //GSRS-638 removing an approval ID makes the new id null
                    if (objnew.approvalID == null) {
                        callback.addMessage(GinasProcessingMessage
                                .WARNING_MESSAGE("The approvalID for the record has been removed. Was ('%s'). This is strongly discouraged.",
                                                objold.approvalID));
                    } else {
                        if(!substanceApprovalIdGenerator.isValidId(objnew.approvalID)){
                            callback.addMessage(GinasProcessingMessage
                                    .ERROR_MESSAGE("The approvalID for the record has changed. Was ('%s') but now is ('%s'). This approvalID is either a duplicate or invalid.",
                                                objold.approvalID, objnew.approvalID));
                        } else {
                            callback.addMessage(GinasProcessingMessage
                                    .WARNING_MESSAGE("The approvalID for the record has changed. Was ('%s') but now is ('%s'). This is strongly discouraged.",
                                                objold.approvalID, objnew.approvalID));
                        }
                    }
                } else{
                    callback.addMessage(GinasProcessingMessage
                            .ERROR_MESSAGE("The approvalID for the record has changed. Was ('%s') but now is ('%s'). This is not allowed, except by an admin.",
                                    objold.approvalID, objnew.approvalID));
                }

            }
        }
    }
}
