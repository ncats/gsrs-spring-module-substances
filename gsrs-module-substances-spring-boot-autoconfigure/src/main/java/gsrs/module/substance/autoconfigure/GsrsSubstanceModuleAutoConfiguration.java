package gsrs.module.substance.autoconfigure;

import gsrs.EnableGsrsAkka;
import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsJpaEntities;
import gsrs.module.substance.StructureProcessingConfiguration;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.repository.*;
import gsrs.repository.ETagRepository;
import ix.ginas.models.v1.Subunit;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
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
