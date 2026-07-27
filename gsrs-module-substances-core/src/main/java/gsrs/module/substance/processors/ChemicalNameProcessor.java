package gsrs.module.substance.processors;

import gsrs.module.substance.services.IupacNameService;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChemicalNameProcessor implements EntityProcessor<Substance> {

    private IupacNameService service = new IupacNameService();

    @Override
    public void prePersist(Substance obj) throws FailProcessingException {
        if(obj instanceof ChemicalSubstance){
            try {
                service.ensureIupacName  ((ChemicalSubstance)obj);
            }
            catch (Exception ex){
                log.error("Error looking up PubChem name for {}", obj.getOrGenerateUUID());
            }
        }
    }

    @Override
    public void preUpdate(Substance obj) throws FailProcessingException {
        if(obj instanceof ChemicalSubstance){
            try {
                service.ensureIupacName  ((ChemicalSubstance)obj);
            }
            catch (Exception ex){
                log.error("Error looking up PubChem name for {}", obj.getOrGenerateUUID());
            }
        }
    }

    @Override
    public Class<Substance> getEntityClass() {
        return Substance.class;
    }

}
