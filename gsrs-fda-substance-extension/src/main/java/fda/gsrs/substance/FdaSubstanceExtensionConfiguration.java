package fda.gsrs.substance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.hhs.gsrs.applications.api.ApplicationsApi;
import gov.hhs.gsrs.clinicaltrial.us.api.ClinicalTrialUSApi;
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
    public ClinicalTrialUSApi clinicalTrialUSApi(ClinicalTrialUSApiConfiguration clinicalTrialUSApiConfiguration){
        return new ClinicalTrialUSApi(clinicalTrialUSApiConfiguration.createNewRestTemplateBuilder(),clinicalTrialUSApiConfiguration.getBaseURL(), mapper);
    }

    private ObjectMapper mapper = new ObjectMapper();
}
