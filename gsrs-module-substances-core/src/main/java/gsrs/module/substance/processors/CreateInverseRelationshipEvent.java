package gsrs.module.substance.processors;

import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInverseRelationshipEvent {

    private UUID toSubstance;

    private UUID fromSubstance;

    private UUID originatorSubstance;
    private UUID relationshipIdToInvert;

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
