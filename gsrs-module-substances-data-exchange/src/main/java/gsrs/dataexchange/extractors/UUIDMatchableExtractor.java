package gsrs.dataexchange.extractors;

import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class UUIDMatchableExtractor implements MatchableKeyValueTupleExtractor<Substance> {

    private final String KEY="UUID";

    @Override
    public void extract(Substance substance, Consumer<MatchableKeyValueTuple> c) {
        if(substance.getUuid() == null) {
            log.info("Substance had no UUID. Skipping UUID matchable");
            return;
        }
        MatchableKeyValueTuple tuple =
                MatchableKeyValueTuple.builder()
                        .key(KEY)
                        .value(substance. getUuid().toString())
                        .build();
        c.accept(tuple);
    }
}
