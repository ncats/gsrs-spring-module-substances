package gsrs.module.substance.processors;

import gsrs.EntityPersistAdapter;
import gsrs.module.substance.services.RelationshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

@Component
public class RelationEventListener {
    private static final ThreadLocal<Set<String>> IN_FLIGHT_EVENTS = ThreadLocal.withInitial(HashSet::new);

    @Autowired
    private RelationshipService relationshipService;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeInverseRelationship(RemoveInverseRelationshipEvent event) {
        runIfNotReentrant(eventKey("remove", event.getRelationshipIdThatWasRemoved(),
                        event.getRelationshipOriginatorIdToRemove(),
                        event.getSubstanceRefIdOfRemovedRelationship(),
                        event.getRelatedSubstanceRefId()),
                () -> {
                    relationshipService.removeInverseRelationshipFor(event);
                    return null;
                });
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateInverseRelationship(UpdateInverseRelationshipEvent event) {
        runIfNotReentrant(eventKey("update",
                        event.getRelationshipIdThatWasUpdated(),
                        event.getOriginatorUUID(),
                        event.getSubstanceIdThatWasUpdated(),
                        event.getSubstanceIdToUpdate()),
                () -> {
                    relationshipService.updateInverseRelationshipFor(event);
                    return null;
                });
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createInverse(TryToCreateInverseRelationshipEvent event) {
        runIfNotReentrant(eventKey("create",
                        event.getRelationshipIdToInvert(),
                        event.getOriginatorUUID(),
                        event.getFromSubstance(),
                        event.getToSubstance()),
                () -> {
                    relationshipService.createNewInverseRelationshipFor(event);
                    return null;
                });

    }

    private static String eventKey(String prefix, Object... parts) {
        StringBuilder builder = new StringBuilder(prefix);
        for (Object part : parts) {
            builder.append('|').append(part);
        }
        return builder.toString();
    }

    private static <T> T runIfNotReentrant(String key, Supplier<T> action) {
        Set<String> inFlight = IN_FLIGHT_EVENTS.get();
        if (!inFlight.add(key)) {
            return null;
        }
        try {
            return action.get();
        } finally {
            inFlight.remove(key);
            if (inFlight.isEmpty()) {
                IN_FLIGHT_EVENTS.remove();
            }
        }
    }
}
