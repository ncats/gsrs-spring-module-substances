package gsrs.api.substances;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class MixtureSubstanceDTO extends SubstanceDTO{

    private MixtureDTO mixture;

    public MixtureSubstanceDTO(){
        setSubstanceClass(SubstanceClass.mixture);
    }
}
