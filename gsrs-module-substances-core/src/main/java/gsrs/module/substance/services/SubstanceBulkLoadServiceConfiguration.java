package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import ix.core.processing.*;
import ix.ginas.utils.validation.ValidatorFactory;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Optional;

@Configuration
@Data
public class SubstanceBulkLoadServiceConfiguration {
    @Value("${ix.ginas.maxrecordqueue:2}")
    private int maxQueueSize=2;
    @Value("#{new Boolean('${ix.ginas.batch.persist:true}')}")
    private boolean actuallyPersist=true;
    @Value("#{new Boolean('${ix.ginas.batch.validation:true}')}")
    private boolean validate=true;
    @Value("${ix.ginas.batch.validationStrategy:ACCEPT_APPLY_ALL_MARK_FAILED}")
    private String batchProcessingStrategy;

    @Value("${ix.ginas.PersistRecordWorkerFactoryImpl:ix.core.plugins.SingleThreadedPersistRecordWorkerFactory}")
    private String persistRecordWorkerFactoryImpl;

    private GinasSubstancePersisterFactory persister=  new GinasSubstancePersisterFactory();

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


     public RecordPersisterFactory getRecordPersisterFactory(){
        return persister;
     }
     public RecordExtractorFactory getRecordExtractorFactory(){
         return new GinasDumpRecordExtractorFactory();
     }

     public GinasSubstanceTransformerFactory getRecordTransformFactory(){
         //TODO move this validate check to entity service ?
         return GinasSubstanceTransformerFactory.INSTANCE;
//         if(validate){
//             return new GinasSubstanceTransformerFactory(validatorFactory.newFactory(SubstanceEntityServiceImpl.CONTEXT));
//         }
//         //no validation make a fake one
//         return new GinasSubstanceTransformerFactory(FakeValidatorFactory.INSTANCE);
     }

     private static class FakeValidatorFactory extends ValidatorFactory{
         public static ValidatorFactory INSTANCE = new FakeValidatorFactory();
        private static ObjectMapper MAPPER = new ObjectMapper();

         public FakeValidatorFactory() {
             super(Collections.emptyList(), MAPPER);
         }
     }
}
