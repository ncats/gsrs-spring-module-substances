package ix.core.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PubChemResolutionResult {
    private String iupacName;
    private String cid;
    private String lookupValue;
}
