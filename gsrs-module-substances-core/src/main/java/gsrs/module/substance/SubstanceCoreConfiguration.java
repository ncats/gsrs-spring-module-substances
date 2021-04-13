package gsrs.module.substance;

import gsrs.controller.EditController2;
import gsrs.controller.EditEntityService;
import gsrs.module.substance.controllers.*;
import gsrs.module.substance.standardizer.StructureStandardizerConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Import({SubstanceController.class, EditController2.class, NameController.class, CodeController.class, ReferenceController.class,
        SubstanceLegacySearchService.class,  StructureProcessingConfiguration.class, StructureStandardizerConfiguration.class,
        EditEntityService.class, NameLegacySearchService.class, CodeLegacySearchService.class, ReferenceLegacySearchService.class,
        SubstanceEntityService.class,
})

public class SubstanceCoreConfiguration {
}
