package gsrs.module.substance.testconfig;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
/**
 * Test Spring Boot application configuration for testing the substances module.
 * This class provides the necessary Spring Boot context for integration tests
 * while excluding heavy dependencies like JPA repositories and API autoconfiguration.
 */
@SpringBootApplication(exclude = {
        DataJpaRepositoriesAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "gsrs.module.substance"
},
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = ".*ServiceImpl"
                )
        }
)
@Import(TestBeansConfiguration.class)
public class SpringBootTestApplication {
}





