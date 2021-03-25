package gsrs.module.substance.exporters;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class SubstanceSpreadhseetExporterConfiguration {

    @Value("${ix.gsrs.delimitedreports.inchikeysforambiguousstereo}")
    private boolean includeInChiKeysAnyway = false;
}
