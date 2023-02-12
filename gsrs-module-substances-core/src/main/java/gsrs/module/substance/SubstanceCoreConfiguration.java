package gsrs.module.substance;

import gsrs.controller.EditController2;
import gsrs.controller.EditEntityService;
import gsrs.module.substance.approval.ApprovalIdConfiguration;
import gsrs.module.substance.controllers.*;
import gsrs.module.substance.exporters.SubstanceSpreadsheetExporterConfiguration;
import gsrs.module.substance.hierarchy.SubstanceHierarchyFinder;
import gsrs.module.substance.hierarchy.SubstanceHierarchyFinderConfig;
import gsrs.module.substance.processors.RelationEventListener;
import gsrs.module.substance.services.*;
import gsrs.module.substance.standardizer.NameStandardizerConfiguration;
import gsrs.module.substance.standardizer.StructureStandardizerConfiguration;
import gsrs.module.substance.utils.MolWeightCalculatorProperties;
import gsrs.module.substance.utils.SubstanceMatchViewGenerator;
import ix.core.search.bulk.BulkSearchService;
import ix.ginas.utils.validation.ChemicalDuplicateFinder;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactory;
import ix.ginas.utils.validation.strategy.GsrsProcessingStrategyFactoryConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({SubstanceController.class, EditController2.class, NameController.class, CodeController.class, ReferenceController.class,
        SubstanceLegacySearchService.class,  StructureProcessingConfiguration.class, StructureStandardizerConfiguration.class,
        EditEntityService.class, NameLegacySearchService.class, CodeLegacySearchService.class, ReferenceLegacySearchService.class,
        SubstanceEntityServiceImpl.class, RelationEventListener.class,
        ConfigBasedDefinitionalElementConfiguration.class, ConfigBasedDefinitionalElementFactory.class,
        LegacyGinasAppController.class, NameStandardizerConfiguration.class,
        ProxyConfiguration.class, StructureResolverService.class, StructureResolverServiceConfiguration.class,
        SubstanceDataConfiguration.class,
        StructureResolverController.class, ChemicalDuplicateFinder.class,
        SubstanceSpreadsheetExporterConfiguration.class,
        SubstanceHierarchyFinder.class, SubstanceHierarchyFinderConfig.class,
        ApprovalIdConfiguration.class,RendererOptionsConfig.class, MolWeightCalculatorProperties.class,
            //legacy bulk load 
        SubstanceBulkLoadService.class, SubstanceBulkLoadServiceConfiguration.class, SubstanceLegacyBulkLoadController.class,
        ProcessingJobController.class, ProcessingJobEntityService.class,
        //used by bulk loader
        ConsoleFilterService.class,
        BulkSearchService.class,
        SubstanceMatchViewGenerator.class,
        SubstanceSequenceFileSupportService.class,
        //used for validation of Substances both single and bulk load
        GsrsProcessingStrategyFactory.class,
        GsrsProcessingStrategyFactoryConfiguration.class
})
public class SubstanceCoreConfiguration {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory>
    containerCustomizer(){
        return new EmbeddedTomcatCustomizer();
    }

    private static class EmbeddedTomcatCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

        @Override
        public void customize(TomcatServletWebServerFactory factory) {
            factory.addConnectorCustomizers((TomcatConnectorCustomizer) connector -> {
                connector.setAttribute("relaxedPathChars", "<>[\\]^`{|}");
                connector.setAttribute("relaxedQueryChars", "<>[\\]^`{|}");
            });
        }
    }
}
