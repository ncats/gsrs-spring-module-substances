package gsrs.module.substance.autoconfigure;

import gsrs.EnableGsrsAkka;
import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsJpaEntities;
import gsrs.module.substance.repository.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
//@EnableEurekaClient
@EnableGsrsAkka
@EnableGsrsJpaEntities
@EnableGsrsApi

//@Import({StructureProcessingConfiguration.class, SubstanceEntityService.class,
//        NucleicAcidSubstanceRepository.class, ComponentRepository.class,
//        NameRepository.class, ProteinSubstanceRepository.class, ReferenceRepository.class,
//         StructureRepository.class, SubunitRepository.class, ValueRepository.class,
//        ETagRepository.class
//})
public class GsrsSubstanceModuleAutoConfiguration {



}
