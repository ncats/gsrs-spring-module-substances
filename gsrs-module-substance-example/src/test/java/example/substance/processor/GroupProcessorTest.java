package example.substance.processor;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.cv.ControlledVocabularyEntityService;
import gsrs.cv.ControlledVocabularyEntityServiceImpl;
import gsrs.cv.CvApiAdapter;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.cv.api.GsrsControlledVocabularyDTO;
import gsrs.cv.api.GsrsVocabularyTermDTO;
import gsrs.module.substance.processors.GroupProcessor;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Group;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 *
 * @author mitch
 */
@Slf4j
@Import(GroupProcessorTest.TestConfig.class)
public class GroupProcessorTest extends AbstractSubstanceJpaEntityTest {

    public GroupProcessorTest() {
    }

    private GroupProcessor processor;

    private final String ACCESS_DOMAIN = "ACCESS_GROUP";

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
        processor = new GroupProcessor();
        AutowireHelper.getInstance().autowire(processor);

        createCv();
        log.debug("completed setup");
    }

    private void createCv() throws IOException {
        log.trace("starting in createCv");
        String accessGroup = "protected";
        List<GsrsVocabularyTermDTO> list = new ArrayList<>();
        list.add(GsrsVocabularyTermDTO.builder()
                .display(accessGroup)
                .value(accessGroup)
                .hidden(true)
                .build());

        GsrsControlledVocabularyDTO vocab =GsrsControlledVocabularyDTO.builder()
                .domain(ACCESS_DOMAIN)
                .terms(list)
                .vocabularyTermType("ix.ginas.models.v1.ControlledVocabulary") // "gsrs.cv.api.GsrsVocabularyTermDTO")//ix.ginas.models.v1.VocabularyTerm"
                .build();
                
        controlledVocabularyApi.create(vocab);
        log.trace("createCv worked");
    }

    @Autowired(required = true)
    private ControlledVocabularyApi controlledVocabularyApi;

    @Test
    public void testProcessGroup() throws IOException {
        Optional<GsrsControlledVocabularyDTO> optVocab= controlledVocabularyApi.findByDomain(ACCESS_DOMAIN);
        GsrsControlledVocabularyDTO vocab= optVocab.get();
        long totalBefore =vocab.getTerms().size();
        Group labWorkers= new Group();
        labWorkers.name="Lab Workers";
        processor.prePersist(labWorkers);
        long totalAfter = ((GsrsControlledVocabularyDTO) controlledVocabularyApi.findByDomain(ACCESS_DOMAIN).get()).getTerms().size();
        Assertions.assertEquals(totalAfter, (totalBefore+1));
    }

}
