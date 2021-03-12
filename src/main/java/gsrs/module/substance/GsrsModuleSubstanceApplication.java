package gsrs.module.substance;

import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsJpaEntities;
import gsrs.EnableGsrsLegacyAuthentication;
import gsrs.GsrsFactoryConfiguration;
import gsrs.validator.GsrsValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties
//@EnableEurekaClient
@EnableGsrsJpaEntities
@EnableGsrsApi
@EntityScan(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableJpaRepositories(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableGsrsLegacyAuthentication
public class GsrsModuleSubstanceApplication {

    @Primary
    @Bean
    @ConfigurationProperties("gsrs")
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GsrsFactoryConfiguration gsrsFactoryConfiguration(){
        return new GsrsFactoryConfiguration();
    }

    public static void main(String[] args) {
        SpringApplication.run(GsrsModuleSubstanceApplication.class, args);
    }

}
