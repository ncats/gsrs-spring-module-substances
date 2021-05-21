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
public class UpdateInverseRelationshipEvent {

    private UUID relationshipIdThatWasUpdated;

    private UUID originatorIdToUpdate;

    private UUID substanceIdToUpdate;
}
