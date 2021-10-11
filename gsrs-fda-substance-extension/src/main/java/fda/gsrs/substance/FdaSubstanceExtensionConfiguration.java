package fda.gsrs.substance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.cv.api.ControlledVocabularyRestApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FdaSubstanceExtensionConfiguration {

    //Put FDA specific API @Bean definitions here

    private ObjectMapper mapper = new ObjectMapper();
    @Bean
    @Qualifier("testing")
    @ConditionalOnMissingBean
    public ControlledVocabularyApi controlledVocabularyApi(RestTemplateBuilder builder, @Value("${application.host}") String applicationHost){

        return new ControlledVocabularyRestApi(builder,applicationHost, mapper );
    }
}
