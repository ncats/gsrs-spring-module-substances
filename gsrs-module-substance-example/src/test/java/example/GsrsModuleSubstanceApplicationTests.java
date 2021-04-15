package example;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.module.substance.SubstanceCoreConfiguration;
import gsrs.module.substance.autoconfigure.GsrsSubstanceModuleAutoConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
@Profile("test")
@SpringBootTest
public class GsrsModuleSubstanceApplicationTests  {

    @Test
    void contextLoads() {
    }

}
