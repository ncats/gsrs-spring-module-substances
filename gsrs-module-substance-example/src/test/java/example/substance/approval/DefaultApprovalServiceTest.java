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

//Case-sensitive comparison; test data has ADMIN as created by
@WithMockUser(username = "ADMIN", roles="Admin")
@Transactional
@Slf4j
public class DefaultApprovalServiceTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    PrincipalRepository principalRepository;

    @Autowired
    private SubstanceApprovalIdGenerator substanceApprovalIdGenerator;


    @Test
    public void testApproval1() {
        Substance preApprovedSubstance = getSubstanceFromFile("invrelate1");
        AutowireHelper.getInstance().autowire(this.substanceRepository);
        AutowireHelper.getInstance().autowire(this.principalRepository);
        DefaultApprovalService approvalService = new DefaultApprovalService(substanceApprovalIdGenerator, this.substanceRepository,
                this.principalRepository);
        AutowireHelper.getInstance().autowire(approvalService);
        ApprovalService.ApprovalResult result= null;
        ApprovalService.ApprovalException exception = null;
        try {
            result =approvalService.approve(preApprovedSubstance);
        }
        catch (ApprovalService.ApprovalException ex) {
            exception=ex;
            log.error("error during approval: " + ex.getMessage());
        }
        Assertions.assertNull(exception);
    }

    @Test
    public void testApproval2() {
        Substance preApprovedSubstance = getSubstanceFromFile("PJY633525U_createdBySmith");
        AutowireHelper.getInstance().autowire(this.substanceRepository);
        AutowireHelper.getInstance().autowire(this.principalRepository);
        DefaultApprovalService approvalService = new DefaultApprovalService(substanceApprovalIdGenerator, this.substanceRepository,
                this.principalRepository);
        AutowireHelper.getInstance().autowire(approvalService);
        ApprovalService.ApprovalResult result= null;
        ApprovalService.ApprovalException exception = null;
        try {
            result =approvalService.approve(preApprovedSubstance);
            log.trace("substance approved without exception by " + result.getApprovedBy());
        }
        catch (ApprovalService.ApprovalException ex) {
            exception=ex;
            log.error("error during approval: " + ex.getMessage());
        }
        Assertions.assertNull(exception);
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
