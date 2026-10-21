package gsrs.module.substance;

import java.util.Map;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("gsrs.substance.structures")
@Data
@Slf4j
public class StructureHandlingConfiguration {

    /**
     * Salt component data used by {@link gsrs.module.substance.utils.ChemicalUtils} when stripping
     * salts from a structure.  The keys are the InChIKeys of salt components and the values are
     * their SMILES representations.
     *
     * The data used to be read from an external tab separated file named by {@code saltFilePath};
     * it is now supplied directly through configuration (see the {@code salt-data.conf} defaults).
     */
    private Map<String, String> saltData;
}
