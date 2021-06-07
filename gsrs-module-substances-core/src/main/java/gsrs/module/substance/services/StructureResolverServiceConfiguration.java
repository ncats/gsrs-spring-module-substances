package gsrs.module.substance.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties("gsrs.resolvers")
@Data
public class StructureResolverServiceConfiguration {
    @Data
    public static class ResolverImplConf{
//        @JsonProperty("class")
        private String resolverClass;

        private Map<String, Object> parameters;

    }

    private List<ResolverImplConf> implementations;
}
