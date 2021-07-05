package gsrs.module.substance;


import ix.core.chem.StructureHasher;
import ix.core.chem.StructureProcessor;
import ix.core.chem.StructureStandardizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StructureProcessingConfiguration {

    @Value("${ix.structure-hasher}")
    private String structureHasherName;
    @Value("${ix.structure-standardizer}")
    private String structureStandardizerName;


    @Bean
    public StructureHasher structureHashserInstance() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (StructureHasher) Class.forName(structureHasherName).newInstance();
    }

    @Bean
    public StructureProcessor structureProcessor(StructureStandardizer standardizer, StructureHasher hasher){
        return new StructureProcessor(standardizer, hasher);
    }
}
