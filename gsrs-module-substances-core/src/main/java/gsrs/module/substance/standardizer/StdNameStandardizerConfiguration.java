package gsrs.module.substance.standardizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Map;

@Configuration
@ConfigurationProperties("gsrs.standardizers.substances.stdname")
@Data
public class StdNameStandardizerConfiguration {

    private Class standardizerClass;
    private Map<String, Object> parameters;

    @Bean
    @ConditionalOnMissingBean
    public NameStandardizer getNameStandardizer() {
        if(standardizerClass == null) {
            return new HtmlStdNameStandardizer();
        }
        ObjectMapper mapper = new ObjectMapper();

        if(parameters == null) {
            return (NameStandardizer) mapper.convertValue(Collections.emptyMap(), standardizerClass);
        }
        return (NameStandardizer) mapper.convertValue(parameters, standardizerClass);
    }
}
