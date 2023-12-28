package gsrs.module.substance.processors;

import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.UUID;

/**
 * Event signaling that a bi-directional {@link Relationship}
 * was created and an inverse- relationship
 * with the following properties Should be attempted
 * to be created.  These Events are published
 * without the ability to check if this inverse already
 * exists or not so care should be made to confirm
 * new entities should be persisted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TryToCreateInverseRelationshipEvent {

    private UUID toSubstance;

    private UUID fromSubstance;

    private UUID originatorUUID;
    private UUID relationshipIdToInvert;

    @Builder.Default
    private CreationMode creationMode = CreationMode.CREATE_IF_MISSING;

    private UUID newRelationshipId;

    public enum CreationMode{
        FORCE,
        CREATE_IF_MISSING{
            @Override
            public boolean shouldAdd(Relationship r, Substance from, Substance to){
                for (Relationship rOld : from.relationships) {
                    if (r.type.equals(rOld.type) && r.relatedSubstance.isEquivalentTo(rOld.relatedSubstance)) {
                        return false;
                    }
                }
                return true;
            }
        }
        ;

        public boolean shouldAdd(Relationship newRelationship, Substance from, Substance to){
            return true;
        }
    }
}
