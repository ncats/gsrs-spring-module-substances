package gsrs.module.substance.services;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("ix.ginas.structure.search")
@Data
@Accessors(fluent = true)
public class StructureSearchConfiguration {
    private boolean includePolymers;
    private boolean includeModifications;
    private boolean includeMixtures;
}
