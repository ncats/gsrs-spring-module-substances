package gsrs.module.substance;

import gsrs.module.substance.controllers.SubstanceController;
import gsrs.module.substance.services.SubstanceSequenceSearchService;
import gsrs.*;
import gsrs.cache.GsrsCache;
import gsrs.controller.EditEntityService;
import gsrs.module.substance.repository.NucleicAcidSubstanceRepository;
import gsrs.module.substance.repository.ProteinSubstanceRepository;
import gsrs.module.substance.services.LegacySubstanceSequenceSearchService;
import gsrs.service.PayloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import ix.seqaln.service.SequenceIndexerService;


@EnableConfigurationProperties
@Configuration
//@EnableEurekaClient
@EnableGsrsAkka
//@EntityScan(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
//@EnableJpaRepositories(basePackages ={"ix","gsrs", "gov.nih.ncats"} )

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

    //let's try creating the beans for our controllers and services
//    @Bean
//    @ConditionalOnMissingBean(SubstanceEntityService.class)
//    public SubstanceEntityService substanceEntityService(){
//        return new SubstanceEntityService();
//    }
//    @Bean
//    @ConditionalOnMissingBean(EditEntityService.class)
//    public EditEntityService editEntityService(){
//        return new EditEntityService();
//    }
//    @Bean
//    @ConditionalOnMissingBean(SubstanceController.class)
//    public SubstanceController substanceController(){
//        return new SubstanceController();
//    }
}
