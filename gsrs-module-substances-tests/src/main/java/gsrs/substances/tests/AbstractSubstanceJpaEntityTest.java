<<<<<<< HEAD
package gsrs.substances.tests;
=======
<<<<<<< HEAD:gsrs-ncats-substance-extension/src/test/java/gov/nih/ncats/bases/AbstractSubstanceJpaEntityTest.java
package gov.nih.ncats.bases;
=======
package gsrs.substances.tests;
>>>>>>> master:gsrs-module-substances-tests/src/main/java/gsrs/substances/tests/AbstractSubstanceJpaEntityTest.java
>>>>>>> master

import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.startertests.GsrsJpaTest;
import ix.seqaln.service.SequenceIndexerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;

@GsrsJpaTest
public abstract class AbstractSubstanceJpaEntityTest extends AbstractSubstanceJpaEntityTestSuperClass {

    @Autowired
    protected TestEntityManager entityManager;


    @MockBean
    protected SequenceIndexerService mockSequenceIndexerService;

    @MockBean
    protected StructureIndexerService mockStructureIndexerService;

    @Override
    protected EntityManagerFacade getEntityManagerFacade() {
        return EntityManagerFacade.wrap(entityManager);
    }
}
