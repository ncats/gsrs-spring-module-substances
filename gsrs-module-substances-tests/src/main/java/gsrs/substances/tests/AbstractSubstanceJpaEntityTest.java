package gsrs.substances.tests;

import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.startertests.GsrsJpaTest;
import ix.seqaln.service.SequenceIndexerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GsrsJpaTest
public abstract class AbstractSubstanceJpaEntityTest extends AbstractSubstanceJpaEntityTestSuperClass {

    @Autowired
    protected TestEntityManager entityManager;


    @MockitoBean
    protected SequenceIndexerService mockSequenceIndexerService;

    @MockitoBean
    protected StructureIndexerService mockStructureIndexerService;

    @Override
    protected EntityManagerFacade getEntityManagerFacade() {
        return EntityManagerFacade.wrap(entityManager);
    }
}
