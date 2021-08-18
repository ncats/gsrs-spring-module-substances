package example;

import gsrs.cv.EnableControlledVocabulary;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
@Profile("test")
@SpringBootTest
@EnableControlledVocabulary
public class GsrsModuleSubstanceApplicationTests  {

    @Test
    void contextLoads() {
    }

}
