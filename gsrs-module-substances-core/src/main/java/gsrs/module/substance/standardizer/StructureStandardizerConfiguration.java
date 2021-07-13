package gsrs.module.substance.standardizer;

import ix.core.chem.AbstractStructureStandardizer;
import ix.core.chem.InchiStandardizer;
import ix.core.chem.StructureStandardizer;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;

@Configuration
@Data
public class StructureStandardizerConfiguration {
    @Value("${ix.core.structureIndex.atomLimit}")
    private int maxNumberOfAtoms = 240;
    @Value(value ="${ix.structure-standardizer}")
    private Class<? extends AbstractStructureStandardizer> standardizerClass = InchiStandardizer.class;
    @Bean
    @ConditionalOnMissingBean
    public StructureStandardizer getStructureStandardizer() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return standardizerClass.getDeclaredConstructor(int.class).newInstance(maxNumberOfAtoms);
    }
}
