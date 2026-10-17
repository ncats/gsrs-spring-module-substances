package gsrs.substances.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper=false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubstanceReferenceDTO extends BaseEditableDTO{

    private UUID uuid;

    private String refPname;

    private String refuuid;

    private String approvalID;

    private String linkingID;

    private String name;

    private SubstanceDTO.SubstanceClass substanceClass;

}
