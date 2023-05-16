package ix.ginas.modelBuilders;

import ix.ginas.models.v1.SpecifiedSubstanceGroup1;
import ix.ginas.models.v1.SpecifiedSubstanceGroup1Substance;
import ix.ginas.models.v1.StructurallyDiverseSubstance;
import ix.ginas.models.v1.Substance;

import java.util.function.Supplier;

/**
 * Created by peryeat on 8/31/18.
 */
public class SpecifiedSubstanceGroup1SubstanceBuilder extends AbstractSubstanceBuilder<SpecifiedSubstanceGroup1Substance, SpecifiedSubstanceGroup1SubstanceBuilder> {

    public SpecifiedSubstanceGroup1SubstanceBuilder(){

    }

    public SpecifiedSubstanceGroup1SubstanceBuilder(Substance copy) {
        super(copy);
        if(copy instanceof SpecifiedSubstanceGroup1Substance){
        	SpecifiedSubstanceGroup1 ss = ((SpecifiedSubstanceGroup1Substance)copy).specifiedSubstance;
            if(ss !=null){
            	setSpecifiedSubstance(ss);
            }
        }
    }

    protected <S extends Substance> SpecifiedSubstanceGroup1SubstanceBuilder(AbstractSubstanceBuilder<S,?> builder){
        this.andThen = (s)-> (SpecifiedSubstanceGroup1Substance) builder.andThen.apply((S) s);
    }
    @Override
    public Supplier<SpecifiedSubstanceGroup1Substance> getSupplier() {
        return () ->{
        	SpecifiedSubstanceGroup1Substance s = new SpecifiedSubstanceGroup1Substance();
            s.specifiedSubstance = new SpecifiedSubstanceGroup1();
            return s;
        };
    }

    @Override
    protected Substance.SubstanceClass getSubstanceClass() {
        return Substance.SubstanceClass.specifiedSubstanceG1;
    }

    @Override
    protected SpecifiedSubstanceGroup1SubstanceBuilder getThis() {
        return this;
    }

    public SpecifiedSubstanceGroup1SubstanceBuilder setSpecifiedSubstance(SpecifiedSubstanceGroup1 ss){
        andThen( s ->{
            s.specifiedSubstance=ss;
        });
        return getThis();
    }

}
