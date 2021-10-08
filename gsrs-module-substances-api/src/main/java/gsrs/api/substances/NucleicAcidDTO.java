package gsrs.api.substances;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NucleicAcidDTO extends SubstanceComponentBaseDTO{

    private UUID uuid;

    private String nucleicAcidType;

    private List<String> nucleicAcidSubType = new ArrayList<>();


    private String sequenceOrigin;

    private String sequenceType;

    private List<SubunitDTO> subunits = new ArrayList<>();
}
