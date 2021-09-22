package gsrs.api.substances;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class ChemicalSubstanceDTO extends SubstanceDTO{

    private StructureDTO structure;

    @Setter(AccessLevel.NONE)
    private LazyFetchedCollection _moieties;

    public ChemicalSubstanceDTO(){
        setSubstanceClass(SubstanceClass.chemical);
    }
}
