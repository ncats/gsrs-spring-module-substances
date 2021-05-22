package example.substance;

import example.GsrsModuleSubstanceApplication;
import gsrs.startertests.GsrsFullStackTest;
import gsrs.startertests.GsrsJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;

@SpringBootTest(classes = {GsrsModuleSubstanceApplication.class})
@GsrsFullStackTest
public abstract class AbstractSubstanceJpaFullStackEntityTest extends AbstractSubstanceJpaEntityTest2 {
    @Autowired
    protected EntityManager entityManager;

    @Override
    protected EntityManagerFacade getEntityManagerFacade() {
        return EntityManagerFacade.wrap(entityManager);
    }
}
