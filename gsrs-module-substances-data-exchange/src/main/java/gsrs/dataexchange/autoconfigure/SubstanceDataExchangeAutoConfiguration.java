package gsrs.dataexchange.autoconfigure;

import gsrs.GsrsFactoryConfiguration;
import gsrs.dataexchange.extractors.ExplicitMatchableExtractorFactory;
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
}
