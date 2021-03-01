package gsrs.module.substance.standardizer;

import ix.core.chem.InchiStandardizer;
import ix.core.chem.StructureStandardizer;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class StructureStandardizerConfiguration {
    @Value("ix.core.structureIndex.atomLimit")
    private int maxNumberOfAtoms = 240;

    @Bean
    @ConditionalOnMissingBean
    public StructureStandardizer getStructureStandardizer(){
        return new InchiStandardizer(getMaxNumberOfAtoms());
    }
}
