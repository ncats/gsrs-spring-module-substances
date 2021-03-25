package gsrs.module.substance;

import gsrs.springUtils.AutowireHelper;
import ix.core.chem.StructureHasher;
import ix.core.chem.StructureStandardizer;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StructureProcessingConfiguration {
    /*
    ix.structure-hasher = "ix.core.chem.LychiStructureHasher"
ix.structure-standardizer="ix.core.chem.LychiStandardizer"
     */
    @Value("${ix.structure-hasher}")
    private String structureHasherName;
    @Value("${ix.structure-standardizer}")
    private String structureStandardizerName;


    @Bean
    public StructureHasher structureHashserInstance() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (StructureHasher) Class.forName(structureHasherName).newInstance();
    }

}
