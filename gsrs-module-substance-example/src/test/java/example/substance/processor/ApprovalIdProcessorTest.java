package example.substance.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import gsrs.springUtils.AutowireHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.module.substance.processors.ApprovalIdProcessor;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 *
 * @author mitch miller
 */
@Slf4j
public class ApprovalIdProcessorTest extends AbstractSubstanceJpaEntityTest {


    ApprovalIdProcessor processor;

    @BeforeEach
    public void setup(){
        processor = new ApprovalIdProcessor();
        processor.setCodeSystem("FDA UNII");

        AutowireHelper.getInstance().autowire(processor);
    }

    /**
    the substance read in from file has an approval ID but no corrresponding code.  We expect one to be created
     */
    @Test
    public void testCopyCodeIfNecessary() throws IOException {
        String unii = "88ECG9H7RA_minus_code";
        Substance approvedSubstance = getSubstanceFromFile(unii);
        //Setup directly
        Map<String, String> mmap = new HashMap<>();
        mmap.put("codeSystem", "FDA UNII");

        processor.copyCodeIfNecessary(approvedSubstance);

        assertTrue(approvedSubstance.codes.stream().anyMatch(c
                -> (c.codeSystem.equals(processor.getCodeSystem()) && c.code.equals(approvedSubstance.approvalID))));
    }

    /**
    process a substance without an approval ID, but otherwise identical to what we had before.
    Expect no new code
     */
    @Test
    public void testCopyCodeIfNecessaryNoApproval() {
        log.trace("testCopyCodeIfNecessaryApproval");
        Substance substance = getSubstanceFromFile("88ECG9H7RA_no_approval_id");

        int codesBefore = substance.codes.size();
        log.trace("code type " + processor.getCodeSystem() + " for approval id: " + substance.approvalID);
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


        int codesBefore = substance.codes.size();
        log.trace("code type " + processor.getCodeSystem() + " for approval id: " + substance.approvalID);
        processor.copyCodeIfNecessary(substance);
        int codesAfter = substance.codes.size();
        assertEquals(codesBefore, codesAfter);
    }

    private ProteinSubstance getSubstanceFromFile(String name) {
        try {
            File proteinFile = new ClassPathResource("testJSON/" + name + ".json").getFile();
            ProteinSubstanceBuilder builder = SubstanceBuilder.from(proteinFile);
            return builder.build();
        } catch (IOException ex) {
            log.error("Error retrieving substance from file", ex);
        }
        return null;
    }

}
