package gsrs.module.substance.processors;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import ix.core.EntityProcessor;
import ix.core.util.SemaphoreCounter;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import lombok.extern.slf4j.Slf4j;

/**
 * Processor to handle both sides of the Relationship.
 * Because GSRS Relationships are actually 2 separate objects,
 * each a member of a different Substance that's related;
 * we have to make sure any change or deletion of one
 * performs a "spooky action at a distance" to the other one.
 * This processor also handles creating the inverse relationship
 * on the other Substance if a user creates a new relationship on a Substance
 * to keep everything in sync.
 */
@Slf4j
public class RelationshipProcessor implements EntityProcessor<Relationship> {

    @Autowired
    private PlatformTransactionManager transactionManager;


    @Autowired
    private ApplicationEventPublisher eventPublisher;



    ThreadLocal<AtomicBoolean> enabled = ThreadLocal.withInitial(()->new AtomicBoolean(true));
    
	//TODO:
	/*
	 * There are some issues remaining here, specifically the following:
	 *
	 * 1. The relationship inverse / updating code fails if a concept is being upgraded.
	 *    Almost all relationships that were added to the concept record will be removed from the opposite records.
	 *    This appears to only really be an issue if the relationships were pointing toward the concept originally, rather than being stored on the concept to begin with. [should work now]
	 * 2. Access settings are not copied over when a relationship is updated / added. [should work now]
	 * 3. If you change the type of a relationship to a one-way type, it won't do anything with the inverted relationship, which causes inconsistency [should work now]
	 * 4. If you change the related substance of a relationship, it neither deletes the inverse nor adds the new inverse [should work now]
	 * 5. If a relationship gets in an unstable state from any of the above, and is missing an inverse, no changes to that relationship will generate an inverse.
	 *    It would need to be removed and added again. [should work now]
	 *
	 *
	 * We need tests for each of the above.
	 *
	 *
	 *
	 *
	 *
	 */


	private static final String MENTION = "mention";

	

	@Override
	public Class<Relationship> getEntityClass() {
		return Relationship.class;
	}


    
    /**
     * This is really "pre-create" only called when new object persisted for 1st time- not updates
     * @param thisRelationship
     */
	@Override
	public void prePersist(Relationship thisRelationship) {
	    if(!enabled.get().get()) return;
	    
	    if (thisRelationship.isAutomaticInvertible()) {
	        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
	        transactionTemplate.executeWithoutResult( stauts -> {

	            TryToCreateInverseRelationshipEvent event = new TryToCreateInverseRelationshipEvent();
	            final Substance thisSubstance = thisRelationship.fetchOwner();
	            event.setRelationshipIdToInvert(thisRelationship.getOrGenerateUUID());
	            event.setToSubstance(thisSubstance.getOrGenerateUUID());
	            if(thisRelationship.isGenerator()){
	                event.setOriginatorUUID(thisRelationship.uuid);
	            }else{
	                event.setOriginatorUUID(UUID.fromString(thisRelationship.originatorUuid));
	            }
	            SubstanceReference otherSubstanceReference = thisRelationship.relatedSubstance;
	            //TODO maybe change the fromSubstance from UUID to a substance reference incase the uuid changes we could use approval id or name etc?
	            if (otherSubstanceReference != null && otherSubstanceReference.refuuid !=null) {
	                event.setFromSubstance(UUID.fromString(otherSubstanceReference.refuuid));
	            }
	            event.setCreationMode(TryToCreateInverseRelationshipEvent.CreationMode.CREATE_IF_MISSING);
	            eventPublisher.publishEvent(event);

	        });
	    }
	}

	@Override
	public void preUpdate(Relationship obj) {
	    if(!enabled.get().get()) return;

	    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
	    transactionTemplate.executeWithoutResult( stauts -> {
	        UpdateInverseRelationshipEvent.UpdateInverseRelationshipEventBuilder builder = UpdateInverseRelationshipEvent.builder();
	        builder.relationshipIdThatWasUpdated(obj.uuid);
	        builder.substanceIdToUpdate(UUID.fromString(obj.relatedSubstance.refuuid));
	        builder.substanceIdThatWasUpdated(obj.fetchOwner().getOrGenerateUUID());
	        if(obj.isGenerator()){
	            builder.originatorUUID(obj.uuid);
	        }else{
	            builder.originatorUUID(UUID.fromString(obj.originatorUuid));
	        }

	        eventPublisher.publishEvent(builder.build());
	    });

	}

	@Override
	public void preRemove(Relationship obj) {
	    if(!enabled.get().get()) return;
	    if (obj.isAutomaticInvertible()) {

	        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
	        transactionTemplate.executeWithoutResult( stauts -> {
	            RemoveInverseRelationshipEvent.RemoveInverseRelationshipEventBuilder builder = RemoveInverseRelationshipEvent.builder();
	            builder.relationshipIdThatWasRemoved(obj.getOrGenerateUUID());
	            builder.relationshipTypeThatWasRemoved(obj.type);
	            builder.substanceRefIdOfRemovedRelationship(obj.fetchOwner().getOrGenerateUUID().toString());
	            builder.relatedSubstanceRefId(obj.relatedSubstance.refuuid);
                builder.relationshipOriginatorIdToRemove(UUID.fromString(obj.originatorUuid));
	            if (obj.isGenerator()) {
	                builder.relationshipOriginatorIdToRemove(obj.getOrGenerateUUID());
	            } else {
	            }
	            eventPublisher.publishEvent(builder.build());
	        });

	    }
	}
	
	
	
	 /**
     * Disable this {@link EntityProcessor} from having events fire from operations
     * performed in the supplied {@link Runnable} within its executing thread. This
     * is accomplished by using a {@link ThreadLocal} flag which temporarily
     * disables this processor until the {@link Runnable} execution finishes. 
     * @param <T>
     * @param r
     * @return
     */
    public void doWithoutEventTracking(Runnable r) {
        doWithoutEventTracking(()->{
            r.run();
            return null;
        });
    }
    
    /**
     * Disable this {@link EntityProcessor} from having events fire from operations
     * performed in the supplied {@link Supplier} within its executing thread. This
     * is accomplished by using a {@link ThreadLocal} flag which temporarily
     * disables this processor until the {@link Supplier} finishes. The use of 
     * a {@link Supplier} here is just to allow a convenient way for processes that
     * would typically return a value to still return a value. If no value needs to be
     * returned {@link #doWithoutEventTracking(Runnable)} can be used instead.
     * @param <T>
     * @param r
     * @return
     */
    public <T> T doWithoutEventTracking(Supplier<T> r) {
        enabled.get().set(false);
        try {
            return r.get();
        }finally {
            enabled.get().set(true);
        }
    }

}
