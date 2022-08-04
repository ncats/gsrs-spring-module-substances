package gsrs.dataExchange.extractors;

import gsrs.holdingarea.model.MatchableKeyValueTupleExtractor;

public interface MatchableExtractorFactory {
    <T> MatchableKeyValueTupleExtractor<T> createExtractorFor(Class <T> cls);
}
