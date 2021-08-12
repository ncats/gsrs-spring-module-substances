package example.substance.processor;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.module.substance.processors.ApprovalIdProcessor;
import gsrs.startertests.TestEntityProcessorFactory;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author mitch miller
 */
@Slf4j
public class ApprovalIdProcessorTest extends AbstractSubstanceJpaEntityTest {

    public ApprovalIdProcessorTest() {
    }

    @Autowired
    TestEntityProcessorFactory processorFactory;

    /*
    the substance read in from file has an approval ID but no corrresponding code.  We expect one to be created
    */
    @Test
    public void testCopyCodeIfNecessary() throws IOException {
        log.trace("testCopyCodeIfNecessary");
        String unii="88ECG9H7RA_minus_code";
        Substance approvedSubstance = getSubstanceFromFile(unii);
        ApprovalIdProcessor processor = new ApprovalIdProcessor();
        log.trace("code type " + processor.getCodeSystem()+ " for approval id: " + approvedSubstance.approvalID);
        processor.copyCodeIfNecessary(approvedSubstance);
       
        assertTrue( approvedSubstance.codes.stream().anyMatch(c
                -> (c.codeSystem.equals(processor.getCodeSystem()) && c.code.equals(approvedSubstance.approvalID))));
    }
    
    /*
    process a substance without an approval ID, but otherwise identical to what we had before. 
    Expect no new code
    */
    @Test
    public void testCopyCodeIfNecessaryNoApproval() {
        log.trace("testCopyCodeIfNecessaryApproval");
        Substance substance = getSubstanceFromFile("88ECG9H7RA_no_approval_id");
        ApprovalIdProcessor processor = new ApprovalIdProcessor();
        int codesBefore = substance.codes.size();
        log.trace("code type " + processor.getCodeSystem()+ " for approval id: " + substance.approvalID);
        processor.copyCodeIfNecessary(substance);
        int codesAfter = substance.codes.size();
        assertFalse(substance.codes.stream().anyMatch(c
                -> (c.codeSystem.equals(processor.getCodeSystem()) && c.code.equals(substance.approvalID))));
        assertEquals(codesBefore, codesAfter);
    }

        /*
    process a substance without an approval ID, but otherwise identical to what we had before. 
    Expect no new code
    */
    @Test
    public void testCopyCodeNotNecessary() {
        log.trace("testCopyCodeNotNecessary");
        Substance substance = getSubstanceFromFile("88ECG9H7RA");
        ApprovalIdProcessor processor = new ApprovalIdProcessor();
        int codesBefore = substance.codes.size();
        log.trace("code type " + processor.getCodeSystem()+ " for approval id: " + substance.approvalID);
        processor.copyCodeIfNecessary(substance);
        int codesAfter = substance.codes.size();
        assertEquals(codesBefore, codesAfter);
    }


    private ProteinSubstance getSubstanceFromFile(String name) {
        try {
            File proteinFile = new ClassPathResource("testJSON/" + name +".json").getFile();
            ProteinSubstanceBuilder builder = SubstanceBuilder.from(proteinFile);
            return builder.build();
        } catch (IOException ex) {
            log.error("Error retrieving substance from file", ex);
        }
        return null;
    }

}
