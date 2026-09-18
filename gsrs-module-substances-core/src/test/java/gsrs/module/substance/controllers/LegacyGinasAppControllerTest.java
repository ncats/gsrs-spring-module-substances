package gsrs.module.substance.controllers;

import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Relationship;
import org.junit.jupiter.api.Test;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyGinasAppControllerTest {

    @Test
    void makeFastaFromPrimaryProteinWithoutApprovalIdDoesNotReadRelationships() {
        UUID substanceUuid = UUID.fromString("8116157b-1998-487a-9346-5c97be8626b2");
        ProteinSubstance protein = new SubstanceBuilder()
                .asProtein()
                .setUUID(substanceUuid)
                .addName("Tripartite motif-containing protein 38")
                .addSubunitWithDefaultReference("ACDEFGHIK")
                .build();
        protein.approvalID = null;
        protein.relationships = new ThrowOnReadRelationshipList();

        String fasta = LegacyGinasAppController.makeFastaFromProtein(protein);

        assertEquals(">" + substanceUuid + "|SUBUNIT_1\nACDEFGHIK\n", fasta);
    }

    private static final class ThrowOnReadRelationshipList extends AbstractList<Relationship> {
        @Override
        public Relationship get(int index) {
            throw new AssertionError("FASTA export should not read relationships for a primary protein without an approval ID");
        }

        @Override
        public Iterator<Relationship> iterator() {
            throw new AssertionError("FASTA export should not iterate relationships for a primary protein without an approval ID");
        }

        @Override
        public int size() {
            return 1;
        }
    }
}
