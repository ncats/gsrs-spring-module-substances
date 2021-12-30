package gsrs.substances.tests;

//import example.GsrsModuleSubstanceApplication;
import gsrs.startertests.GsrsFullStackTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;

//@SpringBootTest(classes = {GsrsModuleSubstanceApplication.class})
@SpringBootTest
@GsrsFullStackTest(dirtyMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class AbstractSubstanceJpaFullStackEntityTest extends AbstractSubstanceJpaEntityTestSuperClass {
    @Autowired
    protected EntityManager entityManager;

    @Override
    protected EntityManagerFacade getEntityManagerFacade() {
        return EntityManagerFacade.wrap(entityManager);
    }
}
