package example.substance.processor;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.cv.ControlledVocabularyEntityService;
import gsrs.cv.ControlledVocabularyEntityServiceImpl;
import gsrs.cv.CvApiAdapter;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.cv.api.GsrsCodeSystemControlledVocabularyDTO;
import gsrs.cv.api.GsrsControlledVocabularyDTO;
import gsrs.cv.api.GsrsVocabularyTermDTO;
import gsrs.module.substance.processors.UniqueCodeGenerator;
import gsrs.springUtils.AutowireHelper;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ProteinSubstance;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author mitch
 */
@Slf4j
@Import(UniqueCodeGeneratorTest.TestConfig.class)
public class UniqueCodeGeneratorTest extends AbstractSubstanceJpaEntityTest{

    private UniqueCodeGenerator processor;

    private final String CV_DOMAIN = "CODE_SYSTEM";

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

    @Autowired(required = true)
    private ControlledVocabularyApi controlledVocabularyApi;

    @BeforeEach
    public void setup() throws IOException {
        log.trace("starting in setup");

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

        GsrsControlledVocabularyDTO vocab = GsrsControlledVocabularyDTO.builder()
                .domain(CV_DOMAIN)
                .terms(list)
                .vocabularyTermType("ix.ginas.models.v1.ControlledVocabulary")
                .build();

        controlledVocabularyApi.create(vocab);
        log.trace("createCv worked");
    }

    @Test
    public void testCreateCodeSystem() {
        //verify that a call to the constructor of the class results in a new term added to the CV
        String codeSystemName ="NCATSID";
        Map<String, Object> instantiationMap = new HashMap<>();
        instantiationMap.put("name", "whatever");
        instantiationMap.put("suffix", "OO");
        instantiationMap.put("length", 9);
        instantiationMap.put("codesystem", codeSystemName);
        instantiationMap.put("padding", true);
        
        UniqueCodeGenerator generator = new UniqueCodeGenerator(instantiationMap);
        //AutowireHelper.getInstance().autowire(processor);
        ProteinSubstance substance= getSubstanceFromFile("YYD6UT8T47");
        generator.prePersist(substance);
        Optional<GsrsCodeSystemControlledVocabularyDTO> cvOpt=null;
        try {
            //now check for a CV term with the expected value
            cvOpt= controlledVocabularyApi.findByDomain(CV_DOMAIN);
            
        } catch (IOException ex) {
            log.error("Error during cv retrieval", ex);
        }
        
        boolean anyMatch = cvOpt.get().getTerms().stream().anyMatch(t-> t.getValue().equals(codeSystemName));
        Assertions.assertTrue(anyMatch);
        
        Assertions.assertTrue(substance.codes.stream().anyMatch(c->c.codeSystem.equals(codeSystemName)));
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
