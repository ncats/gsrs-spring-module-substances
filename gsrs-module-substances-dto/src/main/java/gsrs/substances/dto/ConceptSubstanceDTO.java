package gsrs.substances.dto;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ConceptSubstanceDTO extends SubstanceDTO{

    public ConceptSubstanceDTO(){
        setSubstanceClass(SubstanceClass.concept);
    }
}
