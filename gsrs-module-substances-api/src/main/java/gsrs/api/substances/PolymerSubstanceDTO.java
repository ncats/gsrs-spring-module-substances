package gsrs.api.substances;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolymerSubstanceDTO extends SubstanceDTO{

    private PolymerDTO polymer;

    public PolymerSubstanceDTO(){
        setSubstanceClass(SubstanceClass.polymer);
    }
}
