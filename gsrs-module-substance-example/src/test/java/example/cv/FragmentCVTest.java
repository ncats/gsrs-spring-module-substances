package example.cv;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.GsrsModuleSubstanceApplication;
import gsrs.security.GsrsSecurityUtils;
import gsrs.services.GroupService;
import gsrs.springUtils.AutowireHelper;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.ValidatorConfig;
import ix.core.models.Role;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidationResponseBuilder;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.FragmentControlledVocabulary;
import ix.ginas.models.v1.FragmentVocabularyTerm;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactoryConfiguration;
import ix.ginas.utils.validation.validators.CVFragmentStructureValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class FragmentCVTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private GroupService groupService;

    private CVFragmentStructureValidator cvFragmentStructureValidator = new CVFragmentStructureValidator();

    @BeforeEach
    public void makeSureValidatorIsReady() {
        AutowireHelper.getInstance().autowire(cvFragmentStructureValidator);
    }

    @Test
    void processFragmentVocabulary() throws IOException {
        String fileName = "cv_na.sugar.test.json";
        ClassPathResource fileResource =new ClassPathResource("/testJSON/" + fileName);
        String naSugarVocabSource= FileUtils.readFileToString(fileResource.getFile());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ControlledVocabulary naSugarVocab = mapper.readValue(naSugarVocabSource, FragmentControlledVocabulary.class);

        ValidationResponse<ControlledVocabulary> response = new ValidationResponse<>(naSugarVocab);
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        pConf.setDefaultStrategy("ACCEPT_APPLY_ALL_WARNINGS");

        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL_WARNINGS");
        ValidatorCallback callback = createCallbackFor(naSugarVocab, response, ValidatorConfig.METHOD_TYPE.UPDATE, strategy);
        cvFragmentStructureValidator.validate(naSugarVocab, null, callback);
        System.out.printf("total messages: %d\n", response.getValidationMessages().size());
        response.getValidationMessages().forEach(System.out::println);
        Assertions.assertEquals(0,
        response.getValidationMessages().stream()
                        .filter(m->m.getMessage().contains("appears to have duplicates"))
                        .count());
        Assertions.assertTrue(response.isValid());
    }

    @ParameterizedTest
    @MethodSource("getFragments")
    void testDifferentFragmentsHaveDifferentHashes(String termValue1, String fragmentSmiles1,
                                                   String termValue2, String fragmentSmiles2) {
        FragmentVocabularyTerm term1 = new FragmentVocabularyTerm();
        term1.setFragmentStructure(fragmentSmiles1);
        term1.value=termValue1;
        FragmentVocabularyTerm term2 = new FragmentVocabularyTerm();
        term2.setFragmentStructure(fragmentSmiles2);
        term2.value=termValue2;
        String hash1 = cvFragmentStructureValidator.getHash(term1).get();
        String hash2 = cvFragmentStructureValidator.getHash(term2).get();
        Assertions.assertNotEquals(hash1, hash2);
    }

    @Test
    void testParseToughSmiles() {
        String complexSmiles = "[H]C(=O)c1ccc(cc1)C(=O)NCCCCCCO[*] |$;;;;;;;;;;;;;;;;;;;_R92$|";
        FragmentVocabularyTerm term1 = new FragmentVocabularyTerm();
        term1.setFragmentStructure(complexSmiles);
        term1.value="5FBC6";
        String hash1 = cvFragmentStructureValidator.getHash(term1).get();
        Assertions.assertTrue(hash1.length() > 0);
    }

    @Test
    void testParseToughSmilesKekulized() {
        String complexSmiles = "[H]C(=O)C1=CC=C(C=C1)C(=O)NCCCCCCO[*] |$;;;;;;;;;;;;;;;;;;;_R92$|";
        FragmentVocabularyTerm term1 = new FragmentVocabularyTerm();
        term1.setFragmentStructure(complexSmiles);
        term1.value="5FBC6";
        String hash1 = cvFragmentStructureValidator.getHash(term1).get();
        Assertions.assertTrue(hash1.length() > 0);
    }

    protected <T> ValidatorCallback createCallbackFor(T object, ValidationResponse<T> response, ValidatorConfig.METHOD_TYPE type, GsrsProcessingStrategy strategy) {
       ValidationResponseBuilder<T> builder = new ValidationResponseBuilder<T>(object, response, strategy){
            @Override
            public void complete() {
                if(object instanceof Substance) {
                    ValidationResponse<T> resp = buildResponse();

                    List<GinasProcessingMessage> messages = resp.getValidationMessages()
                            .stream()
                            .filter(m -> m instanceof GinasProcessingMessage)
                            .map(m -> (GinasProcessingMessage) m)
                            .collect(Collectors.toList());
                    //processMessage, handleMessages, addProblems?
                    //Why all 3? because right now each of these methods might set or change fields in validation response.
                    messages.stream().forEach(strategy::processMessage);
                    resp.setValid(false);
                    if (strategy.handleMessages((Substance) object, messages)) {
                        resp.setValid(true);
                    }
                    strategy.addProblems((Substance) object, messages);

                    strategy.setIfValid(resp, messages);
                }
            }
        };
        if(type == ValidatorConfig.METHOD_TYPE.BATCH){
            builder.allowPossibleDuplicates(true);
        }
        if(GsrsSecurityUtils.hasAnyRoles(Role.SuperUpdate,Role.SuperDataEntry,Role.Admin)) {
            builder.allowPossibleDuplicates(true);
        }

        return builder;
    }

    private static Stream<Arguments> getFragments(){
        return Stream.of(
                Arguments.of("LR", "O[C@@H]1[C@@H]([*])O[C@@H](CO[*])[C@@H]1O[*] |$;;;_R90;;;;;_R91;;;_R92$|",
                        "R", "O[C@H]1[C@H]([*])O[C@H](CO[*])[C@H]1O[*] |$;;;_R90;;;;;_R91;;;_R92$|"),
                Arguments.of("LR", "[*]OC[C@@H](C[*])O[*] |$_R91;;;;;_R90;;_R92$|",
                        "R", "[*]OC[C@H](C[*])O[*] |$_R91;;;;;_R90;;_R92$|"),
                Arguments.of("r1", "[H]C(=O)c1ccc(cc1)C(=O)NCCCCCCO[*] |$;;;;;;;;;;;;;;;;;;;_R92$|",
                        "r2", "[H]C(=O)c1ccc(cc1)C(=O)NCCCCCCO[*] |$;;;;;;;;;;;;;;;;;;_R92;$|"));
    }

}
