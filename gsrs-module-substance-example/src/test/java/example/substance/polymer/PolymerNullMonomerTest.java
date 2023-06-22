package example.substance.polymer;

import gov.nih.ncats.common.Tuple;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.PolymerSubstanceBuilder;
import ix.ginas.models.GinasAccessControlled;
import ix.ginas.models.v1.*;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public class PolymerNullMonomerTest {

    @Test
    public void testNullMonomer() {
        Polymer polymer = new Polymer();
        polymer.monomers.add(null);
        polymer.displayStructure= new GinasChemicalStructure();
        polymer.displayStructure.molfile ="\n" +
                "  2D\n" +
                "\n" +
                " 10 10  0  0  0  0  0  0  0  0999 V2000\n" +
                "    2.3539   -7.0992    0.0000 *   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.3768   -6.5087    0.0000 C   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "    3.3767   -5.3276    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.3996   -4.7370    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.3996   -3.5559    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.3767   -2.9653    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    2.3539   -3.5559    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    2.3539   -4.7370    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.3996   -7.0992    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.4225   -6.5086    0.0000 *   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  1  0  0  0  0\n" +
                "  3  4  1  0  0  0  0\n" +
                "  4  5  2  0  0  0  0\n" +
                "  5  6  1  0  0  0  0\n" +
                "  6  7  2  0  0  0  0\n" +
                "  7  8  1  0  0  0  0\n" +
                "  8  3  2  0  0  0  0\n" +
                "  2  9  1  0  0  0  0\n" +
                "  9 10  1  0  0  0  0\n" +
                "M  STY  1   1 SRU\n" +
                "M  SLB  1   1   1\n" +
                "M  SCN  1   1 HT \n" +
                "M  SAL   1  8   9   2   3   8   7   6   5   4\n" +
                "M  SBL   1  2   1  10\n" +
                "M  SDI   1  4    2.8653   -7.3945    2.8653   -6.2134\n" +
                "M  SDI   1  4    4.9111   -6.2134    4.9111   -7.3945\n" +
                "M  SMT   1 n\n" +
                "M  END\n";
        PolymerSubstanceBuilder builder = new PolymerSubstanceBuilder();
        builder.addName("Polymer with null monomer");
        builder.setPolymer(polymer);
        Reference basicRef = new Reference();
        basicRef.citation="Some Reference";
        basicRef.docType="Some Document";
        builder.addReference(basicRef);

        PolymerSubstance polymerSubstance = builder.build();
        List<Tuple<GinasAccessControlled, SubstanceReference>> refs=null;
        try {
            refs = polymerSubstance.getDependsOnSubstanceReferencesAndParents();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        Assertions.assertNotNull(refs);

    }

    @Test
    public void testNullDefiningMonomer() {
        Polymer polymer = new Polymer();

        ChemicalSubstance monomer = (new ChemicalSubstanceBuilder())
                .setStructureWithDefaultReference("C/C=C(/C)\\c1ccccc1")
                .addName("styrene")
                .generateNewUUID()
                .build();

        Material material = new Material();
        material.monomerSubstance = new SubstanceReference();
        material.monomerSubstance.wrappedSubstance= monomer;
        material.defining=null;
        polymer.monomers.add (material);
        polymer.monomers.get(0).defining=null;//make certain it's null
        polymer.displayStructure= new GinasChemicalStructure();
        polymer.displayStructure.molfile ="\n" +
                "  2D\n" +
                "\n" +
                " 10 10  0  0  0  0  0  0  0  0999 V2000\n" +
                "    2.3539   -7.0992    0.0000 *   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.3768   -6.5087    0.0000 C   0  0  3  0  0  0  0  0  0  0  0  0\n" +
                "    3.3767   -5.3276    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.3996   -4.7370    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.3996   -3.5559    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    3.3767   -2.9653    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    2.3539   -3.5559    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    2.3539   -4.7370    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    4.3996   -7.0992    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    5.4225   -6.5086    0.0000 *   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  1  0  0  0  0\n" +
                "  3  4  1  0  0  0  0\n" +
                "  4  5  2  0  0  0  0\n" +
                "  5  6  1  0  0  0  0\n" +
                "  6  7  2  0  0  0  0\n" +
                "  7  8  1  0  0  0  0\n" +
                "  8  3  2  0  0  0  0\n" +
                "  2  9  1  0  0  0  0\n" +
                "  9 10  1  0  0  0  0\n" +
                "M  STY  1   1 SRU\n" +
                "M  SLB  1   1   1\n" +
                "M  SCN  1   1 HT \n" +
                "M  SAL   1  8   9   2   3   8   7   6   5   4\n" +
                "M  SBL   1  2   1  10\n" +
                "M  SDI   1  4    2.8653   -7.3945    2.8653   -6.2134\n" +
                "M  SDI   1  4    4.9111   -6.2134    4.9111   -7.3945\n" +
                "M  SMT   1 n\n" +
                "M  END\n";
        PolymerSubstanceBuilder builder = new PolymerSubstanceBuilder();
        builder.addName("Polymer with null monomer");
        builder.setPolymer(polymer);
        Reference basicRef = new Reference();
        basicRef.citation="Some Reference";
        basicRef.docType="Some Document";
        builder.addReference(basicRef);

        PolymerSubstance polymerSubstance = builder.build();
        List<Tuple<GinasAccessControlled, SubstanceReference>> refs=null;
        try {
            refs = polymerSubstance.getDependsOnSubstanceReferencesAndParents();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        Assertions.assertNotNull(refs);

    }
}
