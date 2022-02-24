package ix.core.processing;

import ix.core.models.ProcessingJob;

public interface RecordPersisterFactory<K,T> {

    String getPersisterName();

    RecordPersister<K,T> createPersisterFor(ProcessingJob job);
}
