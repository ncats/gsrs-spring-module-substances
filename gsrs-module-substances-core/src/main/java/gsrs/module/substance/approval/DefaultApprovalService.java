package gsrs.module.substance.approval;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.controller.GetGsrsRestApiMapping;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.security.GsrsSecurityUtils;
import gsrs.security.hasApproverRole;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.SubstanceApprovalIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Default implementation of ApprovalService.
 * An instance is created if no other ApprovalService beans
 * are created.
 * 
 * <p/>
 * Custom approval rules can be set by extending this class and overriding 
 * {@link #extraApprovalValidation(Substance, String)} or {@link #defaultApprovalValidation(Substance, String)}.
 * 
 * @see #extraApprovalValidation(Substance, String) 
 * @see #defaultApprovalValidation(Substance, String)
 */
@Service
public class DefaultApprovalService implements ApprovalService{


    private SubstanceApprovalIdGenerator approvalIdGenerator;


    private SubstanceRepository substanceRepository;

    private PrincipalRepository principalRepository;

    @Autowired
    public DefaultApprovalService(SubstanceApprovalIdGenerator approvalIdGenerator,
                                  SubstanceRepository substanceRepository,
                                  PrincipalRepository principalRepository) {
        this.approvalIdGenerator = approvalIdGenerator;
        this.substanceRepository = substanceRepository;
        this.principalRepository = principalRepository;
    }

    /**
     * Perform any additional Validation checks before allowing the Substance to be approved.
     *
     * @implNote By basic, this method does nothing, clients may choose to override this method
     * to add custom checks beyond what is done by {@link #defaultApprovalValidation(Substance, String)}.
     *
     * @param s The Substance to be approved.
     * @param usernameOfApprover The logged in username of the person approving the Substance.
     * @throws ApprovalException if there is any reason not to approve this Substance.
     */
    protected void extraApprovalValidation(Substance s, String usernameOfApprover) throws ApprovalException{
        //basic to no-op
    }

    /**
     * Default validation checks before allowing a Substance to be approved.  While not
     * recommended, clients may choose to override this method to change the basic validation.
     * The recommended way clients should modify the validation is by overriding
     * {@link #extraApprovalValidation(Substance, String)} to add additional checks
     * which are performed AFTER this method is successfully invoked.
     *
     * @implSpec The validation checks performed are:
     * <ol>
     *     <li>the current Substance's status can not already be approved</li>
     *     <li>The Substance must have already been persisted. </li>
     *     <li>The user who last edited this substance must be different than the user who is approving this substance</li>
     *     <li>This Substance must be a Primary Definition</li>
     *     <li>This Substance can not be a Concept</li>
     *     <li>If there are any {@link SubstanceReference}s returned by {@link Substance#getDependsOnSubstanceReferences()},
     *      then each must:
     *          <ul>
     *              <li>exist in the database</li>
     *              <li>be approved</li>
     *          </ul>
     *      </li>
     *
     * </ol>
     * Any violations of these checks will throw an ApprovalException.
     *
     * @param s The Substance to be approved.
     * @param usernameOfApprover The logged in username of the person approving the Substance.
     * @throws ApprovalException if there is any reason not to approve this Substance.
     * 
     * @see #extraApprovalValidation(Substance, String)
     */
    protected void defaultApprovalValidation(Substance s, String usernameOfApprover) throws ApprovalException{
        if (Substance.STATUS_APPROVED.equals(s.status)) {
            throw new ApprovalException("Cannot approve an approved substance");
        }
        if (s.lastEditedBy == null) {
            throw new ApprovalException(
                    "There is no last editor associated with this record. One must be present to allow approval. Please contact your system administrator.");
        } else {
            if (s.lastEditedBy.username.equals(usernameOfApprover)) {
                throw new ApprovalException(
                        "You cannot approve a substance if you are the last editor of the substance.");
            }
        }
        if (!s.isPrimaryDefinition()) {
            throw new ApprovalException("Cannot approve non-primary definitions.");
        }
        if (Substance.SubstanceClass.concept.equals(s.substanceClass)) {
            throw new ApprovalException("Cannot approve non-substance concepts.");
        }
        for (SubstanceReference sr : s.getDependsOnSubstanceReferences()) {
            Optional<SubstanceRepository.SubstanceSummary> s2 = substanceRepository.findSummaryBySubstanceReference(sr);
            if (!s2.isPresent()) {
                throw new IllegalStateException("Cannot approve substance that depends on " + sr.toString()
                        + " which is not found in database.");
            }
            if (!s2.get().isValidated()) {
                throw new IllegalStateException(
                        "Cannot approve substance that depends on " + sr.toString() + " which is not approved.");
            }
        }
    }

    /**
     * Try to approve the given Substance.  The user invoking this method
     * must have Approver Role.
     * @param s the Substance to approve.
     * @return a new {@link gsrs.module.substance.approval.ApprovalService.ApprovalResult};
     * will never be null.
     * @throws NullPointerException if substance is null or fields that are checked are null.
     * @throws ApprovalException if any sort of validation check on the Substance to make sure
     * if can be approved fails validation.
     */
    @Transactional
    @hasApproverRole
    @Override
    public ApprovalResult approve(Substance s) throws ApprovalException {

        Optional<String> loggedInUsername = GsrsSecurityUtils.getCurrentUsername();
        //technically I don't think this can happen since we have the @hasApproverRole annotation check
        //to get this far we must not only be logged in but have Approver Role
        //but we need to keep track of who we are so might as well double check.
        if (!loggedInUsername.isPresent()) {
            throw new ApprovalException("Must be logged in user to approve substance");
        }

        String userName = loggedInUsername.get();
        defaultApprovalValidation(s, userName);
        extraApprovalValidation(s, userName);

        s.markApproved(approvalIdGenerator.generateId(s),
                TimeUtil.getCurrentDate(),
                principalRepository.findDistinctByUsernameIgnoreCase(userName));


        return ApprovalResult.builder()
                .approvalId(s.approvalID)
                .generatorName(approvalIdGenerator.getName())
                .substance(s)
                .approvalDateTime(TimeUtil.asLocalDateTime(s.approved))
                .approvedBy(loggedInUsername.get())
                .build();
    }
}
