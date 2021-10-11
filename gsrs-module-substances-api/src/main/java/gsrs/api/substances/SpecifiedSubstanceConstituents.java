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
public class SpecifiedSubstanceConstituents extends SubstanceComponentBaseDTO{

    private UUID uuid;

    private String role;

    private AmountDTO amount;

    private ReferenceDTO substance;
}
