package gsrs.substances.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper=false)
public class ChemicalSubstanceDTO extends SubstanceDTO{

    private StructureDTO structure;

    @Setter(AccessLevel.NONE)
    private LazyFetchedCollection _moieties;

    public ChemicalSubstanceDTO(){
        setSubstanceClass(SubstanceClass.chemical);
    }
}
