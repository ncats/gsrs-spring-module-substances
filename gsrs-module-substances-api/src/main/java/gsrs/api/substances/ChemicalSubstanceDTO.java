package gsrs.api.substances;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ChemicalSubstanceDTO extends SubstanceDTO{

    private StructureDTO structure;

    @Setter(AccessLevel.NONE)
    private LazyFetchedCollection _moieties;

    public ChemicalSubstanceDTO(){
        setSubstanceClass(SubstanceClass.chemical);
    }
}
