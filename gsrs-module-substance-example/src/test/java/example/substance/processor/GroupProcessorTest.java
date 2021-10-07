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
        Optional<GsrsControlledVocabularyDTO> vocabOpt= controlledVocabularyApi.findByDomain(ACCESS_DOMAIN);
        if( vocabOpt.isPresent()) {
            log.trace("CV for ACCESS_DOMAIN already exists");
            return;
        }
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
        //verify that a term is added to the CV when necessary
        GsrsControlledVocabularyDTO vocabBefore= (GsrsControlledVocabularyDTO) controlledVocabularyApi.findByDomain(ACCESS_DOMAIN).get();
        long totalBefore =vocabBefore.getTerms().size();
        Group labWorkers= new Group();
        labWorkers.name="Lab Workers";
        processor.postPersist(labWorkers);
        GsrsControlledVocabularyDTO vocabAfter = (GsrsControlledVocabularyDTO) controlledVocabularyApi.findByDomain(ACCESS_DOMAIN).get();
        Assertions.assertTrue(vocabAfter.getTerms().stream().anyMatch(t-> t.getValue().equalsIgnoreCase(labWorkers.name)));
        long totalAfter = vocabAfter.getTerms().size();
        Assertions.assertEquals(totalAfter, (totalBefore+1));
    }

     @Test
    public void testProcessGroupNotAdded() throws IOException {
        //verify that when the CV already contains an item for the group, a duplicate is NOT created
        GsrsControlledVocabularyDTO vocabBefore= (GsrsControlledVocabularyDTO) controlledVocabularyApi.findByDomain(ACCESS_DOMAIN).get();
        long totalBefore =vocabBefore.getTerms().size();
        Group protectedGroup= new Group();
        protectedGroup.name="protected";
        processor.postPersist(protectedGroup);
        GsrsControlledVocabularyDTO vocabAfter = (GsrsControlledVocabularyDTO) controlledVocabularyApi.findByDomain(ACCESS_DOMAIN).get();
        Assertions.assertEquals(1, vocabAfter.getTerms().stream().filter(t-> t.getValue().equalsIgnoreCase(protectedGroup.name)).count());
        long totalAfter = vocabAfter.getTerms().size();
        Assertions.assertEquals(totalAfter, totalBefore);
    }

}
