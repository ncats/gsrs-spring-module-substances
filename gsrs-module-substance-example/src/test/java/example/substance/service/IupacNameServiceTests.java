package example.substance.service;

import gsrs.module.substance.services.IupacNameService;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static example.testutilities.substanceUtility.createSubstanceWithPubchemReference;
import static example.testutilities.substanceUtility.createSubstanceWithoutPubchemReference;

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
}
