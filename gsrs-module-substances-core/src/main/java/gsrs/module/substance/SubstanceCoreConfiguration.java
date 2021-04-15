package gsrs.module.substance;

import gsrs.controller.EditController2;
import gsrs.controller.EditEntityService;
import gsrs.module.substance.controllers.*;
import gsrs.module.substance.standardizer.StructureStandardizerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({SubstanceController.class, EditController2.class, NameController.class, CodeController.class, ReferenceController.class,
        SubstanceLegacySearchService.class,  StructureProcessingConfiguration.class, StructureStandardizerConfiguration.class,
        EditEntityService.class, NameLegacySearchService.class, CodeLegacySearchService.class, ReferenceLegacySearchService.class,
        SubstanceEntityServiceImpl.class,

})
public class SubstanceCoreConfiguration {
}
