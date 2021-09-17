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
public class PolymerDTO extends SubstanceComponentBaseDTO{

    private UUID uuid;

    private PolymerClassificationDTO classification;

    private StructureDTO displayStructure;

    private StructureDTO idealizedStructure;
}
