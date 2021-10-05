package example.substance.validation;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.cv.ControlledVocabularyEntityService;
import gsrs.cv.ControlledVocabularyEntityServiceImpl;
import gsrs.cv.CvApiAdapter;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.repository.ControlledVocabularyRepository;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.validator.ValidationMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.VocabularyTerm;
import ix.ginas.utils.validation.validators.CodeFormatValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
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
@Import(CodeFormatValidatorTest.TestConfig.class)
public class CodeFormatValidatorTest extends AbstractSubstanceJpaEntityTest {

    private static boolean configured = false;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    private ControlledVocabularyRepository cvRepo;

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
    public void setup() {
        if (!configured) {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(CodeFormatValidator.class);
            config.setNewObjClass(Substance.class);
            factory.addValidator("substances", config);
            configured = true;
            System.out.println("configured!");
        }
        createCasVocab();
    }

    @Test
    public void testCasFormat() throws Exception {
        ChemicalSubstance chemical = buildChemicalWithLousyCasButStn();
        ValidationResponse response = substanceEntityService.validateEntity(chemical.toFullJsonNode());
        Stream<ValidationMessage> s1 = response.getValidationMessages().stream();
        long messageTotal = s1
                .filter(m -> m.getMessageType() == ValidationMessage.MESSAGE_TYPE.WARNING)
                .map(ValidationMessage::getMessage)
                .filter(m -> m.contains(" does not match pattern"))
                .count();
        long expectedMessageTotal = 1;
        Assert.assertEquals(expectedMessageTotal, messageTotal);
    }

    private ChemicalSubstance buildChemicalWithLousyCasButStn() {
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = "";
        Reference ref = new Reference();
        ref.citation = "475992-30-four";
        ref.docType = "STN (SCIFINDER)";
        ref.url = "https://chem.nlm.nih.gov/chemidplus/rn/475992-30-4";
        ref.getOrGenerateUUID();

        Code casCode = new Code();
        casCode.setCode("475992-30-four");
        casCode.codeSystem = "CAS";
        casCode.addReference(ref.getUuid().toString());
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("chemical with CAS and issues")
                .setStructure(structure)
                .addReference(ref)
                .addCode(casCode);
        return builder.build();
    }

    /*private GsrsCodeSystemControlledVocabularyDTO createCasCV() {
        GsrsCodeSystemControlledVocabularyDTO vocab = new GsrsCodeSystemControlledVocabularyDTO();
        vocab.setDomain("CODE_SYSTEM");
        vocab.setVocabularyTermType("ix.ginas.models.v1.CodeSystemControlledVocabulary");
        CodeSystemTermDTO term = new CodeSystemTermDTO();
        term.setRegex("^[0-9][0-9]*[-][0-9][0-9]-[0-9]$");
        term.setDisplay("CAS");
        term.setValue("CAS");
        List<CodeSystemTermDTO> terms = new ArrayList<>();
        terms.add(term);
        vocab.setTerms(terms);

        List<GsrsCodeSystemControlledVocabularyDTO> cv = new ArrayList<>();
        cv.add(vocab);
        cvRepo.save(vocab);
        return vocab;
        
    } */
    private void createCasVocab() {
        ControlledVocabulary vocab = new ControlledVocabulary();
        vocab.setDomain("CODE_SYSTEM");
        vocab.setVocabularyTermType("ix.ginas.models.v1.CodeSystemControlledVocabulary");
        VocabularyTerm term = new VocabularyTerm();
        term.setRegex("^[0-9][0-9]*[-][0-9][0-9]-[0-9]$");
        term.setDisplay("CAS");
        term.setValue("CAS");
        List<VocabularyTerm> terms = new ArrayList<>();
        terms.add(term);
        vocab.setTerms(terms);
        ControlledVocabulary savedV = cvRepo.save(vocab);
        log.debug("id of new vocab: " + savedV.id);
    }

}
