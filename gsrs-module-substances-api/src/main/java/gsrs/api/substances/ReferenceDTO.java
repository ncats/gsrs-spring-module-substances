package gsrs.api.substances;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferenceDTO extends SubstanceComponentBaseDTO{

    private UUID uuid;
    private String citation;
    private String docType;
    private boolean publicDomain;
    private String type;

    private String url;

}
