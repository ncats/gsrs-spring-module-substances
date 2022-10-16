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
@ConfigurationProperties("gsrs.standardizers.substances")
@Data
public class NameStandardizerConfiguration {

    private Map<String, Object> name;
    private Map<String, Object> stdname;

    @Bean
    public NameStandardizer nameStandardizer() throws InstantiationException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        Class standardizerClass = null;
        Map<String, Object> parameters = null;
        if (name != null) {
            standardizerClass = Class.forName((String) name.get("standardizerClass"));
            parameters = (Map<String, Object>) name.get("parameters");
        }
        if(standardizerClass == null) {
            HtmlNameStandardizer standardizer = new HtmlNameStandardizer();
            return (NameStandardizer) standardizer;
        }
        if(parameters == null) {
            return (NameStandardizer) mapper.convertValue(Collections.emptyMap(), standardizerClass);
        }
        return (NameStandardizer) mapper.convertValue(parameters, standardizerClass);
    }

    @Bean
    public NameStandardizer stdNameStandardizer() throws InstantiationException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        Class standardizerClass = null;
        Map<String, Object> parameters = null;
        if (stdname != null) {
            standardizerClass = Class.forName((String) stdname.get("standardizerClass"));
            parameters = (Map<String, Object>) stdname.get("parameters");
        }
        if(standardizerClass == null) {
            HtmlStdNameStandardizer standardizer = new HtmlStdNameStandardizer();
            return (NameStandardizer) standardizer;
        }
        if(parameters == null) {
            return (NameStandardizer) mapper.convertValue(Collections.emptyMap(), standardizerClass);
        }
        return (NameStandardizer) mapper.convertValue(parameters, standardizerClass);
    }
}
