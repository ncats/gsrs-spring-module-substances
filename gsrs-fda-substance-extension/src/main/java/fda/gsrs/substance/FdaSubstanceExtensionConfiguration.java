package fda.gsrs.substance;

import gov.hhs.gsrs.applications.api.*;
import gov.hhs.gsrs.products.api.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.hhs.gsrs.applications.api.ApplicationsApi;
import gov.hhs.gsrs.products.api.ProductsApi;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FdaSubstanceExtensionConfiguration {

    //Put FDA specific API @Bean definitions here
    @Bean
    public ApplicationsApi applicationsApi(RestTemplateBuilder builder, ApplicationApiConfiguration applicationApiConfiguration){
       applicationApiConfiguration.configure(builder);
        return new ApplicationsApi(builder,applicationApiConfiguration.getBaseURL(), mapper );
    }

    @Bean
    public ProductsApi productsApi(RestTemplateBuilder builder, ProductsApiConfiguration productsApiConfiguration){
        productsApiConfiguration.configure(builder);
        return new ProductsApi(builder,productsApiConfiguration.getBaseURL(), mapper);
    }

    private ObjectMapper mapper = new ObjectMapper();
}
