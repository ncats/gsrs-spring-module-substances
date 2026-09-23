package gsrs.substances.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSG1SubstanceDTO extends SubstanceDTO{


    private SpecifiedSubstanceConstituents constituents;

    public SSG1SubstanceDTO(){
        setSubstanceClass(SubstanceClass.specifiedSubstanceG1);
    }
}
