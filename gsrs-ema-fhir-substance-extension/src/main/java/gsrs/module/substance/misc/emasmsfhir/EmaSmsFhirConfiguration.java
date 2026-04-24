package gsrs.module.substance.misc.emasmsfhir;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
@ConfigurationProperties("ix.core.emasmsfhir")
@Data
@Slf4j
@ComponentScan(basePackages="gsrs.module.substance.misc.emasmsfhir")
public class EmaSmsFhirConfiguration {
    private Map<String, Map<String, String>> codeConfigs;
    private Map<String, Map<String, String>> substanceTypeConfigs;
    private Map<String, Map<String, String>> miscDefaultConfigs;
}