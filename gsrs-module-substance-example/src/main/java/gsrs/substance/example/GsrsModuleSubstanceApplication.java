package gsrs.substance.example;

import gsrs.*;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.repository.NucleicAcidSubstanceRepository;
import gsrs.module.substance.repository.ProteinSubstanceRepository;
import gsrs.module.substance.services.LegacySubstanceSequenceSearchService;
import gsrs.module.substance.services.SubstanceSequenceSearchService;
import gsrs.service.PayloadService;
import ix.seqaln.service.LegacySequenceIndexerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EntityScan(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableJpaRepositories(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableGsrsLegacyAuthentication
@EnableGsrsLegacyCache
@EnableGsrsLegacyPayload
@EnableGsrsLegacySequenceSearch
//@Import(LegacySequenceIndexerService.class)
public class GsrsModuleSubstanceApplication {


    public static void main(String[] args) {
        SpringApplication.run(GsrsModuleSubstanceApplication.class, args);
    }

}
