package gsrs.substances.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper=false)
public class MixtureSubstanceDTO extends SubstanceDTO{

    private MixtureDTO mixture;

    public MixtureSubstanceDTO(){
        setSubstanceClass(SubstanceClass.mixture);
    }
}
