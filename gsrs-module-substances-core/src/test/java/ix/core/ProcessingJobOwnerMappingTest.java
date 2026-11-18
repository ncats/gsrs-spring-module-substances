package ix.core;

import ix.core.models.ProcessingJob;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingJobOwnerMappingTest {
    @Test
    void multipleImportJobsShouldBeAllowedToShareAnExistingOwner() throws Exception {
        Field owner = ProcessingJob.class.getField("owner");

        assertNull(owner.getAnnotation(OneToOne.class));
        ManyToOne relationship = owner.getAnnotation(ManyToOne.class);
        assertNotNull(relationship, "one user must be allowed to own multiple import jobs");
        assertFalse(Arrays.asList(relationship.cascade()).contains(CascadeType.ALL));
        assertFalse(Arrays.asList(relationship.cascade()).contains(CascadeType.REMOVE),
                "deleting an import job must not delete its shared user");
        assertEquals(0, relationship.cascade().length,
                "job persistence must not cascade changes to the existing audited user");
    }

    @Test
    void multipleImportJobsShouldBeAllowedToShareAnExistingPayload() throws Exception {
        Field payload = ProcessingJob.class.getField("payload");

        assertNull(payload.getAnnotation(OneToOne.class));
        ManyToOne relationship = payload.getAnnotation(ManyToOne.class);
        assertNotNull(relationship, "re-importing the same file must be allowed");
        assertEquals(0, relationship.cascade().length,
                "deleting an import job must not delete a shared payload");
    }
}
