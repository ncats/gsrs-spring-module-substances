package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.sneak.Sneak;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.cache.GsrsCache;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.autoconfigure.GsrsSubstanceModuleAutoConfiguration;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.repository.ETagRepository;
import gsrs.repository.EditRepository;
import gsrs.repository.GroupRepository;
import gsrs.service.ExportService;
import gsrs.service.GsrsEntityService;
import gsrs.startertests.*;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import gsrs.startertests.jupiter.ClearAuditorBeforeEachExtension;
import gsrs.startertests.jupiter.ResetAllEntityServicesBeforeEachExtension;
import ix.core.models.Group;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.Substance;
import ix.seqaln.service.SequenceIndexerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GsrsJpaTest
public abstract class AbstractSubstanceJpaEntityTest extends AbstractSubstanceJpaEntityTest2 {

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
