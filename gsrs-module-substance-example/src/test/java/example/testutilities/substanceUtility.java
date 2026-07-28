package example.testutilities;

import gsrs.module.substance.services.IupacNameService;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;

import java.util.UUID;

public class substanceUtility {
    public static Substance createSubstanceWithPubchemReference() {
        Substance baseSubstance = createSubstanceWithoutPubchemReference();
        Reference ref3 = new Reference();
        ref3.docType = IupacNameService.PUBCHEM_REFERENCE_TYPE;
        ref3.citation = "abcd";
        ref3.uuid = UUID.randomUUID();
        baseSubstance.addReference(ref3);
        return baseSubstance;
    }

    public static Substance createSubstanceWithoutPubchemReference() {
        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder();
        builder.addName("dibenzylmethane");
        builder.setStructureWithDefaultReference("c1ccccc1CCCc2ccccc2");
        Reference ref1 = new Reference();
        ref1.docType = "CHEBI";
        ref1.citation = "CHEBI:27732";
        ref1.uuid = UUID.randomUUID();
        builder.addReference(ref1);

        Reference ref2 = new Reference();
        ref2.docType = "Data Source";
        ref2.citation = "23827b";
        ref2.uuid = UUID.randomUUID();
        builder.addReference(ref2);
        return builder.build();
    }
}
