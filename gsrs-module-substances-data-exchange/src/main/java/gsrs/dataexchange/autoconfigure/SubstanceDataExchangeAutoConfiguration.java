package gsrs.dataexchange.autoconfigure;

import gsrs.GsrsFactoryConfiguration;
import gsrs.dataexchange.extractors.ExplicitMatchableExtractorFactory;
import gsrs.module.substance.services.ConfigBasedDefinitionalElementFactory;
import gsrs.module.substance.services.DefinitionalElementFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class SubstanceDataExchangeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ExplicitMatchableExtractorFactory.class)
    public ExplicitMatchableExtractorFactory explicitMatchableExtractorFactory(
            GsrsFactoryConfiguration configuration) {

        ExplicitMatchableExtractorFactory factory =
                new ExplicitMatchableExtractorFactory();
        factory.setGsrsFactoryConfiguration(configuration);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public DefinitionalElementFactory definitionalElementFactory() {
        return new ConfigBasedDefinitionalElementFactory();
    }
}
