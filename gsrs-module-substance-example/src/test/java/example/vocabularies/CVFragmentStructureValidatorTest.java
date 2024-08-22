package example.vocabularies;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.validator.ValidationResponse;
import ix.core.validator.ValidationResponseBuilder;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.VocabularyTerm;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.ginas.utils.validation.validators.CVFragmentStructureValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

@Slf4j
public class CVFragmentStructureValidatorTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory;

    @Test
    void testCVNoDuplicate() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("Bogus Domain");
        VocabularyTerm term = new VocabularyTerm();
        term.value = "value1";
        term.display = "display1";
        term.description = "description1";

        VocabularyTerm term2 = new VocabularyTerm();
        term2.value = "value2";
        term2.display = "display2";
        term2.description = "description2";
        vocabulary.setTerms(Arrays.asList(term, term2));
        CVFragmentStructureValidator validator = new CVFragmentStructureValidator();
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewDefaultStrategy();
        ValidationResponse<Substance> response2 = new ValidationResponse<>();
        ValidationResponseBuilder validationResponsebuilder = new ValidationResponseBuilder(vocabulary,
                response2, strategy);
        validator.validate(vocabulary,null, validationResponsebuilder);
        ValidationResponse<ControlledVocabulary>response= validationResponsebuilder.buildResponse();
        Assertions.assertTrue(response.isValid());
    }

    @Test
    void testCVWithDuplicate() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("Bogus Domain");
        VocabularyTerm term = new VocabularyTerm();
        term.value = "value1";
        term.display = "display1";
        term.description = "description1";

        VocabularyTerm term2 = new VocabularyTerm();
        term2.value = "value1";
        term2.display = "display1";
        term2.description = "description1";
        vocabulary.setTerms(Arrays.asList(term, term2));
        CVFragmentStructureValidator validator = new CVFragmentStructureValidator();
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewDefaultStrategy();
        ValidationResponse<Substance> response2 = new ValidationResponse<>();
        ValidationResponseBuilder validationResponsebuilder = new ValidationResponseBuilder(vocabulary,
                response2, strategy);
        validator.validate(vocabulary,null, validationResponsebuilder);
        ValidationResponse<ControlledVocabulary>response= validationResponsebuilder.buildResponse();
        response.getValidationMessages().forEach(vm->System.out.println( vm.getMessage()));
        Assertions.assertTrue(response.hasError());
    }

    @Test
    void testCVWithDuplicate2() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("Bogus Domain");
        VocabularyTerm term = new VocabularyTerm();
        term.value = "value1";
        term.display = "display1";
        term.description = "description1";

        VocabularyTerm term0 = new VocabularyTerm();
        term0.value = "value0";
        term0.display = "display0";
        term0.description = "description0";

        VocabularyTerm term2 = new VocabularyTerm();
        term2.value = "value1";
        term2.display = "display new";
        term2.description = "description new";
        vocabulary.setTerms(Arrays.asList(term, term2));
        CVFragmentStructureValidator validator = new CVFragmentStructureValidator();
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewDefaultStrategy();
        ValidationResponse<Substance> response2 = new ValidationResponse<>();
        ValidationResponseBuilder validationResponsebuilder = new ValidationResponseBuilder(vocabulary,
                response2, strategy);
        validator.validate(vocabulary,null, validationResponsebuilder);
        ValidationResponse<ControlledVocabulary>response= validationResponsebuilder.buildResponse();
        response.getValidationMessages().forEach(vm->System.out.println( vm.getMessage()));
        Assertions.assertTrue(response.hasError());
    }

    @Test
    void testCVWithDuplicate3() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("Bogus Domain");
        VocabularyTerm term = new VocabularyTerm();
        term.value = "value1";
        term.display = "display1";
        term.description = "description1";

        VocabularyTerm term0 = new VocabularyTerm();
        term0.value = "value0";
        term0.display = "display0";
        term0.description = "description0";

        VocabularyTerm term2 = new VocabularyTerm();
        term2.value = "value new";
        term2.display = "display1";
        term2.description = "description new";
        vocabulary.setTerms(Arrays.asList(term, term2));
        CVFragmentStructureValidator validator = new CVFragmentStructureValidator();
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewDefaultStrategy();
        ValidationResponse<Substance> response2 = new ValidationResponse<>();
        ValidationResponseBuilder validationResponsebuilder = new ValidationResponseBuilder(vocabulary,
                response2, strategy);
        validator.validate(vocabulary,null, validationResponsebuilder);
        ValidationResponse<ControlledVocabulary>response= validationResponsebuilder.buildResponse();
        response.getValidationMessages().forEach(vm->System.out.println( vm.getMessage()));
        Assertions.assertTrue(response.hasError());
    }

    @Test
    void testCVWithDuplicate4() {
        ControlledVocabulary vocabulary = new ControlledVocabulary();
        vocabulary.setDomain("Bogus Domain");
        VocabularyTerm term = new VocabularyTerm();
        term.value = "value1";
        term.display = "display1";
        term.description = "description1";

        VocabularyTerm term0 = new VocabularyTerm();
        term0.value = "value0";
        term0.display = "display0";
        term0.description = "description0";

        VocabularyTerm term2 = new VocabularyTerm();
        term2.value = "value mew";
        term2.display = "display new";
        term2.description = "description1";
        vocabulary.setTerms(Arrays.asList(term, term2));
        CVFragmentStructureValidator validator = new CVFragmentStructureValidator();
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewDefaultStrategy();
        ValidationResponse<Substance> response2 = new ValidationResponse<>();
        ValidationResponseBuilder validationResponsebuilder = new ValidationResponseBuilder(vocabulary,
                response2, strategy);
        validator.validate(vocabulary,null, validationResponsebuilder);
        ValidationResponse<ControlledVocabulary>response= validationResponsebuilder.buildResponse();
        response.getValidationMessages().forEach(vm->System.out.println( vm.getMessage()));
        Assertions.assertTrue(response.hasError());
    }


}
