package fda.gsrs.substance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.hhs.gsrs.applications.api.ApplicationsApi;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FdaSubstanceExtensionConfiguration {

    //Put FDA specific API @Bean definitions here
    @Bean
    public ApplicationsApi applicationsApi(RestTemplateBuilder builder, ApplicationApiConfiguration applicationApiConfiguration){
    //public ApplicationsApi applicationsApi(RestTemplateBuilder builder, @Value("${application.host}") String applicationHost){
        applicationApiConfiguration.configure(builder);
        return new ApplicationsApi(builder,applicationApiConfiguration.getBaseURL(), mapper );
    }

    private ObjectMapper mapper = new ObjectMapper();
}
