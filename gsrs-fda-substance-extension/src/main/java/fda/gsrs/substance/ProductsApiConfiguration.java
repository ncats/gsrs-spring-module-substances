package fda.gsrs.substance;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("gsrs.microservice.products.api")
public class ProductsApiConfiguration extends AbstractGsrsRestApiConfiguration{


}
