package gsrs.module.substance.processors;

import gsrs.module.substance.repository.RelationshipRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gov.nih.ncats.common.Tuple;
import ix.core.EntityProcessor;

import ix.core.util.SemaphoreCounter;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

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


	//These fields keep track of what UUIDs we are in the middle
	//of processing since several of these actions will trigger
	//other updates/creates/delete calls on this processor for the
	//other side of the relationship and we don't want to get trapped in a cycle.

	private SemaphoreCounter<String> relationshipUuidsBeingWorkedOn = new  SemaphoreCounter<String>();

	private SemaphoreCounter<String> relationshipUuidsBeingDeleted = new  SemaphoreCounter<String>();



	private boolean notWorkingOn(String uuid){
		return notWorkingOn(uuid,false);
	}

	private boolean notWorkingOn(String uuid, boolean isRemove){
		if(relationshipUuidsBeingWorkedOn.add(uuid)){
			if(isRemove){
				relationshipUuidsBeingDeleted.add(uuid);
			}
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter(){
				@Override
				public void afterCompletion(int status) {
					//this should be called if commit or rollback
					relationshipUuidsBeingWorkedOn.removeCompletely(uuid);
					relationshipUuidsBeingDeleted.removeCompletely(uuid);
				}
			});
			return true;
		}
		return false;
	}


	@Override
	public Class<Relationship> getEntityClass() {
		return Relationship.class;
	}


    @Override
    public void postPersist(Relationship obj) throws FailProcessingException {
        relationshipUuidsBeingWorkedOn.removeCompletely(obj.getOrGenerateUUID().toString());
        relationshipUuidsBeingDeleted.removeCompletely(obj.getOrGenerateUUID().toString());
    }

    @Override
    public void postUpdate(Relationship obj) throws FailProcessingException {
        relationshipUuidsBeingWorkedOn.removeCompletely(obj.getOrGenerateUUID().toString());
        relationshipUuidsBeingDeleted.removeCompletely(obj.getOrGenerateUUID().toString());
    }
	@Override
	public void postRemove(Relationship obj) throws FailProcessingException {
		relationshipUuidsBeingWorkedOn.removeCompletely(obj.getOrGenerateUUID().toString());
		relationshipUuidsBeingDeleted.removeCompletely(obj.getOrGenerateUUID().toString());
	}

    /**
     * This is really "pre-create" only called when new object persisted for 1st time- not updates
     * @param thisRelationship
     */
	@Override
	public void prePersist(Relationship thisRelationship) {

	    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
//	    transactionTemplate.setReadOnly(true);
//	    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	    transactionTemplate.executeWithoutResult( stauts -> {
	        if (thisRelationship.isAutomaticInvertible()) {
	            TryToCreateInverseRelationshipEvent event = new TryToCreateInverseRelationshipEvent();
	            final Substance thisSubstance = thisRelationship.fetchOwner();
	            event.setRelationshipIdToInvert(thisRelationship.getOrGenerateUUID());
	            event.setToSubstance(thisSubstance.getOrGenerateUUID());
	            event.setOriginatorSubstance(thisSubstance.getOrGenerateUUID());
	            SubstanceReference otherSubstanceReference = thisRelationship.relatedSubstance;
	            //TODO maybe change the fromSubstance from UUID to a substance reference incase the uuid changes we could use approval id or name etc?
	            if (otherSubstanceReference != null && otherSubstanceReference.refuuid !=null) {
	                event.setFromSubstance(UUID.fromString(otherSubstanceReference.refuuid));
	            }
	            event.setCreationMode(TryToCreateInverseRelationshipEvent.CreationMode.CREATE_IF_MISSING);
	            eventPublisher.publishEvent(event);
	        }
	    });
	}

	@Override
	public void preUpdate(Relationship obj) {

	    if(!notWorkingOn(obj.getOrGenerateUUID().toString())){
	        return;
	    }

	    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
	    transactionTemplate.setReadOnly(true);
	    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	    transactionTemplate.executeWithoutResult( stauts -> {
	        UpdateInverseRelationshipEvent.UpdateInverseRelationshipEventBuilder builder = UpdateInverseRelationshipEvent.builder();
	        builder.relationshipIdThatWasUpdated(obj.uuid);
	        builder.substanceIdToUpdate(UUID.fromString(obj.relatedSubstance.refuuid));
	        if(obj.isGenerator()){
	            builder.originatorIdToUpdate(obj.uuid);
	        }else{
	            builder.originatorIdToUpdate(UUID.fromString(obj.originatorUuid));
	        }

	        eventPublisher.publishEvent(builder.build());
	    });

	}

	@Override
	public void preRemove(Relationship obj) {

	    if(!notWorkingOn(obj.getOrGenerateUUID().toString(),true)){
	        return;
	    }

	    if (obj.isAutomaticInvertible()) {

	        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
	        transactionTemplate.setReadOnly(true);
	        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	        transactionTemplate.executeWithoutResult( stauts -> {
	            RemoveInverseRelationshipEvent.RemoveInverseRelationshipEventBuilder builder = RemoveInverseRelationshipEvent.builder();
	            builder.relationshipIdThatWasRemoved(obj.getOrGenerateUUID());
	            builder.relationshipTypeThatWasRemoved(obj.type);
	            builder.substanceRefIdOfRemovedRelationship(obj.fetchOwner().getOrGenerateUUID().toString());
	            builder.relatedSubstanceRefId(obj.relatedSubstance.refuuid);
	            if (obj.isGenerator()) {
	                builder.relationshipOriginatorIdToRemove(obj.getOrGenerateUUID());
	            } else {
	                builder.relationshipOriginatorIdToRemove(UUID.fromString(obj.originatorUuid));
	            }
	            eventPublisher.publishEvent(builder.build());
	        });

	    }
	}

}
