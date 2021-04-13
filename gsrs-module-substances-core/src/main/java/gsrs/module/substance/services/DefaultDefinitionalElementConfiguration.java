package gsrs.module.substance.services;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultDefinitionalElementConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DefinitionalElementFactory definitionalElementFactory(){
        return new ConfigBasedDefinitionalElementFactory();
    }
}
