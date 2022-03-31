package example.substance.approval;

import gsrs.module.substance.approval.ApprovalService;
import gsrs.module.substance.approval.DefaultApprovalService;
import gsrs.repository.PrincipalRepository;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.SubstanceApprovalIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;

@WithMockUser(username = "admin", roles = "Admin")
@Transactional
@Slf4j
public class DefaultApprovalServiceTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    PrincipalRepository principalRepository;

    @Autowired
    private SubstanceApprovalIdGenerator substanceApprovalIdGenerator;


    @Test
    public void testNoCreatorException() {
        Substance preApprovedSubstance = getSubstanceFromFile("invrelate1");
        AutowireHelper.getInstance().autowire(this.substanceRepository);
        AutowireHelper.getInstance().autowire(this.principalRepository);
        DefaultApprovalService approvalService = new DefaultApprovalService(substanceApprovalIdGenerator, this.substanceRepository,
                this.principalRepository);
        AutowireHelper.getInstance().autowire(approvalService);
        ApprovalService.ApprovalResult result = null;
        ApprovalService.ApprovalException exception = null;
        try {
            result = approvalService.approve(preApprovedSubstance);
        } catch (ApprovalService.ApprovalException ex) {
            exception = ex;
            log.error("error during approval: " + ex.getMessage());
        }
        Assertions.assertTrue(exception.getMessage().contains("There is no creator associated with this record"));
    }

    /*
    Current user is the last editor but not the creator -> no error
     */
    @Test
    public void testApproval2() {
        Substance preApprovedSubstance = getSubstanceFromFile("PJY633525U_createdBySmith");
        AutowireHelper.getInstance().autowire(this.substanceRepository);
        AutowireHelper.getInstance().autowire(this.principalRepository);
        DefaultApprovalService approvalService = new DefaultApprovalService(substanceApprovalIdGenerator, this.substanceRepository,
                this.principalRepository);
        AutowireHelper.getInstance().autowire(approvalService);
        ApprovalService.ApprovalResult result = null;
        ApprovalService.ApprovalException exception = null;
        try {
            result = approvalService.approve(preApprovedSubstance);
            log.trace("substance approved without exception by " + result.getApprovedBy());
        } catch (ApprovalService.ApprovalException ex) {
            exception = ex;
            log.error("error during approval: " + ex.getMessage());
        }
        Assertions.assertNull(exception);
    }

    /*
   substance is alreayd approved
    */
    @Test
    public void testApproval3() {
        Substance preApprovedSubstance = getSubstanceFromFile("PJY633525U");
        String expectedMessage = "Cannot approve an approved substance";
        AutowireHelper.getInstance().autowire(this.substanceRepository);
        AutowireHelper.getInstance().autowire(this.principalRepository);
        DefaultApprovalService approvalService = new DefaultApprovalService(substanceApprovalIdGenerator, this.substanceRepository,
                this.principalRepository);
        AutowireHelper.getInstance().autowire(approvalService);
        ApprovalService.ApprovalResult result = null;
        ApprovalService.ApprovalException exception = null;
        try {
            result = approvalService.approve(preApprovedSubstance);
            log.trace("substance approved without exception by " + result.getApprovedBy());
        } catch (ApprovalService.ApprovalException ex) {
            exception = ex;
            log.error("error during approval: " + ex.getMessage());
        }
        Assertions.assertTrue(exception.getMessage().contains(expectedMessage));
    }

    /*
   Current user is the last editor and the creator -> expect an error
   */
    @Test
    public void testApprovalErrorExpected() {
        Substance preApprovedSubstance = getSubstanceFromFile("PJY633525U_complete");
        String expectedMessage = "You cannot approve a substance if you are the last editor of the substance.";
        AutowireHelper.getInstance().autowire(this.substanceRepository);
        AutowireHelper.getInstance().autowire(this.principalRepository);
        DefaultApprovalService approvalService = new DefaultApprovalService(substanceApprovalIdGenerator, this.substanceRepository,
                this.principalRepository);
        AutowireHelper.getInstance().autowire(approvalService);
        ApprovalService.ApprovalResult result = null;
        ApprovalService.ApprovalException exception = null;
        approvalService.setApprovalDependency("LastEditedBy");
        try {
            result = approvalService.approve(preApprovedSubstance);
            log.trace("substance approved without exception by " + result.getApprovedBy());
        } catch (ApprovalService.ApprovalException ex) {
            exception = ex;
            log.error("error during approval: " + ex.getMessage());
        }
        Assertions.assertTrue(exception.getMessage().contains(expectedMessage));
    }

    /*
Current user is the last editor and the creator -> expect an error
*/
    @Test
    public void testApprovalLastEditedNoError() {
        Substance preApprovedSubstance = getSubstanceFromFile("PJY633525U_complete");
        String expectedMessage = "You cannot approve a substance if you are the creator of the substance";
        AutowireHelper.getInstance().autowire(this.substanceRepository);
        AutowireHelper.getInstance().autowire(this.principalRepository);
        DefaultApprovalService approvalService = new DefaultApprovalService(substanceApprovalIdGenerator, this.substanceRepository,
                this.principalRepository);
        AutowireHelper.getInstance().autowire(approvalService);
        ApprovalService.ApprovalResult result = null;
        ApprovalService.ApprovalException exception = null;
        approvalService.setApprovalDependency(DefaultApprovalService.ApprovalDependency.LastEditedBy);
        try {
            result = approvalService.approve(preApprovedSubstance);
            log.trace("substance approved without exception by " + result.getApprovedBy());
        } catch (ApprovalService.ApprovalException ex) {
            exception = ex;
            log.error("error during approval: " + ex.getMessage());
        }
        Assertions.assertFalse(exception.getMessage().contains(expectedMessage));
    }


    @Test
    public void testSetApprovalDependency() {
        DefaultApprovalService approvalService = new DefaultApprovalService(substanceApprovalIdGenerator, this.substanceRepository,
                this.principalRepository);
        AutowireHelper.getInstance().autowire(approvalService);
        approvalService.setApprovalDependency("CreatedBy");
        DefaultApprovalService.ApprovalDependency expected = DefaultApprovalService.ApprovalDependency.CreatedBy;
        DefaultApprovalService.ApprovalDependency actual = approvalService.getApprovalDependency();
        Assertions.assertEquals(expected, actual);
    }

    private Substance getSubstanceFromFile(String name) {
        try {
            File dataFile = new ClassPathResource("testJSON/" + name + ".json").getFile();
            ChemicalSubstanceBuilder builder = SubstanceBuilder.from(dataFile);
            return builder.build();
        } catch (IOException ex) {
            log.error("Error retrieving substance from file", ex);
        }
        return null;
    }

}