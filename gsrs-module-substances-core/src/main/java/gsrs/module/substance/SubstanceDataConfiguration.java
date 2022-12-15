package gsrs.module.substance;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.StaticContextAccessor;

import java.io.IOException;
import java.net.*;

@Configuration
@ConfigurationProperties("gsrs.substance.data")
@Data
public class SubstanceDataConfiguration {
	private static CachedSupplier<SubstanceDataConfiguration> _instanceSupplier = CachedSupplier.of(()->{
		SubstanceDataConfiguration instance =StaticContextAccessor.getBean(SubstanceDataConfiguration.class);
    	return instance;
	});
	
    private int nameColumnLength=254;
    
    
    
    public static SubstanceDataConfiguration INSTANCE() {
    	return _instanceSupplier.get();
    }
}
