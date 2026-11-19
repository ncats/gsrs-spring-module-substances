package gsrs.module.substance.testconfig;

import gsrs.EntityProcessorFactory;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that provides mock beans for Spring Boot tests.
 */
@Configuration
public class TestBeansConfiguration {

    @Bean
    @Primary
    public EntityProcessorFactory entityProcessorFactory() {
        return Mockito.mock(EntityProcessorFactory.class);
    }
}

