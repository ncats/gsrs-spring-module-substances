package gsrs.substances.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper=false)
public class StructurallyDiverseSubstanceDTO extends SubstanceDTO{

    private StructurallyDiverseDTO structurallyDiverse;

    public StructurallyDiverseSubstanceDTO(){
        setSubstanceClass(SubstanceClass.structurallyDiverse);
    }
}
