package example.substance.processor;

import example.substance.AbstractSubstanceJpaEntityTest;
import gov.nih.ncats.common.sneak.Sneak;
import gsrs.cv.ControlledVocabularyEntityService;
import gsrs.cv.ControlledVocabularyEntityServiceImpl;
import gsrs.cv.CvApiAdapter;
import gsrs.cv.api.CodeSystemTermDTO;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.cv.api.GsrsCodeSystemControlledVocabularyDTO;
import gsrs.module.substance.processors.UniqueCodeGenerator;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityProcessor;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.utils.CodeSequentialGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 *
 * @author mitch
 */
@Slf4j
@Import(UniqueCodeGeneratorTest.TestConfig.class)
public class UniqueCodeGeneratorTest extends AbstractSubstanceJpaEntityTest {

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

    @Autowired
    private ControlledVocabularyApi controlledVocabularyApi;

    private final String codeSystemName = "NCATSID";
    
    @BeforeEach
    public void setup() throws IOException {
        log.trace("starting in setup");

        createCv();
        log.debug("completed setup");
    }

    private void createCv() throws IOException {
        log.trace("starting in createCv");
        String accessGroup = "protected";
        List<CodeSystemTermDTO> list = new ArrayList<>();
        list.add(CodeSystemTermDTO.builder()
                .display(accessGroup)
                .value(accessGroup)
                .hidden(true)
                .build());

        GsrsCodeSystemControlledVocabularyDTO vocab = GsrsCodeSystemControlledVocabularyDTO.builder()
                .domain(CV_DOMAIN)
                .terms(list)
                .build();

        controlledVocabularyApi.create(vocab);
        log.trace("createCv worked");
    }

    @Test
    public void testSeqGen() {
        String seqGenName = "not used";
        int length = 14;
        String suffix = "SUFFIX";
        boolean padding = true;
        String codeSystem = "Codes R Us";
        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(seqGenName, length, suffix, padding, codeSystem);//removed 'last'
        ProteinSubstance substance = getSubstanceFromFile("YYD6UT8T47");
        AutowireHelper.getInstance().autowire(codeGenerator);
        codeGenerator.addCode(substance);
        Assertions.assertTrue(substance.codes.stream().anyMatch(c -> c.codeSystem.equals(codeSystem)));
    }

    @Test
    public void testCreateCodeSystem() {
        Map<String, Object> instantiationMap = new HashMap<>();
        instantiationMap.put("name", "whatever");
        instantiationMap.put("suffix", "OO");
        instantiationMap.put("length", 9);
        instantiationMap.put("codesystem", codeSystemName);
        instantiationMap.put("padding", true);
        UniqueCodeGenerator uniqueCodeGenerator = new UniqueCodeGenerator(instantiationMap);
        AutowireHelper.getInstance().autowire(uniqueCodeGenerator);

        //verify that a call to the constructor of the class results in a new term added to the CV

        TransactionTemplate transactionTemplate = new TransactionTemplate( transactionManager);
        transactionTemplate.executeWithoutResult(s->{
            try {
                uniqueCodeGenerator.initialize();
            } catch (EntityProcessor.FailProcessingException e) {
                Sneak.sneakyThrow(e);
            }
            ProteinSubstance substance = getSubstanceFromFile("YYD6UT8T47");
                    uniqueCodeGenerator.generateCodeIfNecessary(substance);
                    try {
                        Optional<GsrsCodeSystemControlledVocabularyDTO> cvOpt = controlledVocabularyApi.findByDomain(CV_DOMAIN);
                        Assertions.assertTrue(cvOpt.get().getTerms().stream().anyMatch(t -> t.getValue().equals(codeSystemName)));
                    }catch(IOException e){
                        throw new UncheckedIOException(e);
                    }
                }
        );

    }

    @Test
//    @Transactional
    public void testAddCode() {
        Map<String, Object> instantiationMap = new HashMap<>();
        instantiationMap.put("name", "whatever");
        instantiationMap.put("suffix", "OO");
        instantiationMap.put("length", 9);
        instantiationMap.put("codesystem", codeSystemName);
        instantiationMap.put("padding", true);
        UniqueCodeGenerator uniqueCodeGenerator = new UniqueCodeGenerator(instantiationMap);
        AutowireHelper.getInstance().autowire(uniqueCodeGenerator);

        //verify that a call to the constructor of the class results in a new term added to the CV
        ProteinSubstance substance = getSubstanceFromFile("YYD6UT8T47");
        uniqueCodeGenerator.prePersist(substance);
        Assertions.assertTrue(substance.codes.stream().anyMatch(c -> c.codeSystem.equals(codeSystemName)));
    }

    @Test
    public void testSkipAddingCode() {
        Map<String, Object> instantiationMap = new HashMap<>();
        instantiationMap.put("name", "whatever");
        instantiationMap.put("suffix", "OO");
        instantiationMap.put("length", 9);
        instantiationMap.put("codesystem", codeSystemName);
        instantiationMap.put("padding", true);
        UniqueCodeGenerator uniqueCodeGenerator = new UniqueCodeGenerator(instantiationMap);
        AutowireHelper.getInstance().autowire(uniqueCodeGenerator);

        //verify that a call to the constructor of the class results in a new term added to the CV
        ProteinSubstance substance = getSubstanceFromFile("YYD6UT8T47");
        //manually create a code
        Code newCode = new Code();
        newCode.codeSystem= codeSystemName;
        newCode.code="Blah";
        newCode.type="PRIMAY";
        substance.codes.add(newCode);
        int totalBefore = substance.codes.size();
        
        uniqueCodeGenerator.prePersist(substance);
        int totalAfter = substance.codes.size();
        
        Assertions.assertEquals(totalBefore, totalAfter);
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
