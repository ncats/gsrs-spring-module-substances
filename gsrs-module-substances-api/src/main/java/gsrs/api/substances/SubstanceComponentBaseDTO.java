package gsrs.api.substances;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class SubstanceComponentBaseDTO extends BaseEditableDTO{



    private Set<String> access = new LinkedHashSet<>();
    private Set<UUID> references = new LinkedHashSet<>();
}
