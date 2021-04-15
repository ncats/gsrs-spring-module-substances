package example;

import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;

public final class SubstanceTestUtil {

    private SubstanceTestUtil(){}

    public static ChemicalSubstance makeChemicalSubstance(String smiles){
        return new SubstanceBuilder()
                .asChemical()
                .generateNewUUID()
                .addName(smiles + " name")
                .setStructure(smiles)
                .build();
    }

}
