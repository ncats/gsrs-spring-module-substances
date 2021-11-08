package gsrs.substances.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class MixtureSubstanceDTO extends SubstanceDTO{

    private MixtureDTO mixture;

    public MixtureSubstanceDTO(){
        setSubstanceClass(SubstanceClass.mixture);
    }
}
