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

    @Test
    public void testCopyCodeIfNecessary() {
        log.trace("testCopyCodeIfNecessary");
        Substance approvedSubstance = getProteinFromFile();
        //substanceRepository.saveAndFlush(approvedSubstance);
        ApprovalIdProcessor processor = new ApprovalIdProcessor();
        log.trace("code type for approval id: " + approvedSubstance.approvalID);
        processor.copyCodeIfNecessary(approvedSubstance);
        assertTrue( approvedSubstance.codes.stream().anyMatch(c
                -> (c.codeSystem.equals(processor.getCodeSystem()) && c.code.equals(approvedSubstance.approvalID))));
    }

    private ProteinSubstance getProteinFromFile() {
        try {
            File proteinFile = new ClassPathResource("testJSON/88ECG9H7RA.json").getFile();
            ProteinSubstanceBuilder builder = SubstanceBuilder.from(proteinFile);
            return builder.build();
        } catch (IOException ex) {
            log.error("Error retrieving substance from file", ex);
        }
        return null;
    }

}
