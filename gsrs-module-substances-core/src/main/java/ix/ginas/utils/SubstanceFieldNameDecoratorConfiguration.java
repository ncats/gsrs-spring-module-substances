package ix.ginas.utils;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("gsrs.field-name-decorator")
public class SubstanceFieldNameDecoratorConfiguration {
    private Map<String,String> substances;
    private static CachedSupplier<SubstanceFieldNameDecoratorConfiguration> _instanceSupplier = CachedSupplier.of(()->{
        SubstanceFieldNameDecoratorConfiguration instance = StaticContextAccessor.getBean(SubstanceFieldNameDecoratorConfiguration.class);
        return instance;
    });

    public static SubstanceFieldNameDecoratorConfiguration INSTANCE() {
        return _instanceSupplier.get();
    }


    public String get(String key) {
        if (substances == null) return null;
        return substances.get(key.replace(" ", ""));
    }
}