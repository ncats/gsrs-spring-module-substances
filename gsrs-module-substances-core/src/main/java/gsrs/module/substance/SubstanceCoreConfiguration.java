package gsrs.module.substance;

import gsrs.controller.EditController2;
import gsrs.controller.EditEntityService;
import gsrs.module.substance.approvalId.ApprovalIdConfiguration;
import gsrs.module.substance.controllers.*;
import gsrs.module.substance.exporters.SubstanceSpreadsheetExporterConfiguration;
import gsrs.module.substance.hierarchy.SubstanceHierarchyFinder;
import gsrs.module.substance.hierarchy.SubstanceHierarchyFinderConfig;
import gsrs.module.substance.processors.RelationEventListener;
import gsrs.module.substance.services.ConfigBasedDefinitionalElementConfiguration;
import gsrs.module.substance.services.ConfigBasedDefinitionalElementFactory;
import gsrs.module.substance.services.StructureResolverService;
import gsrs.module.substance.services.StructureResolverServiceConfiguration;
import gsrs.module.substance.standardizer.StructureStandardizerConfiguration;
import gsrs.module.substance.utils.MolWeightCalculatorProperties;
import ix.ginas.utils.validation.ChemicalDuplicateFinder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({SubstanceController.class, EditController2.class, NameController.class, CodeController.class, ReferenceController.class,
        SubstanceLegacySearchService.class,  StructureProcessingConfiguration.class, StructureStandardizerConfiguration.class,
        EditEntityService.class, NameLegacySearchService.class, CodeLegacySearchService.class, ReferenceLegacySearchService.class,
        SubstanceEntityServiceImpl.class, RelationEventListener.class,
        ConfigBasedDefinitionalElementConfiguration.class, ConfigBasedDefinitionalElementFactory.class,
        LegacyGinasAppController.class,
        ProxyConfiguration.class, StructureResolverService.class, StructureResolverServiceConfiguration.class,
        StructureResolverController.class, ChemicalDuplicateFinder.class,
        SubstanceSpreadsheetExporterConfiguration.class,
        SubstanceHierarchyFinder.class, SubstanceHierarchyFinderConfig.class,
        ApprovalIdConfiguration.class, MolWeightCalculatorProperties.class

})
public class SubstanceCoreConfiguration {
}
