package ix.ginas.utils.validation.strategy;

import ix.core.validator.GinasProcessingMessage;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("gsrs.processing-strategy")
public class GsrsProcessingStrategyFactoryConfiguration {
    private String defaultStrategy;
    private List<OverrideRule> overrideRules;

    @Data
    public static class OverrideRule {
        private Pattern regex;
        private GinasProcessingMessage.ACTION_TYPE actionType;
        private GinasProcessingMessage.MESSAGE_TYPE messageType;
    }
}
