package fda.gsrs.substance;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("gsrs.microservice.applications.api")
public class ApplicationApiConfiguration extends AbstractGsrsRestApiConfiguration{


}
