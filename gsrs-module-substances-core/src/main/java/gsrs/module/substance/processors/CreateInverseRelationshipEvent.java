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
public class CreateInverseRelationshipEvent {

    private UUID toSubstance;

    private UUID fromSubstance;

    private UUID originatorSubstance;
    private UUID relationshipIdToInvert;

    private boolean forceCreation;

    private UUID newRelationshipId;
}
