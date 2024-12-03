package gsrs.module.substance;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("gsrs.substance.structures")
@Data
@Slf4j
public class StructureHandlingConfiguration {

    private String saltFilePath;

    private String resolverBaseUrl;
}
