package gsrs.substances.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
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
