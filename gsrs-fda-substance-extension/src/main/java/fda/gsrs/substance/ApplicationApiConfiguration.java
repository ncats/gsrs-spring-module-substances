package fda.gsrs.substance;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties("gsrs.microservice.applications.api")
@Data
public class ApplicationApiConfiguration {

    private String baseURL;

    private Map<String,String> headers = new LinkedHashMap<>();

    public void configure(RestTemplateBuilder restTemplateBuilder){
        for(Map.Entry<String,String> entry: headers.entrySet()){
            restTemplateBuilder.defaultHeader(entry.getKey(), entry.getValue());
        }
    }

}
