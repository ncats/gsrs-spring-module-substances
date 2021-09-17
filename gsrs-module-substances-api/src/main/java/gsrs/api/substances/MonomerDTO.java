package gsrs.api.substances;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonomerDTO extends SubstanceComponentBaseDTO{

    private UUID uuid;

    private boolean defining;

    private String type;

    private SubstanceReferenceDTO monomerSubstance;

    private AmountDTO amount;
}
