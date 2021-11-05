package fda.gsrs.substance;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import gsrs.api.AbstractGsrsRestApiConfiguration;

@Configuration
@ConfigurationProperties("gsrs.microservice.products.api")
public class ProductsApiConfiguration extends AbstractGsrsRestApiConfiguration{


}
