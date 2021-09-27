package gsrs.module.substance.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.definitional.DefinitionalElement;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
@ConfigurationProperties("substance.definitional-elements")
@Data
public class ConfigBasedDefinitionalElementConfiguration{

    private List<DefinitionalElementImplementationConfig> implementations;


    private CachedSupplier<List<DefinitionalElementImplementation>> cachedInstances = CachedSupplier.runOnce(()->{
       List<DefinitionalElementImplementation> list = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
       for(DefinitionalElementImplementationConfig config : implementations){
           DefinitionalElementImplementation implementation;
           if(config.getParameters() ==null){
               implementation = (DefinitionalElementImplementation) mapper.convertValue(Collections.emptyMap(), config.getImplementationClass());
           }else{
               implementation = (DefinitionalElementImplementation) mapper.convertValue(config.getParameters(), config.getImplementationClass());
           }

           list.add(AutowireHelper.getInstance().autowireAndProxy(implementation));
       }
       return list;
    });

    public void computeDefinitionalElements(Object s, Consumer<DefinitionalElement> consumer){
        if(s==null){
            return;
        }
        for(DefinitionalElementImplementation impl : cachedInstances.getSync()){
            if(impl.supports(s)){
                impl.computeDefinitionalElements(s, consumer);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefinitionalElementImplementationConfig{

        private Class implementationClass;
        /**
         * Additional parameters to initialize in your instance returned by
         * {@link #getImplementationClass()}.
         */
        private Map<String, Object> parameters;
    }
}
