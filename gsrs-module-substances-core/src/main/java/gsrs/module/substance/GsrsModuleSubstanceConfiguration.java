package gsrs.module.substance;

import gsrs.*;
import gsrs.cache.GsrsCache;
import gsrs.module.substance.repository.NucleicAcidSubstanceRepository;
import gsrs.module.substance.repository.ProteinSubstanceRepository;
import gsrs.module.substance.services.LegacySubstanceSequenceSearchService;
import gsrs.module.substance.services.SubstanceSequenceSearchService;
import gsrs.service.PayloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ix.seqaln.service.SequenceIndexerService;


@SpringBootApplication
@EnableConfigurationProperties
@Configuration
//@EnableEurekaClient
@EnableGsrsAkka
@EnableGsrsJpaEntities
@EnableGsrsApi
@EntityScan(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableJpaRepositories(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableGsrsLegacyAuthentication
@EnableGsrsLegacyCache
@EnableGsrsLegacyPayload
@EnableGsrsLegacySequenceSearch
//@Import(LegacySequenceIndexerService.class)
public class GsrsModuleSubstanceConfiguration {

    @Autowired
    private SequenceIndexerService sequenceIndexerService;

    @Autowired
    private GsrsCache ixCache;

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
    @ConditionalOnMissingBean(SubstanceSequenceSearchService.class)
    public SubstanceSequenceSearchService getSequenceSearchService(){
        return new LegacySubstanceSequenceSearchService(sequenceIndexerService, ixCache,payloadService,
                proteinSubstanceRepository, nucleicAcidSubstanceRepository);
    }
}
