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
	private SubstanceRepository substanceRepository;

	@Autowired
	private RelationshipRepository relationshipRepository;

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


	/**
	 * This is really "pre-create" only called when new object persisted for 1st time- not updates
	 * @param thisRelationship
	 */
	@Override
	public void prePersist(Relationship thisRelationship) {

	    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
	    transactionTemplate.setReadOnly(true);
	    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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

	private boolean notWorkingOn(String uuid){
		return notWorkingOn(uuid,false);
	}

	private boolean notWorkingOn(String uuid, boolean isRemove){
//		System.out.println("working on list =" + relationshipUuidsBeingWorkedOn);
		if(relationshipUuidsBeingWorkedOn.add(uuid)){
			if(isRemove){
//				System.out.println("Adding delete flag for uuid:" + uuid);
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
	public void postPersist(Relationship obj) throws FailProcessingException {
		relationshipUuidsBeingWorkedOn.removeCompletely(obj.getOrGenerateUUID().toString());
		relationshipUuidsBeingDeleted.removeCompletely(obj.getOrGenerateUUID().toString());
//		System.out.println("Persist done for:" + obj.uuid.toString());
	}

	@Override
	public void postUpdate(Relationship obj) throws FailProcessingException {
		relationshipUuidsBeingWorkedOn.removeCompletely(obj.getOrGenerateUUID().toString());
		relationshipUuidsBeingDeleted.removeCompletely(obj.getOrGenerateUUID().toString());
//		System.out.println("Update done for:" + obj.uuid.toString());
	}

	@Override
	public Class<Relationship> getEntityClass() {
		return Relationship.class;
	}

	@Override
	public void postRemove(Relationship obj) throws FailProcessingException {
		relationshipUuidsBeingWorkedOn.removeCompletely(obj.getOrGenerateUUID().toString());
		relationshipUuidsBeingDeleted.removeCompletely(obj.getOrGenerateUUID().toString());
//		System.out.println("Removal done for:" + obj.uuid.toString());
	}

	/**
	 * Test to see if the given relationship can be inverted and created for the
	 * other substance. Specifically, it tests that the relationship is:
	 *
	 * <ol>
	 * <li>New</li>
	 * <li>Not otherwise automatically inverted</li>
	 * <li>Invertible</li>
	 * <li>That the inverted relationship doesn't already exist on target substance</li>
	 * </ol>
	 *
	 *
	 * @param obj
	 * @param oldSub
	 * @param newSub
	 * @return
	 */
	public boolean canCreateInverseFor(Relationship obj, SubstanceReference oldSub, Substance newSub){
		if(obj.isAutomaticInvertible()){
			if(newSub!=null){
				Relationship r = obj.fetchInverseRelationship();
				for(Relationship rOld:newSub.relationships){
					if(r.type.equals(rOld.type) && oldSub.refuuid.equals(rOld.relatedSubstance.refuuid)){
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}


	private Optional<Relationship> getRealInvertedRelationshipToRealRelationship(Relationship obj){
		List<Relationship> rel;
		try {
			if (obj.isGenerator()) {
//			System.out.println("Finding relationship that is child of this generator");
				rel = relationshipRepository.findByOriginatorUuid(obj.getOrGenerateUUID().toString());
			} else {
//			System.out.println("Finding relationship that parent of this child non-generator");
				rel = relationshipRepository.findByOriginatorUuid(obj.originatorUuid);
			}

		}catch(Throwable t){
			t.printStackTrace();
			throw t;
		}
		return rel.stream()
				.filter(rr->!rr.getOrGenerateUUID().equals(obj.getOrGenerateUUID()))
				.findAny();
	}

	private enum InverseMethod{
		EXPLICIT,
		SAME_TYPE_ISOLATED,
		SAME_TYPE_BEST_MATCH
	}

	public Optional<Tuple<Relationship,InverseMethod>> findRealExplicitOrImplicitInvertedRelationship(Relationship obj1){
		Optional<Relationship> opR=getRealInvertedRelationshipToRealRelationship(obj1);
		if(opR.isPresent())return opR.map(rr->Tuple.of(rr, InverseMethod.EXPLICIT));
		SubstanceReference parentRef=obj1.fetchOwner().asSubstanceReference();
		if(obj1.isAutomaticInvertible()){
//			System.out.println("Looking at old related substance for possible inversions");
			Substance relatedSubstance = substanceRepository.findBySubstanceReference(obj1.relatedSubstance);
			//GSRS-860 sometimes when grabbing substance json from public data
			//and loading it on local system and then making edits without pulling latest version from GSRS
			//we edit/ remove references that were system generated or point to other substances
			//that weren't also loaded so check to make sure we have the related substance
			if(relatedSubstance !=null && relatedSubstance.relationships !=null) {
//					System.out.println("Found old referenced substance:" + relatedSubstance.getName());

				List<Relationship> candidates = relatedSubstance.relationships.stream()
						.filter(r->r.relatedSubstance.isEquivalentTo(parentRef))
						.filter(r -> r.isAutomaticInvertible() && r.fetchInverseRelationship().type.equals(obj1.type))
						.collect(Collectors.toList());

				if (candidates.size() == 1) {

//						System.out.println("Inference successful");
					return Optional.of(Tuple.of(candidates.get(0), InverseMethod.SAME_TYPE_ISOLATED));
				}else if (candidates.size() == 0) {
//						System.out.println("Found no suitable relationships");
					return Optional.empty();
				}else{
//						System.out.println("Found too many possible inverse relationships:" + candidates.size());

					RelationshipHash rex= RelationshipHash.of(obj1);
					Relationship r2=candidates.stream()
							.map(r->Tuple.of(r, RelationshipHash.of(r)))
							.map(Tuple.vmap(hh->hh.matchLevel(rex)))
							.sorted(Comparator.comparing(t->-t.v()))
							.findFirst()
							.map(t->{
//						        	  System.out.println("Best one matches at level:" + t.v());
								return t.k();
							})
							.orElse(null)
							;

					return Optional.of(Tuple.of(r2, InverseMethod.SAME_TYPE_BEST_MATCH));

				}
			}
		}
		return Optional.empty();
	}
	private static class RelationshipHash{
		String[] levels = new String[5];

		public RelationshipHash(Relationship r){
			levels[0]=r.relatedSubstance.refuuid;
			levels[1]=r.type;
			levels[2]=r.qualification + ":" + r.interactionType;
			levels[3]=(r.mediatorSubstance!=null)?r.mediatorSubstance.refuuid:"null";
			levels[4]=(r.amount!=null)?r.amount.toString():"null";
		}

		public int matchLevel(RelationshipHash rhash){
			for(int i=0;i<levels.length;i++){
				if(!rhash.levels[i].equals(this.levels[i])){
					return i-1;
				}
			}
			return levels.length-1;
		}
		static RelationshipHash of(Relationship r){
			return new RelationshipHash(r);
		}
	}


	@Override
	public void preUpdate(Relationship obj) {

		if(!notWorkingOn(obj.getOrGenerateUUID().toString())){
			return;
		}
		UpdateInverseRelationshipEvent.UpdateInverseRelationshipEventBuilder builder = UpdateInverseRelationshipEvent.builder();
		builder.relationshipIdThatWasUpdated(obj.uuid);
		builder.substanceIdToUpdate(UUID.fromString(obj.relatedSubstance.refuuid));
		if(obj.isGenerator()){
			builder.originatorIdToUpdate(obj.uuid);
		}else{
			builder.originatorIdToUpdate(UUID.fromString(obj.originatorUuid));
		}

		eventPublisher.publishEvent(builder.build());
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
