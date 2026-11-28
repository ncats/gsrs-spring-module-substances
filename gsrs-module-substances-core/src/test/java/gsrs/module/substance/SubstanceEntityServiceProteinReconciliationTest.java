package gsrs.module.substance;

import ix.ginas.models.v1.Protein;
import ix.ginas.models.v1.ProteinSubstance;
import ix.ginas.models.v1.Subunit;
import ix.ginas.models.v1.Glycosylation;
import ix.ginas.models.v1.OtherLinks;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.persistence.EntityManager;
import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubstanceEntityServiceProteinReconciliationTest {
    abstract static class ProteinWithoutApiLinks {
        @JsonIgnore
        public abstract Object getJsonGlycosylation();
    }

    @Test
    void preservesManagedDefinitionChildrenWhileApplyingProteinMetadata() {
        Protein managed = new Protein();
        managed.uuid = UUID.randomUUID();
        managed.sequenceOrigin = "NATURAL";
        Glycosylation glycosylation = new Glycosylation();
        glycosylation.uuid = UUID.randomUUID();
        glycosylation.glycosylationType = "OLD";
        managed.glycosylation = glycosylation;
        OtherLinks link = new OtherLinks();
        link.uuid = UUID.randomUUID();
        link.linkageType = "OLD";
        managed.otherLinks.add(link);
        Protein updated = new Protein();
        updated.uuid = managed.uuid;
        updated.sequenceOrigin = "SYNTHETIC";
        updated.glycosylation = new Glycosylation();
        updated.glycosylation.uuid = glycosylation.uuid;
        updated.glycosylation.glycosylationType = "NEW";
        OtherLinks updatedLink = new OtherLinks();
        updatedLink.uuid = link.uuid;
        updatedLink.linkageType = "NEW";
        updated.otherLinks.add(updatedLink);
        ProteinSubstance managedOwner = new ProteinSubstance();
        managedOwner.uuid = UUID.randomUUID();
        managedOwner.setProtein(managed);
        ProteinSubstance updatedOwner = new ProteinSubstance();
        updatedOwner.uuid = managedOwner.uuid;
        updatedOwner.setProtein(updated);

        SubstanceEntityServiceImpl service = new SubstanceEntityServiceImpl();
        // Derived API links require a running application; persisted glycosylation
        // data remains included in this focused reconciliation test.
        ReflectionTestUtils.setField(service, "mapper", JsonMapper.builderWithJackson2Defaults()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addMixIn(Protein.class, ProteinWithoutApiLinks.class).build());
        ReflectionTestUtils.invokeMethod(service, "reconcileManagedProtein", updated, managed);

        assertEquals("SYNTHETIC", managed.sequenceOrigin);
        assertSame(glycosylation, managed.glycosylation);
        assertEquals("NEW", glycosylation.glycosylationType);
        assertSame(link, managed.otherLinks.get(0));
        assertEquals("NEW", link.linkageType);
        assertTrue(managed.isAllDirty());
        assertTrue(glycosylation.isAllDirty());
        assertTrue(link.isAllDirty());
    }

    @Test
    void removesDeletedOwnedSubunitSoItsSequenceCanLeaveTheIndex() {
        EntityManager em = mock(EntityManager.class);
        SubstanceEntityServiceImpl service = new SubstanceEntityServiceImpl() {
            @Override
            public EntityManager getEntityManager() { return em; }
        };
        Protein managed = new Protein();
        Subunit removed = new Subunit();
        removed.uuid = UUID.randomUUID();
        removed.sequence = "ACDEFGHIK";
        managed.subunits.add(removed);

        ReflectionTestUtils.invokeMethod(service, "reconcileManagedProteinSubunits", new Protein(), managed);

        assertTrue(managed.subunits.isEmpty());
        verify(em).remove(removed);
    }

    @Test
    void appliesSequenceToExistingSubunitWithoutReplacingItsIdentity() {
        Protein managed = new Protein();
        Subunit existing = new Subunit();
        existing.uuid = UUID.randomUUID();
        existing.sequence = "ACDEFGHIK";
        existing.created = new java.util.Date(1000L);
        managed.subunits.add(existing);
        Protein updated = new Protein();
        Subunit replacement = new Subunit();
        replacement.uuid = existing.uuid;
        replacement.sequence = "TTTTTTTTTTATCGHHHH";
        updated.subunits.add(replacement);

        ReflectionTestUtils.invokeMethod(new SubstanceEntityServiceImpl(),
                "reconcileManagedProteinSubunits", updated, managed);

        assertSame(existing, managed.subunits.get(0));
        assertEquals("TTTTTTTTTTATCGHHHH", existing.sequence);
        assertSame(managed, existing.getParent());
        assertEquals(new java.util.Date(1000L), existing.created, "The update must preserve creation audit data");
        assertTrue(existing.isAllDirty(), "GSRS must schedule the subunit's database update");
    }
}
