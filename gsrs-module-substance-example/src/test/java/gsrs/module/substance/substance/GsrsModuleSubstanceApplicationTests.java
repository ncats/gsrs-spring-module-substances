package gsrs.module.substance.substance;

import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@GsrsJpaTest
@ActiveProfiles("test")
class GsrsModuleSubstanceApplicationTests extends AbstractGsrsJpaEntityJunit5Test {

    @Test
    void contextLoads() {
    }

}
