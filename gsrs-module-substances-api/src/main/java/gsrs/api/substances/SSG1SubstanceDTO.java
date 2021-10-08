package gsrs.api.substances;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSG1SubstanceDTO extends SubstanceDTO{


    private SpecifiedSubstanceConstituents constituents;

    public SSG1SubstanceDTO(){
        setSubstanceClass(SubstanceClass.specifiedSubstanceG1);
    }
}
