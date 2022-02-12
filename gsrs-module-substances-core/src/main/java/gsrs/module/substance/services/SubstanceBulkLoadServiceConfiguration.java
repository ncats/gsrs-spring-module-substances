package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.springUtils.AutowireHelper;
import gsrs.validator.GsrsValidatorFactory;
import ix.core.processing.*;
import ix.ginas.utils.validation.ValidatorFactory;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Value("${ix.ginas.PersistRecordWorkerFactoryImpl:ix.core.plugins.SingleThreadedPersistRecordWorkerFactory}")
    private String persistRecordWorkerFactoryImpl;

    private GinasSubstancePersisterFactory persister=  new GinasSubstancePersisterFactory();
    @Autowired
    private GsrsValidatorFactory validatorFactory;

    private CachedSupplier.CachedThrowingSupplier<GinasRecordProcessorPlugin.PersistRecordWorkerFactory> persistRecordWorkerFactoryCachedSupplier = CachedSupplier.ofThrowing(()->{
        return AutowireHelper.getInstance().autowireAndProxy((GinasRecordProcessorPlugin.PersistRecordWorkerFactory) Class.forName(persistRecordWorkerFactoryImpl).newInstance());
    });
     public GinasRecordProcessorPlugin.PersistRecordWorkerFactory getPersistRecordWorkerFactory(SubstanceBulkLoadService.SubstanceBulkLoadParameters parameters){
         Optional<GinasRecordProcessorPlugin.PersistRecordWorkerFactory> opt= persistRecordWorkerFactoryCachedSupplier.get();
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
         if(validate){
             return new GinasSubstanceTransformerFactory(validatorFactory.newFactory(SubstanceEntityServiceImpl.CONTEXT));
         }
         //no validation make a fake one
         return new GinasSubstanceTransformerFactory(FakeValidatorFactory.INSTANCE);
     }

     private static class FakeValidatorFactory extends ValidatorFactory{
         public static ValidatorFactory INSTANCE = new FakeValidatorFactory();
        private static ObjectMapper MAPPER = new ObjectMapper();

         public FakeValidatorFactory() {
             super(Collections.emptyList(), MAPPER);
         }
     }
}
