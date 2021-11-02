package gsrs.substances.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProteinSubstanceDTO extends SubstanceDTO{


    private ProteinDTO protein;

    public ProteinSubstanceDTO(){
        setSubstanceClass(SubstanceClass.protein);
    }
}
