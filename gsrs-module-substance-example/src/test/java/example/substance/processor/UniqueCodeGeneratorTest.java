package example.substance.processor;

import gov.nih.ncats.common.sneak.Sneak;
import gsrs.cv.api.AbstractGsrsControlledVocabularyDTO;
import gsrs.cv.api.CodeSystemTermDTO;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.cv.api.GsrsCodeSystemControlledVocabularyDTO;
import gsrs.module.substance.processors.UniqueCodeGenerator;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.EntityProcessor;
import ix.core.models.Group;
import ix.ginas.models.v1.ChemicalSubstance;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

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
        public ControlledVocabularyApi controlledVocabularyApi() {
            return new InMemoryControlledVocabularyApi();
        }
    }

    private static class InMemoryControlledVocabularyApi implements ControlledVocabularyApi {
        private final Map<String, AbstractGsrsControlledVocabularyDTO> vocabulariesByDomain = new HashMap<>();

        @Override
        public <T extends AbstractGsrsControlledVocabularyDTO> Optional<T> findByDomain(String domain) {
            return Optional.ofNullable((T) vocabulariesByDomain.get(domain));
        }

        @Override
        public long count() {
            return vocabulariesByDomain.size();
        }

        @Override
        public <T extends AbstractGsrsControlledVocabularyDTO> Optional<T> findByResolvedId(String anyKindOfId) {
            return Optional.empty();
        }

        @Override
        public <T extends AbstractGsrsControlledVocabularyDTO> Optional<T> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public boolean existsById(Long id) {
            return false;
        }

        @Override
        public <T extends AbstractGsrsControlledVocabularyDTO> T create(T dto) {
            vocabulariesByDomain.put(dto.getDomain(), dto);
            return dto;
        }

        @Override
        public <T extends AbstractGsrsControlledVocabularyDTO> T update(T dto) {
            vocabulariesByDomain.put(dto.getDomain(), dto);
            return dto;
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
        String suffix = "SUFFIX";
        int length = String.valueOf(Long.MAX_VALUE).length()+suffix.length();
        boolean padding = true;
        String codeSystem = "Codes R Us";
        Long max = Long.MAX_VALUE;
        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(seqGenName, length, suffix, padding, max, codeSystem, null);
        ProteinSubstance substance = newProteinSubstance();
        AutowireHelper.getInstance().autowire(codeGenerator);
        codeGenerator.addCode(substance);
        Assertions.assertTrue(substance.codes.stream().anyMatch(c -> c.codeSystem.equals(codeSystem)));
    }

    // ==== A

    @Test
    public void testSeqGenNoSuffix() {
        String seqGenName = "not used";
        int length = String.valueOf(Long.MAX_VALUE).length();
        String suffix = "";
        boolean padding = true;
        String codeSystem = "Codes R Us";
        Long max = Long.MAX_VALUE;
        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(seqGenName, length, suffix, padding, max, codeSystem, null);
        ProteinSubstance substance = newProteinSubstance();
        AutowireHelper.getInstance().autowire(codeGenerator);
        codeGenerator.addCode(substance);
        Assertions.assertTrue(substance.codes.stream().anyMatch(c -> c.codeSystem.equals(codeSystem)));
    }

    @Test
    public void testSeqGenNoCode() {
        String seqGenName = "not used";
        String suffix = "SUFFIX";
        int length = String.valueOf(Long.MAX_VALUE).length()+suffix.length();
        boolean padding = true;
        String codeSystem = "";
        Long max = Long.MAX_VALUE;
        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(seqGenName, length, suffix, padding, max, codeSystem, null);
        ProteinSubstance substance = newProteinSubstance();
        AutowireHelper.getInstance().autowire(codeGenerator);
        codeGenerator.addCode(substance);
        Assertions.assertTrue(substance.codes.stream().anyMatch(c -> c.codeSystem.equals(codeSystem)));
    }

    @Test
    public void testSeqGenTopBottomRange() {
        {
            String seqGenName = "not used1";
            int length = 8;
            String suffix = "SUFFIX1";
            boolean padding = true;
            // should null be allowed/handled?
            String codeSystem = "Codes R Us1";
            Long max = 1L;
            CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(seqGenName, length, suffix, padding, max, codeSystem, null);
            ProteinSubstance substance = newProteinSubstance();
            AutowireHelper.getInstance().autowire(codeGenerator);
            codeGenerator.addCode(substance);
            assert substance != null;
            Assertions.assertTrue(substance.codes.stream().anyMatch(c -> c.codeSystem.equals(codeSystem)));
        }
        {
            String seqGenName = "not used1";
            int length = 8;
            String suffix = "SUFFIX1";
            boolean padding = true;
            // should null be allowed/handled?
            String codeSystem = "Codes R Us1";
            Long max = 1L;
            CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(seqGenName, length, suffix, padding, max, codeSystem, null);
            ChemicalSubstance substance = newChemicalSubstance();
            AutowireHelper.getInstance().autowire(codeGenerator);
            codeGenerator.addCode(substance);
            assert substance != null;
            Assertions.assertTrue(substance.codes.stream().anyMatch(c -> c.codeSystem.equals(codeSystem)));
        }
    }

    @Test
    public void testSeqGenCheckDefaults() {
        String seqGenName = "not used";
        int length = 0;
        String suffix = null;
        boolean padding = true;
        Long max = null;
        String codeSystem = "Codes R Us";
        CodeSequentialGenerator codeGenerator = new CodeSequentialGenerator(seqGenName, length, suffix, padding, max, codeSystem, null);
        AutowireHelper.getInstance().autowire(codeGenerator);
        Assertions.assertEquals(codeGenerator.getMax(), (Long) Long.MAX_VALUE);

        Assertions.assertEquals(codeGenerator.getLen(), String.valueOf(codeGenerator.getMax()).length()+codeGenerator.getSuffix().length());
    }

    // ==== B

    @Test
    public void testCreateCodeSystem() {
        Map<String, Object> instantiationMap = new HashMap<>();
        instantiationMap.put("name", "whatever");
        instantiationMap.put("suffix", "OO");
        instantiationMap.put("length", String.valueOf(Long.MAX_VALUE).length()+2);
        instantiationMap.put("codesystem", codeSystemName);
        instantiationMap.put("padding", true);
        instantiationMap.put("max", Long.MAX_VALUE);
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
            ProteinSubstance substance = newProteinSubstance();
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
        instantiationMap.put("length", String.valueOf(Long.MAX_VALUE).length()+2);
        instantiationMap.put("codesystem", codeSystemName);
        instantiationMap.put("padding", true);
        instantiationMap.put("max", Long.MAX_VALUE);
        UniqueCodeGenerator uniqueCodeGenerator = new UniqueCodeGenerator(instantiationMap);
        AutowireHelper.getInstance().autowire(uniqueCodeGenerator);

        //verify that a call to the constructor of the class results in a new term added to the CV
        ProteinSubstance substance = newProteinSubstance();
        uniqueCodeGenerator.prePersist(substance);
        Assertions.assertTrue(substance.codes.stream().anyMatch(c -> c.codeSystem.equals(codeSystemName)));
    }

    @Test
    public void testPublicCode() {
        Map<String, Object> instantiationMap = new HashMap<>();
        instantiationMap.put("name", "whatever");
        instantiationMap.put("suffix", "OO");
        instantiationMap.put("length", String.valueOf(Long.MAX_VALUE).length()+2);
        instantiationMap.put("codesystem", codeSystemName);
        instantiationMap.put("padding", true);
        instantiationMap.put("max", Long.MAX_VALUE);
        UniqueCodeGenerator uniqueCodeGenerator = new UniqueCodeGenerator(instantiationMap);
        AutowireHelper.getInstance().autowire(uniqueCodeGenerator);
        ProteinSubstance substance = newProteinSubstance();
        uniqueCodeGenerator.prePersist(substance);
        Assertions.assertTrue(substance.codes.stream().filter(c -> c.codeSystem.equals(codeSystemName)).findFirst().get().isPublic());
    }

    @Test
    public void testNonPublicCode() {
        Map<String, Object> instantiationMap = new HashMap<>();
        instantiationMap.put("name", "whatever");
        instantiationMap.put("suffix", "OO");
        instantiationMap.put("length", String.valueOf(Long.MAX_VALUE).length()+2);
        instantiationMap.put("codesystem", codeSystemName);
        instantiationMap.put("padding", true);
        instantiationMap.put("max", Long.MAX_VALUE);
        Map<Integer, String> groupsMap = new HashMap<>();
        groupsMap.put(0, "protected");
        groupsMap.put(1, "admin");
        instantiationMap.put("groups", groupsMap);
        UniqueCodeGenerator uniqueCodeGenerator = new UniqueCodeGenerator(instantiationMap);
        AutowireHelper.getInstance().autowire(uniqueCodeGenerator);
        ProteinSubstance substance = newProteinSubstance();
        uniqueCodeGenerator.prePersist(substance);
        assertEquals(new HashSet<>(Arrays.asList(new Group("protected"), new Group("admin"))),
            substance.codes.stream().filter(c -> c.codeSystem.equals(codeSystemName)).findFirst().get().getAccess());
    }

    @Test
    public void testSkipAddingCode() {
        Map<String, Object> instantiationMap = new HashMap<>();
        instantiationMap.put("name", "whatever");
        instantiationMap.put("suffix", "OO");
        instantiationMap.put("length", String.valueOf(Long.MAX_VALUE).length()+2);
        instantiationMap.put("codesystem", codeSystemName);
        instantiationMap.put("padding", true);
        instantiationMap.put("max", Long.MAX_VALUE);
        UniqueCodeGenerator uniqueCodeGenerator = new UniqueCodeGenerator(instantiationMap);
        AutowireHelper.getInstance().autowire(uniqueCodeGenerator);

        //verify that a call to the constructor of the class results in a new term added to the CV
        ProteinSubstance substance = newProteinSubstance();
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

    private ProteinSubstance newProteinSubstance() {
        return new ProteinSubstance();
    }

    private ChemicalSubstance newChemicalSubstance() {
        return new ChemicalSubstance();
    }

}
