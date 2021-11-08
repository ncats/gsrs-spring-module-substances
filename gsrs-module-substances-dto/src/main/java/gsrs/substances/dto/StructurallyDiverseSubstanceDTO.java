package gsrs.substances.dto;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class StructurallyDiverseSubstanceDTO extends SubstanceDTO{

    private StructurallyDiverseDTO structurallyDiverse;

    public StructurallyDiverseSubstanceDTO(){
        setSubstanceClass(SubstanceClass.structurallyDiverse);
    }
}
