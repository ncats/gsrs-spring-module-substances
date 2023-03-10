package ix.core.processing;

import ix.core.stats.Estimate;

import java.io.InputStream;

public interface RecordExtractorFactory<K> {
    /**
     * Get the Name for this Extractor.
     * By default, the name is the RecordExtractor instance's class name.
     * @return the name as a String can not be null.
     *
     */
    String getExtractorName();

    RecordExtractor<K> createNewExtractorFor(InputStream in);

    Estimate estimateRecordCount(InputStream in);
}
