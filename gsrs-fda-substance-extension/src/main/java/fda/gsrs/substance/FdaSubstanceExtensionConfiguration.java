package fda.gsrs.substance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.hhs.gsrs.applications.api.ApplicationsApi;
import gov.hhs.gsrs.clinicaltrial.europe.api.ClinicalTrialsEuropeApi;
import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialsUSApi;
import gov.hhs.gsrs.products.api.ProductsApi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FdaSubstanceExtensionConfiguration {

    //Put FDA specific API @Bean definitions here
    @Bean
    public ApplicationsApi applicationsApi(ApplicationApiConfiguration applicationApiConfiguration){

        return new ApplicationsApi(applicationApiConfiguration.createNewRestTemplateBuilder(),applicationApiConfiguration.getBaseURL(), mapper );
    }

    @Bean
    public ProductsApi productsApi( ProductsApiConfiguration productsApiConfiguration){
        return new ProductsApi(productsApiConfiguration.createNewRestTemplateBuilder(),productsApiConfiguration.getBaseURL(), mapper);
    }

    @Bean
    public ClinicalTrialsUSApi clinicalTrialsUSApi(ClinicalTrialsUSApiConfiguration clinicalTrialsUSApiConfiguration){
        return new ClinicalTrialsUSApi(clinicalTrialsUSApiConfiguration.createNewRestTemplateBuilder(),clinicalTrialsUSApiConfiguration.getBaseURL(), mapper);
    }

    @Bean
    public ClinicalTrialsEuropeApi clinicalTrialsEuropeApi(ClinicalTrialsEuropeApiConfiguration clinicalTrialsEuropeApiConfiguration){
        return new ClinicalTrialsEuropeApi(clinicalTrialsEuropeApiConfiguration.createNewRestTemplateBuilder(),clinicalTrialsEuropeApiConfiguration.getBaseURL(), mapper);
    }

    private ObjectMapper mapper = new ObjectMapper();
}
