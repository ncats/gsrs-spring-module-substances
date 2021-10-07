package gsrs.module.substance.processors;

import gov.nih.ncats.common.sneak.Sneak;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class GroupProcessor implements EntityProcessor<Group> {

    @Autowired
    private ControlledVocabularyApi cvApi;

    private final String ACCESS_DOMAIN = "ACCESS_GROUP";

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void postPersist(Group obj) {
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
                vt =GsrsVocabularyTermDTO.builder()
                        .display(obj.name)
                        .value(obj.name)
                        .build();
                cvv.get().getTerms().add(vt);
                //not sure why term type is ix.ginas.models.v1.ControlledVocabulary but other values lead to errors
                if( cvv.get().getVocabularyTermType() == null || cvv.get().getVocabularyTermType().length()==0) {
                    cvv.get().setVocabularyTermType("ix.ginas.models.v1.ControlledVocabulary");
                }
                try {
                    cvApi.update(cvv.get());
                } catch (IOException ex) {
                    log.error("Error updating CV", ex);
                    Sneak.sneakyThrow(ex);
                }
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void postUpdate(Group obj) {
        postPersist(obj);
    }

    @Override
    public Class<Group> getEntityClass() {
        return Group.class;
    }

}
