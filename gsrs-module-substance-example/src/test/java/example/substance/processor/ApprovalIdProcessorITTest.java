package example.substance.processor;

import gsrs.module.substance.processors.ApprovalIdProcessor;
import gsrs.repository.ControlledVocabularyRepository;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.models.v1.ControlledVocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

public class ApprovalIdProcessorITTest extends example.substance.AbstractSubstanceJpaEntityTest {


    private ApprovalIdProcessor processor;


    private ControlledVocabularyRepository cvRepository;

    @BeforeEach
    public void setup(){
        processor = new ApprovalIdProcessor();
        processor.setCodeSystem("FDA UNII");

        AutowireHelper.getInstance().autowire(processor);
    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void loadSubstanceWithCodeAlreadyPresent(){
        ControlledVocabulary cv = new ControlledVocabulary();
        cv.domain = "CODE_SYSTEM";
//        cv.addTerms();
    }
}
