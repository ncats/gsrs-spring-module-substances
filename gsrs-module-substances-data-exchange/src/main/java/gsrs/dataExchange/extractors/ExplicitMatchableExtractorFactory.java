package gsrs.dataexchange.extractors;


import gsrs.holdingarea.model.MatchableKeyValueTupleExtractor;

public class ExplicitMatchableExtractorFactory implements MatchableExtractorFactory {
    @Override
    public <T> MatchableKeyValueTupleExtractor<T> createExtractorFor(Class<T> cls) {
        MatchableKeyValueTupleExtractor<T> returnExtractor = (t,c)->{  };
        returnExtractor=returnExtractor.combine((MatchableKeyValueTupleExtractor<T>) new CASNumberMatchableExtractor());
        returnExtractor=returnExtractor.combine((MatchableKeyValueTupleExtractor<T>) new AllNamesMatchableExtractor<>());
        returnExtractor=returnExtractor.combine((MatchableKeyValueTupleExtractor<T>) new ApprovalIdMatchableExtractor());
        returnExtractor=returnExtractor.combine((MatchableKeyValueTupleExtractor<T>) new DefinitionalHashMatchableExtractor());
        returnExtractor=returnExtractor.combine((MatchableKeyValueTupleExtractor<T>) new SelectedCodesMatchableExtractor());
        returnExtractor=returnExtractor.combine((MatchableKeyValueTupleExtractor<T>) new UUIDMatchableExtractor());
        returnExtractor=returnExtractor.combine((MatchableKeyValueTupleExtractor<T>) new CodeMatchableExtractor("FDA UNII"));
        return returnExtractor;
    }
}
