package gsrs.module.substance.autoconfigure;

import gsrs.GsrsFactoryConfiguration;
import gsrs.cache.GsrsCache;
import gsrs.controller.EditEntityService;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.controllers.LegacyGinasAppController;
import gsrs.module.substance.controllers.ReIndexController;
import gsrs.module.substance.controllers.ReindexStatusEventListener;
import gsrs.module.substance.controllers.StructureOCRController;
import gsrs.module.substance.repository.NucleicAcidSubstanceRepository;
import gsrs.module.substance.repository.ProteinSubstanceRepository;
import gsrs.module.substance.repository.SubunitRepository;
import gsrs.module.substance.services.CodeEntityService;
import gsrs.module.substance.services.EntityManagerSubstanceKeyResolver;
import gsrs.module.substance.services.LegacySubstanceSequenceSearchService;
import gsrs.module.substance.services.NameEntityService;
import gsrs.module.substance.services.RecalcStructurePropertiesService;
import gsrs.module.substance.services.ReindexFromBackups;
import gsrs.module.substance.services.ReferenceEntityService;
import gsrs.module.substance.services.RelationshipService;
import gsrs.module.substance.services.StructureSearchConfiguration;
import gsrs.module.substance.services.SubstanceStructureSearchService;
import gsrs.service.PayloadService;
import ix.seqaln.service.SequenceIndexerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@ExtendWith(MockitoExtension.class)
class GsrsSubstanceModuleAutoConfigurationTest {

    @Mock private SequenceIndexerService sequenceIndexerService;
    @Mock private SubstanceStructureSearchService structureSearchService;
    @Mock private GsrsCache ixCache;
    @Mock private PayloadService payloadService;
    @Mock private ProteinSubstanceRepository proteinSubstanceRepository;
    @Mock private NucleicAcidSubstanceRepository nucleicAcidSubstanceRepository;
    @Mock private SubunitRepository subunitRepository;

    private GsrsSubstanceModuleAutoConfiguration configuration;

    @BeforeEach
    void setUp() throws Exception {
        configuration = new GsrsSubstanceModuleAutoConfiguration();
        inject(configuration, "sequenceIndexerService", sequenceIndexerService);
        inject(configuration, "structureSearchService", structureSearchService);
        inject(configuration, "ixCache", ixCache);
        inject(configuration, "payloadService", payloadService);
        inject(configuration, "proteinSubstanceRepository", proteinSubstanceRepository);
        inject(configuration, "nucleicAcidSubstanceRepository", nucleicAcidSubstanceRepository);
        inject(configuration, "subunitRepository", subunitRepository);
    }

    @Test
    @DisplayName("Bean factory methods return expected concrete implementations")
    void beanFactoryMethodsShouldReturnConcreteImplementationsWithoutSpringContext() {
        assertAll(
                () -> assertInstanceOf(GsrsFactoryConfiguration.class, configuration.gsrsFactoryConfiguration()),
                () -> assertInstanceOf(LegacySubstanceSequenceSearchService.class, configuration.getSequenceSearchService()),
                () -> assertInstanceOf(SubstanceStructureSearchService.class, configuration.getStructureSearchService()),
                () -> assertInstanceOf(StructureSearchConfiguration.class, configuration.getStructureSearchConfiguration()),
                () -> assertInstanceOf(RecalcStructurePropertiesService.class, configuration.recalcStructurePropertiesService()),
                () -> assertInstanceOf(SubstanceEntityServiceImpl.class, configuration.substanceEntityService()),
                () -> assertInstanceOf(LegacyGinasAppController.class, configuration.legacyGinasAppController()),
                () -> assertInstanceOf(EditEntityService.class, configuration.editEntityService()),
                () -> assertInstanceOf(gsrs.module.substance.controllers.SubstanceController.class, configuration.substanceController()),
                () -> assertInstanceOf(StructureOCRController.class, configuration.ocrController()),
                () -> assertInstanceOf(ReIndexController.class, configuration.reIndexController()),
                () -> assertInstanceOf(ReindexFromBackups.class, configuration.reindexService()),
                () -> assertInstanceOf(ReindexStatusEventListener.class, configuration.reindexStatusEventListener()),
                () -> assertInstanceOf(NameEntityService.class, configuration.nameEntityService()),
                () -> assertInstanceOf(CodeEntityService.class, configuration.codeEntityService()),
                () -> assertInstanceOf(ReferenceEntityService.class, configuration.referenceEntityService()),
                () -> assertInstanceOf(RelationshipService.class, configuration.relationshipService()),
                () -> assertInstanceOf(EntityManagerSubstanceKeyResolver.class, configuration.entityManagerSubstanceKeyResolverService())
        );
    }

    @Test
    @DisplayName("Factory methods create fresh instances for stateless beans")
    void factoryMethodsShouldCreateFreshInstances() {
        assertAll(
                () -> assertNotSame(configuration.getStructureSearchService(), configuration.getStructureSearchService()),
                () -> assertNotSame(configuration.getStructureSearchConfiguration(), configuration.getStructureSearchConfiguration()),
                () -> assertNotSame(configuration.recalcStructurePropertiesService(), configuration.recalcStructurePropertiesService()),
                () -> assertNotSame(configuration.nameEntityService(), configuration.nameEntityService()),
                () -> assertNotSame(configuration.codeEntityService(), configuration.codeEntityService()),
                () -> assertNotSame(configuration.referenceEntityService(), configuration.referenceEntityService()),
                () -> assertNotSame(configuration.relationshipService(), configuration.relationshipService()),
                () -> assertNotSame(configuration.entityManagerSubstanceKeyResolverService(), configuration.entityManagerSubstanceKeyResolverService())
        );
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

