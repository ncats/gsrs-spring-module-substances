package gsrs.module.substance.processors;

import gsrs.module.substance.services.IupacNameService;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class ChemicalNameProcessor implements EntityProcessor<Substance> {

    @Autowired
    private IupacNameService service;

    @Override
    public void prePersist(Substance obj) throws FailProcessingException {
        log.trace("IupacNameService prepersist");
        if(obj instanceof ChemicalSubstance){
            try {
                service.ensureIupacName((ChemicalSubstance)obj);
                log.trace("IupacNameService prepersist called service.ensureIupacName");
            }
            catch (Exception ex){
                log.error("Error looking up PubChem name for {}", obj.getOrGenerateUUID());
            }
        }
    }

    @Override
    public void preUpdate(Substance obj) throws FailProcessingException {
        log.trace("IupacNameService preUpdate called");
        if(obj instanceof ChemicalSubstance){
            try {
                service.ensureIupacName((ChemicalSubstance)obj);
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
