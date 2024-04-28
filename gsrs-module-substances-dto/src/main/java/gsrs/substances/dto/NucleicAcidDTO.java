package gsrs.substances.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NucleicAcidDTO extends SubstanceComponentBaseDTO{

    private UUID uuid;

    private String nucleicAcidType;

    @Builder.Default
    private List<String> nucleicAcidSubType = new ArrayList<>();


    private String sequenceOrigin;

    private String sequenceType;

    @Builder.Default
    private List<SubunitDTO> subunits = new ArrayList<>();
}
