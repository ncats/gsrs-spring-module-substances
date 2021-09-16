package gsrs.api.substances;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class StructurallyDiverseSubstanceDTO extends SubstanceDTO{

    private StructurallyDiverseDTO structurallyDiverse;

    public StructurallyDiverseSubstanceDTO(){
        setSubstanceClass(SubstanceClass.structurallyDiverse);
    }
}
