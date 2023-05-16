package example.polymer;

import gov.nih.ncats.common.Tuple;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.PolymerSubstanceBuilder;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.v1.Material;
import ix.ginas.models.v1.PolymerSubstance;
import ix.ginas.models.v1.SubstanceReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PolymerSubstanceTest {

    @Test
    public void testFindNondefiningMonomer(){
        ChemicalSubstanceBuilder monomerBuilder = new ChemicalSubstanceBuilder();
        monomerBuilder.setStructureWithDefaultReference("c1ccccc1C=C");
        monomerBuilder.addName("styrene");

        PolymerSubstanceBuilder builder = new PolymerSubstanceBuilder();
        builder.addName("polystyrene");
        PolymerSubstance polystyrene= builder.build();
        Material monomer = new Material();
        monomer.monomerSubstance= new SubstanceReference();
        monomer.monomerSubstance.wrappedSubstance=monomerBuilder.build();
        monomer.defining=false;
        polystyrene.polymer.monomers.add(monomer);

        List<Tuple<GinasAccessControlled,SubstanceReference>> refs= polystyrene.getSubstanceReferencesAndParentsBeyondDependsOn();
        Assertions.assertEquals(1, refs.size());
        Assertions.assertEquals(monomer.monomerSubstance, refs.get(0).v());
    }

    @Test
    public void testFindNullDefiningMonomer(){
        ChemicalSubstanceBuilder monomerBuilder = new ChemicalSubstanceBuilder();
        monomerBuilder.setStructureWithDefaultReference("c1ccccc1C=C");
        monomerBuilder.addName("styrene");

        PolymerSubstanceBuilder builder = new PolymerSubstanceBuilder();
        builder.addName("polystyrene");
        PolymerSubstance polystyrene= builder.build();
        Material monomer = new Material();
        monomer.monomerSubstance= new SubstanceReference();
        monomer.monomerSubstance.wrappedSubstance=monomerBuilder.build();
        monomer.defining=null;
        polystyrene.polymer.monomers.add(monomer);

        List<Tuple<GinasAccessControlled,SubstanceReference>> refs= polystyrene.getSubstanceReferencesAndParentsBeyondDependsOn();
        Assertions.assertEquals(1, refs.size());
        Assertions.assertEquals(monomer.monomerSubstance, refs.get(0).v());
    }

}
