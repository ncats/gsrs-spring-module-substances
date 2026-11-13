package gsrs.substances.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper=false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MixtureComponentDTO extends BaseEditableDTO{

    private UUID uuid;
    @Builder.Default
    private Set<String> access = new LinkedHashSet<>();
    @Builder.Default
    private Set<UUID> references = new LinkedHashSet<>();

    private String type;

    private SubstanceReferenceDTO substance;
}
