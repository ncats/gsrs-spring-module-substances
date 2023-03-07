package gsrs.dataexchange.extractors;

import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;

public interface MatchableExtractorFactory {
    <T> MatchableKeyValueTupleExtractor<T> createExtractorFor(Class <T> cls);
}
