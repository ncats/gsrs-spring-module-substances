package gsrs.module.substance.processors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveInverseRelationshipEvent {

    private UUID relationshipIdThatWasRemoved;

    private UUID relationshipOriginatorIdToRemove;

    private String relationshipTypeThatWasRemoved;

    private String substanceRefIdOfRemovedRelationship;

    private String relatedSubstanceRefId;
}
