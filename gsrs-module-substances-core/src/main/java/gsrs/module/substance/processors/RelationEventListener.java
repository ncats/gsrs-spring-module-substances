package gsrs.module.substance.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import gsrs.EntityPersistAdapter;
import gsrs.module.substance.repository.RelationshipRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.services.RelationshipService;
import ix.core.models.Keyword;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
public class RelationEventListener {

    @Autowired
    private EntityPersistAdapter entityPersistAdapter;

    @Autowired
    private RelationshipService relationshipService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeInverseRelationship(RemoveInverseRelationshipEvent event) {
        relationshipService.removeInverseRelationshipFor(event);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateInverseRelationship(UpdateInverseRelationshipEvent event) {
        relationshipService.updateInverseRelationshipFor(event);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createInverse(CreateInverseRelationshipEvent event) {
        relationshipService.createNewInverseRelationshipFor(event);

    }
}
