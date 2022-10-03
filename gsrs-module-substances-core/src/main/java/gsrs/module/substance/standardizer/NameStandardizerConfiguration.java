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
@ConfigurationProperties("gsrs.standardizers.substances.name")
@Data
public class NameStandardizerConfiguration {

    private Class standardizerClass;
    private Map<String, Object> parameters;

    @Bean
    @ConditionalOnMissingBean
    public NameStandardizer getNameStandardizer() {
        if(standardizerClass == null) {
            HtmlNameStandardizer standardizer = new HtmlNameStandardizer();
            String[] searchStrings = {"\\p{C}", "\\s{2,}", "\u00B9", "\u00B2", "\u00B3", "</sup><sup>", "</sub><sub>", "</i><i>"};
            String[] replaceStrings = {"", " ", "<sup>1</sup>", "<sup>2</sup>", "<sup>3</sup>", "", "", ""};
            standardizer.setSearch(searchStrings);
            standardizer.setReplace(replaceStrings);
            return (NameStandardizer) standardizer;
        }
        ObjectMapper mapper = new ObjectMapper();

        if(parameters == null) {
            return (NameStandardizer) mapper.convertValue(Collections.emptyMap(), standardizerClass);
        }
        return (NameStandardizer) mapper.convertValue(parameters, standardizerClass);
    }
}
