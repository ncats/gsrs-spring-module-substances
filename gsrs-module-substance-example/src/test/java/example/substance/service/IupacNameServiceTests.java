package example.substance.service;

import gsrs.module.substance.services.IupacNameService;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class IupacNameServiceTests {

    private IupacNameService service = new IupacNameService();

    @Test
    void testFindOrCreatePubchemReference() {
        Substance substance = createSubstanceWithoutPubchemReference();
        List<UUID> existingReferenceUuids = substance.references.stream()
                .map( r->r.uuid)
                .collect(Collectors.toList());
        Reference newRef = service.findOrCreatePubchemReference(substance);
        Assertions.assertFalse(existingReferenceUuids.contains(newRef.uuid));
    }

    @Test
    void testFindOrCreatePubchemReference2() {
        Substance substance = createSubstanceWithPubchemReference();
        List<UUID> existingReferenceUuids = substance.references.stream()
                .map( r->r.uuid)
                .collect(Collectors.toList());
        Reference newRef = service.findOrCreatePubchemReference(substance);
        Assertions.assertTrue(existingReferenceUuids.contains(newRef.uuid));
    }

    private Substance createSubstanceWithPubchemReference() {
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
