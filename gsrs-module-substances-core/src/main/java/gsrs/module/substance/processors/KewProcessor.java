package gsrs.module.substance.processors;

import gsrs.module.substance.kew.KewControlledPlantDataSet;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Substance;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * This Substance Processor adds KEW tags if the approval ID is in the list of KEW records as specified in the KEW.json file.
 */
public class KewProcessor implements EntityProcessor<Substance> {

    private final KewControlledPlantDataSet kewData;
    public KewProcessor(){
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        try {
            kewData = new KewControlledPlantDataSet(resourceLoader.getResource("classpath:kew.json"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public KewProcessor(KewControlledPlantDataSet kewData) {
        this.kewData = Objects.requireNonNull(kewData);
    }

    /**
     * Adds/remove Kew tag for controlled kew substances
     * @param s
     */
    private void addKewIfPossible(Substance s){
        if(kewData!=null){
            if(s.approvalID!=null && s.isPrimaryDefinition()){
                if(kewData.contains(s.approvalID)){
                    s.addTagString("KEW");
                }else{
                    if(s.hasTagString("KEW")){
                        s.removeTagString("KEW");
                    }
                }
            }
        }
    }
    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

    @Override
    public void prePersist(Substance obj) throws FailProcessingException {
        addKewIfPossible(obj);
    }

    @Override
    public void preUpdate(Substance obj) throws FailProcessingException {
        addKewIfPossible(obj);
    }
}
