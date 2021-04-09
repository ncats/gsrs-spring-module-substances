package gsrs.module.substance;

import gsrs.*;
import gsrs.module.substance.repository.NucleicAcidSubstanceRepository;
import gsrs.module.substance.repository.ProteinSubstanceRepository;
import gsrs.module.substance.services.LegacySubstanceSequenceSearchService;
import gsrs.module.substance.services.SubstanceSequenceSearchService;
import gsrs.service.PayloadService;
import gsrs.validator.GsrsValidatorFactory;
import ix.core.cache.IxCache;
import ix.core.chem.StructureHasher;
import ix.core.chem.StructureProcessor;
import ix.core.chem.StructureStandardizer;
import ix.seqaln.service.LegacySequenceIndexerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ix.seqaln.service.SequenceIndexerService;
@SpringBootApplication
@EnableConfigurationProperties
//@EnableEurekaClient
@EnableGsrsJpaEntities
@EnableGsrsApi
@EntityScan(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableJpaRepositories(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableGsrsLegacyAuthentication
@EnableGsrsLegacyCache
@EnableGsrsLegacyPayload
@EnableGsrsLegacySequenceSearch
//@Import(LegacySequenceIndexerService.class)
public class GsrsModuleSubstanceApplication {

    @Autowired
    private LegacySequenceIndexerService legacySequenceIndexerService;

    @Autowired
    private IxCache ixCache;

    @Autowired
    PayloadService payloadService;
    @Autowired
    private ProteinSubstanceRepository proteinSubstanceRepository;
    @Autowired
    private NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository;

    @Primary
    @Bean
    @ConfigurationProperties("gsrs")
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GsrsFactoryConfiguration gsrsFactoryConfiguration(){
        return new GsrsFactoryConfiguration();
    }

    @Bean
    public SubstanceSequenceSearchService getSequenceSearchService(){
        return new LegacySubstanceSequenceSearchService(legacySequenceIndexerService, ixCache,payloadService,
                proteinSubstanceRepository, nucleicAcidSubstanceRepository);
    }

    @Bean
    public SequenceIndexerService sequenceIndexerService(){
        return legacySequenceIndexerService;
    }

    public static void main(String[] args) {
        SpringApplication.run(GsrsModuleSubstanceApplication.class, args);
    }

}
