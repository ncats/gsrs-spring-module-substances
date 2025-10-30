package gsrs.module.substance;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties("gsrs.substance.service")
@Data
public class SubstanceEntityServiceConfiguration {
    private static CachedSupplier<SubstanceEntityServiceConfiguration> _instanceSupplier = CachedSupplier.of(()->{
        SubstanceEntityServiceConfiguration instance = StaticContextAccessor.getBean(SubstanceEntityServiceConfiguration.class);
        return instance;
    });

    public static SubstanceEntityServiceConfiguration getInstance(){return _instanceSupplier.get();}

    private Map<String, List<String >> privilegesForPossibleDuplicates;
}
