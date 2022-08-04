package gsrs.dataexchange.extractors;

import gsrs.holdingarea.model.MatchableKeyValueTuple;
import gsrs.holdingarea.model.MatchableKeyValueTupleExtractor;
import ix.ginas.models.v1.Substance;

import java.util.function.Consumer;

public class UUIDMatchableExtractor implements MatchableKeyValueTupleExtractor<Substance> {

    private final String KEY="UUID";

    @Override
    public void extract(Substance substance, Consumer<MatchableKeyValueTuple> c) {
        MatchableKeyValueTuple tuple =
                MatchableKeyValueTuple.builder()
                        .key(KEY)
                        .value(substance.getUuid().toString())
                        .build();
        c.accept(tuple);
    }
}
