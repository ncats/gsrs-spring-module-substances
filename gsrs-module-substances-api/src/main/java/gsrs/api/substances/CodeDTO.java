package gsrs.api.substances;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeDTO extends BaseEditableDTO{
    private UUID uuid;

    private String name;
    private String displayName;
    private boolean preferred;
    private String type;
    private String code;
    private String codeSystem;

    private Set<String> access = new LinkedHashSet<>();
    private Set<UUID> references = new LinkedHashSet<>();
}
