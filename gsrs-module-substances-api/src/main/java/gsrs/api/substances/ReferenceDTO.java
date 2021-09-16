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
public class ReferenceDTO extends BaseEditableDTO{
    private UUID uuid;

    private String citation;
    private String docType;
    private boolean publicDomain;
    private String type;

    private String url;

    private List<String> tags = new ArrayList<>();
    private Set<String> access = new LinkedHashSet<>();
}
