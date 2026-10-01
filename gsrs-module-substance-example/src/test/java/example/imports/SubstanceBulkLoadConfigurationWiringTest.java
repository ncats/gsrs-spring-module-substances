package example.imports;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.services.SubstanceBulkLoadService;
import gsrs.module.substance.services.SubstanceBulkLoadServiceConfiguration;
import ix.core.models.ProcessingJob;
import ix.core.processing.GinasSubstancePersisterFactory;
import ix.core.processing.RecordPersister;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest(
        classes = GsrsModuleSubstanceApplication.class,
        properties = {
                "gsrs.microservice.applications.api.baseURL=http://example.com",
                "gsrs.microservice.products.api.baseURL=http://example.com",
                "gsrs.microservice.clinicaltrialsus.api.baseURL=http://example.com",
                "gsrs.microservice.clinicaltrialseurope.api.baseURL=http://example.com",
                "gsrs.microservice.substances.api.baseURL=http://example.com"
        }
)
class SubstanceBulkLoadConfigurationWiringTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SubstanceBulkLoadServiceConfiguration configuration;

    @Autowired
    private GinasSubstancePersisterFactory persisterFactory;

    @Test
    void bulkLoadPersisterFactoryShouldBeRegisteredOnceAndReturnedByConfiguration() {
        Map<String, GinasSubstancePersisterFactory> beans = applicationContext.getBeansOfType(GinasSubstancePersisterFactory.class);

        assertEquals(1, beans.size());
        assertSame(persisterFactory, configuration.getRecordPersisterFactory());

        RecordPersister<?, ?> persister = persisterFactory.createPersisterFor(new ProcessingJob());
        Object target = AopTestUtils.getUltimateTargetObject(persister);
        assertInstanceOf(SubstanceBulkLoadService.GinasSubstancePersister.class, target);
        assertNotNull(ReflectionTestUtils.getField(target, "entityManager"));
        assertNotNull(ReflectionTestUtils.getField(target, "substanceEntityService"));
    }
}
