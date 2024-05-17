package ix.core.chem;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties("gsrs.structure.chemical")
@Data
public class StructureProcessorConfiguration {

    private Map<String,Object> molwitch;
}
