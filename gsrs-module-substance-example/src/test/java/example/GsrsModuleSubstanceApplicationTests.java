package example;

import gsrs.module.substance.SubstanceCoreConfiguration;
import gsrs.module.substance.autoconfigure.GsrsSubstanceModuleAutoConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes={GsrsSubstanceModuleAutoConfiguration.class})
class GsrsModuleSubstanceApplicationTests extends AbstractGsrsJpaEntityJunit5Test {

    @Test
    void contextLoads() {
    }

}
