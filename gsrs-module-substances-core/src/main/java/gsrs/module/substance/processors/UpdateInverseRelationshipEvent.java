package gsrs.module.substance.processors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.sleepycat.persist.model.Relationship;

import gsrs.module.substance.processors.TryToCreateInverseRelationshipEvent.CreationMode;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInverseRelationshipEvent {

    private UUID relationshipIdThatWasUpdated;
    private UUID substanceIdThatWasUpdated;

    private UUID originatorUUID;
    private UUID substanceIdToUpdate;
    
    
    /**
     * Instantiate the equivalent {@link TryToCreateInverseRelationshipEvent} event,
     * for use when an update actually requires a delete operation and a new persist.
     * This is typical when the {@link ix.ginas.models.v1.Relationship#relatedSubstance}
     * has changed, but the {@link ix.ginas.models.v1.Relationship#getUuid()} has stayed
     * the same.
     * @return
     */
    public TryToCreateInverseRelationshipEvent toCreateEvent() {
    	// TP 2022-11-29: this code used to have a bug where it would
    	// set the from substance AND to substance to be the same, causing unintentional self-referencing
    	// relationships. This is fixed in 3.0.3.
        return TryToCreateInverseRelationshipEvent.builder()
         .creationMode(CreationMode.CREATE_IF_MISSING)
         .relationshipIdToInvert(relationshipIdThatWasUpdated)
         .originatorUUID(originatorUUID)
         .fromSubstance(substanceIdToUpdate)
         .toSubstance(substanceIdThatWasUpdated)
         .build();
    }
}
