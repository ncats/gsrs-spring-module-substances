package gsrs.module.substance.services;

import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import gsrs.payload.LegacyPayloadConfiguration;
import gsrs.module.substance.controllers.PayloadUploadPreflightInterceptor;
import ix.core.interfaces.GsrsJsonMapper;
import ix.core.processing.*;
import ix.ginas.utils.validation.ValidatorFactory;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Optional;

@Configuration
@Data
public class SubstanceBulkLoadServiceConfiguration {
    @Value("${ix.ginas.maxrecordqueue:100}")
    private int maxQueueSize=100;
    @Value("${ix.ginas.batch.loadingThreads:1}")
    private int loadingThreads=1;
    /** Minimum milliseconds between per-record progress saves of the ProcessingJob row; 0 saves after every record. */
    @Value("${ix.ginas.batch.progressSaveIntervalMs:2000}")
    private long progressSaveIntervalMs=2000;
    /** Prepare an indexed case-insensitive name lookup before opening bulk-load transactions. */
    @Value("${ix.ginas.batch.indexNameLookups:true}")
    private boolean indexNameLookups=true;
    /**
     * Index substances created or changed by a bulk load once, after all its workers finish,
     * instead of with background index events while the load runs.
     */
    @Value("#{new Boolean('${ix.ginas.batch.deferIndexing:true}')}")
    private boolean deferIndexing=true;
    @Value("#{new Boolean('${ix.ginas.batch.persist:true}')}")
    private boolean actuallyPersist=true;
    @Value("#{new Boolean('${ix.ginas.batch.validation:true}')}")
    private boolean validate=true;
    @Value("${ix.ginas.batch.validationStrategy:ACCEPT_APPLY_ALL_MARK_FAILED}")
    private String batchProcessingStrategy;

    @Value("${ix.ginas.PersistRecordWorkerFactoryImpl:ix.core.plugins.SingleThreadedPersistRecordWorkerFactory}")
    private String persistRecordWorkerFactoryImpl =
            "ix.core.plugins.SingleThreadedPersistRecordWorkerFactory";

    @Autowired
    private GinasSubstancePersisterFactory persister;

    private final GsrsJsonMapper mapper;

    private final ValidatorFactory fakeValidatorFactory;

    public SubstanceBulkLoadServiceConfiguration() {
        this.mapper = null;
        this.fakeValidatorFactory = null;
    }

    @Autowired
    public SubstanceBulkLoadServiceConfiguration(
            @Qualifier("gsrsJsonMapper") GsrsJsonMapper mapper) {
        this.mapper = mapper;
        this.fakeValidatorFactory = new FakeValidatorFactory(mapper);
    }

    private CachedSupplier.CachedThrowingSupplier<PersistRecordWorkerFactory> persistRecordWorkerFactoryCachedSupplier = CachedSupplier.ofThrowing(()->{
        return AutowireHelper.getInstance().autowireAndProxy((PersistRecordWorkerFactory) Class.forName(persistRecordWorkerFactoryImpl).newInstance());
    });
     public PersistRecordWorkerFactory getPersistRecordWorkerFactory(SubstanceBulkLoadService.SubstanceBulkLoadParameters parameters){
         Optional<PersistRecordWorkerFactory> opt= persistRecordWorkerFactoryCachedSupplier.get();
         if(opt.isPresent()){
             return opt.get();
         }
         return Sneak.sneakyThrow(persistRecordWorkerFactoryCachedSupplier.thrown);
     }

     @Bean
     public GinasSubstancePersisterFactory ginasSubstancePersisterFactory() {
         return new GinasSubstancePersisterFactory();
     }

     @Bean
     public SubstanceLobSchemaCompatibilityInitializer substanceLobSchemaCompatibilityInitializer(DataSource dataSource) {
         return new SubstanceLobSchemaCompatibilityInitializer(dataSource);
     }

     @Bean
     public SubstanceNameLookup substanceNameLookup(DataSource dataSource) {
         return new SubstanceNameLookup(dataSource);
     }

     @Bean
     public BulkUploadPreflight bulkUploadPreflight(
             DataSource dataSource, LegacyPayloadConfiguration payloadConfiguration,
             @Value("${ix.ginas.batch.maxUploadSize:100MB}") DataSize maxUploadSize) {
         return new BulkUploadPreflight(dataSource, payloadConfiguration, maxUploadSize);
     }

     @Bean
     public WebMvcConfigurer payloadUploadPreflightConfigurer(BulkUploadPreflight preflight) {
         return new WebMvcConfigurer() {
             @Override
             public void addInterceptors(InterceptorRegistry registry) {
                 registry.addInterceptor(new PayloadUploadPreflightInterceptor(preflight));
             }
         };
     }

     public RecordPersisterFactory getRecordPersisterFactory(){
        return persister;
     }
     public RecordExtractorFactory getRecordExtractorFactory(){
         return new GinasDumpRecordExtractorFactory();
     }

     public GinasSubstanceTransformerFactory getRecordTransformFactory(){
         //TODO move this validate check to entity service ?
         return GinasSubstanceTransformerFactory.INSTANCE;
     }

    private static class FakeValidatorFactory extends ValidatorFactory {
        FakeValidatorFactory(GsrsJsonMapper mapper) {
            super(Collections.emptyList(), mapper);
        }
    }
}
