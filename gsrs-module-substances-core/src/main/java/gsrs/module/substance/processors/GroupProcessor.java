package gsrs.module.substance.processors;

import gsrs.cv.api.ControlledVocabularyApi;
import ix.core.EntityProcessor;
import ix.core.models.Group;
import gsrs.cv.api.GsrsControlledVocabularyDTO;
import gsrs.cv.api.GsrsVocabularyTermDTO;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class GroupProcessor implements EntityProcessor<Group> {

    @Autowired
    private ControlledVocabularyApi cvApi;

    private final String ACCESS_DOMAIN = "ACCESS_GROUP";

    @Override
    public void prePersist(Group obj) {
        log.debug("GroupProcessor.prePersist");
        Optional<GsrsControlledVocabularyDTO> cvv = null;
        try {
            cvv = cvApi.findByDomain(ACCESS_DOMAIN);
        } catch (IOException ex) {
            Logger.getLogger(GroupProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        GsrsVocabularyTermDTO vt = null;
        if (cvv != null && cvv.isPresent()) {
            log.trace("cvv found");
            Optional<GsrsVocabularyTermDTO> term = cvv.get().getTerms().stream().filter(t -> t.getValue().equals(obj.name)).findFirst();
            if (term.isPresent()) {
                vt = term.get();
            }

            //log.debug("The domain is:" + cvv.domain + " with " + cvv.terms.size() + " terms");
            if (vt == null) {
                log.debug("Group didn't exist before");
                vt = new GsrsVocabularyTermDTO();
                vt.setDisplay(obj.name);
                vt.setValue(obj.name);
                //vt.save();
                cvv.get().getTerms().add(vt);
                if( cvv.get().getVocabularyTermType() == null || cvv.get().getVocabularyTermType().length()==0) {
                    cvv.get().setVocabularyTermType("ix.ginas.models.v1.ControlledVocabulary");
                }
                try {
                    cvApi.update(cvv.get());
                } catch (IOException ex) {
                    log.error("Error updating CV", ex);
                    //throw ex;
                }
            }
        }
    }

    @Override
    public void preUpdate(Group obj) {
        prePersist(obj);
    }

    @Override
    public void postPersist(Group obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void preRemove(Group obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void postRemove(Group obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void postUpdate(Group obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void postLoad(Group obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public Class<Group> getEntityClass() {
        return Group.class;
    }

}
