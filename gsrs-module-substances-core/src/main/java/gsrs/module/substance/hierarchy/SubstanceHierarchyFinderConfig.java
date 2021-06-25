package gsrs.module.substance.hierarchy;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties("substance.hierarchy-finders")
@Data
public class SubstanceHierarchyFinderConfig {

    private List<HierarchyFinderRecipe> recipes;
}
