package gsrs.substances.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NucleicAcidSubstanceDTO extends SubstanceDTO{


    private NucleicAcidDTO nucleicAcid;

    public NucleicAcidSubstanceDTO(){
        setSubstanceClass(SubstanceClass.nucleicAcid);
    }
}
