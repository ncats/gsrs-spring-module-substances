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
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategy;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactoryConfiguration;
import ix.ginas.utils.validation.validators.CVFragmentStructureValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class FragmentCVTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private GroupService groupService;

    @Test
    void processFragments() throws IOException {
        String fileName = "cv_na.sug.22019.json";
        ClassPathResource fileResource =new ClassPathResource("/testJSON/" + fileName);
        String naSugarVocabSource= FileUtils.readFileToString(fileResource.getFile());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ControlledVocabulary naSugarVocab = mapper.readValue(naSugarVocabSource, FragmentControlledVocabulary.class);
        CVFragmentStructureValidator validator = new CVFragmentStructureValidator();
        ValidationResponse<ControlledVocabulary> response = new ValidationResponse<>(naSugarVocab);
        GsrsProcessingStrategyFactoryConfiguration pConf = new GsrsProcessingStrategyFactoryConfiguration();
        pConf = AutowireHelper.getInstance().autowireAndProxy(pConf);
        pConf.setDefaultStrategy("ACCEPT_APPLY_ALL_WARNINGS");

        GsrsProcessingStrategyFactory gsrsProcessingStrategyFactory = new GsrsProcessingStrategyFactory(groupService, pConf);
        GsrsProcessingStrategy strategy = gsrsProcessingStrategyFactory.createNewStrategy("ACCEPT_APPLY_ALL_WARNINGS");
        ValidatorCallback callback = createCallbackFor(naSugarVocab, response, ValidatorConfig.METHOD_TYPE.UPDATE, strategy);
        validator.validate(naSugarVocab, null, callback);
        System.out.printf("total messages: %d\n", response.getValidationMessages().size());
        response.getValidationMessages().forEach(System.out::println);
        Assertions.assertTrue(response.isValid());
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
//
        if(GsrsSecurityUtils.hasAnyRoles(Role.SuperUpdate,Role.SuperDataEntry,Role.Admin)) {
            builder.allowPossibleDuplicates(true);
        }

        return builder;
    }

}
