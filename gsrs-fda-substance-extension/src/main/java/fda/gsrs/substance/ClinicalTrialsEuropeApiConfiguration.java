package fda.gsrs.substance;

import gsrs.api.AbstractGsrsRestApiConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("gsrs.microservice.clinicaltrialseurope.api")
public class ClinicalTrialsEuropeApiConfiguration extends AbstractGsrsRestApiConfiguration { }
