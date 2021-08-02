package gsrs.module.substance.processors;

import gsrs.EntityPersistAdapter;
import gsrs.module.substance.services.RelationshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

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
    public void createInverse(TryToCreateInverseRelationshipEvent event) {
        relationshipService.createNewInverseRelationshipFor(event);

    }
}
