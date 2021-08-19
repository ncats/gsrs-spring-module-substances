package example.substance.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;

import gsrs.cv.ControlledVocabularyEntityService;
import gsrs.cv.ControlledVocabularyEntityServiceImpl;
import gsrs.cv.CvApiAdapter;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.springUtils.AutowireHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.cv.api.CodeSystemTermDTO;
import gsrs.cv.api.GsrsCodeSystemControlledVocabularyDTO;
import gsrs.module.substance.processors.ApprovalIdProcessor;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Substance;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 *
 * @author mitch miller
 */
@Slf4j
@Import(ApprovalIdProcessorTest.TestConfig.class)
public class ApprovalIdProcessorTest extends AbstractSubstanceJpaEntityTest {

    ApprovalIdProcessor processor;

    @TestConfiguration
    static class TestConfig {

        @Bean
        public ControlledVocabularyEntityService controlledVocabularyEntityService() {
            return new ControlledVocabularyEntityServiceImpl();
        }

        @Bean
        public ControlledVocabularyApi controlledVocabularyApi(@Autowired ControlledVocabularyEntityService service) {
            return new CvApiAdapter(service);
        }
    }

    @BeforeEach
    public void setup() throws IOException {
        log.trace("starting in setup");
        processor = new ApprovalIdProcessor();
        processor.setCodeSystem("FDA UNII");

        AutowireHelper.getInstance().autowire(processor);

        createCv();
    }

    private void createCv() throws IOException {
        log.trace("starting in createCv");
        String basicCodeSystem = "CAS";
        List<CodeSystemTermDTO> list = new ArrayList<>();
        list.add(CodeSystemTermDTO.builder()
                .display(basicCodeSystem)
                .value(basicCodeSystem)
                .hidden(true)
                .build());

        controlledVocabularyApi.create(GsrsCodeSystemControlledVocabularyDTO.builder()
                .domain("CODE_SYSTEM")
                .terms(list)
                .vocabularyTermType("ix.ginas.models.v1.CodeSystemControlledVocabulary")
                .build());
        log.trace("createCv worked");
    }

    @Autowired
    ControlledVocabularyApi controlledVocabularyApi;

    /**
     * the substance read in from file has an approval ID but no corresponding
     * code.We expect one to be created
     *
     * @throws java.io.IOException
     */
    @Test
    public void testCopyCodeIfNecessary() throws IOException {
        String unii = "88ECG9H7RA_minus_code";
        Substance approvedSubstance = getSubstanceFromFile(unii);

        long totalCVsBefore = controlledVocabularyApi.count();

        processor.prePersist(approvedSubstance);
        long totalCVsAfter = controlledVocabularyApi.count();

        assertTrue(approvedSubstance.codes.stream().anyMatch(c
                -> (c.codeSystem.equals(processor.getCodeSystem()) && c.code.equals(approvedSubstance.approvalID))));
        assertEquals(totalCVsBefore, totalCVsAfter);
    }

    /**
     * process a substance without an approval ID, but otherwise identical to
     * what we had before. Expect no new code
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
