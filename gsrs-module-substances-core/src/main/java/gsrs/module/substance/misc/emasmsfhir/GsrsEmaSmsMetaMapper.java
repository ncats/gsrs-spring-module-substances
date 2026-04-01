package gsrs.module.substance.misc.emasmsfhir;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Data
public class GsrsEmaSmsMetaMapper {
    @Value("${ix.core.GsrsEmaSmsMetaMap.gsrsDomains.codeSystems}")
    private Map<String, Map<String, String>> codeSystems = new HashMap<>();

    @Value("${ix.core.GsrsEmaSmsMetaMap.gsrsDomains.substanceClasses}")
    private Map<String, Map<String, String>> substanceClasses = new HashMap<>();
}