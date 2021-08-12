package example.substance.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.entityProcessor.ConfigBasedEntityProcessorFactory;
import gsrs.module.substance.processors.ApprovalIdProcessor;
import ix.core.EntityProcessor;
import ix.core.EntityProcessor.FailProcessingException;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author mitch miller
 */
@Slf4j
public class ApprovalIdProcessorTest extends AbstractSubstanceJpaEntityTest {

    public ApprovalIdProcessorTest() {
    }

    @Autowired
    ConfigBasedEntityProcessorFactory processorFactory;

    @Test
    public void testCopyCodeIfNecessary() throws FailProcessingException {
        log.trace("testCopyCodeIfNecessary");
        Substance approvedSubstance = getProteinFromFile();
        //substanceRepository.saveAndFlush(approvedSubstance);
        EntityProcessor ep = processorFactory.getCombinedEntityProcessorFor(approvedSubstance);
        ep.prePersist(approvedSubstance);
        
        //Setup directly
        Map<String,String> mmap = new HashMap<String,String>();
        mmap.put("codeSystem", "FDA UNII");
        
        ApprovalIdProcessor processor = new ApprovalIdProcessor(mmap);
        log.trace("code type " + processor.getCodeSystem()+ " for approval id: " + approvedSubstance.approvalID);
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
