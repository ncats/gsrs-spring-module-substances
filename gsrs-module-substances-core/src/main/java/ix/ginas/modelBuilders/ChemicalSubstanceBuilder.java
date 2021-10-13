package ix.ginas.modelBuilders;

import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.molwitch.Chemical;
import ix.ginas.models.v1.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

public class ChemicalSubstanceBuilder extends AbstractSubstanceBuilder<ChemicalSubstance, ChemicalSubstanceBuilder> {

    @Override
    protected Substance.SubstanceClass getSubstanceClass() {
        return Substance.SubstanceClass.chemical;
    }

    @Override
	protected ChemicalSubstanceBuilder getThis() {
		return this;
	}
	
	@Override
	public Supplier<ChemicalSubstance> getSupplier(){
		return ChemicalSubstance::new;
	}

    protected <S extends Substance> ChemicalSubstanceBuilder(AbstractSubstanceBuilder<S,?> builder){
        this.andThen = (s)-> (ChemicalSubstance) builder.andThen.apply((S) s);
    }
	public ChemicalSubstanceBuilder setStructureWithDefaultReference(String smiles){
		return andThen(cs->{
			cs.setStructure(new GinasChemicalStructure());
            try {
                Chemical chem = Chemical.parse(smiles);
                chem.generateCoordinates();
                cs.getStructure().molfile= chem.toMol();
                cs.getStructure().smiles = smiles;
            } catch (Exception e) {
                Sneak.sneakyThrow(e);
            }
//            =smiles;//not really right, but we know it works
            Reference orAddFirstReference = getOrAddFirstReference(cs);
            cs.getStructure().addReference( orAddFirstReference,cs);
		});
	}

    public ChemicalSubstanceBuilder() {
    }

    public ChemicalSubstanceBuilder(Substance copy) {
        super(copy);

        ChemicalSubstance cs = (ChemicalSubstance)copy;
        setStructure(cs.getStructure());
        for(Moiety m : cs.moieties){
            addMoiety(m);
        }
    }
    public ChemicalSubstanceBuilder addMoiety(Moiety m){
        return andThen( s-> { s.moieties.add(m);});
    }
    public ChemicalSubstanceBuilder setStructure(GinasChemicalStructure structure){
        return andThen(s-> { s.setStructure(structure);});
    }
}
