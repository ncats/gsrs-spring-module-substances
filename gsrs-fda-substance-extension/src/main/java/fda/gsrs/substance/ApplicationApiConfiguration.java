package fda.gsrs.substance;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import gsrs.api.AbstractGsrsRestApiConfiguration;

@Configuration
@ConfigurationProperties("gsrs.microservice.applications.api")
public class ApplicationApiConfiguration extends AbstractGsrsRestApiConfiguration{


}
