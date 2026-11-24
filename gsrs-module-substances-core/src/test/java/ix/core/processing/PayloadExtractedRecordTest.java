package ix.core.processing;

import ix.core.models.ProcessingJob;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PayloadExtractedRecordTest {

    @Test
    void explicitJobKeyShouldNotAccessLazyProcessingJobKeys() {
        ProcessingJob detachedJob = mock(ProcessingJob.class);
        Object record = new Object();

        PayloadExtractedRecord<Object> extracted =
                new PayloadExtractedRecord<>(detachedJob, record, "job-key");

        assertSame(detachedJob, extracted.job);
        assertSame(record, extracted.theRecord);
        assertEquals("job-key", extracted.jobKey);
        verifyNoInteractions(detachedJob);
    }
}
