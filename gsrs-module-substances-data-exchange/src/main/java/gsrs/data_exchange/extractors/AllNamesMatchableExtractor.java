package gsrs.dataexchange.extractors;

import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import ix.ginas.models.v1.Substance;

import java.util.function.Consumer;

public class AllNamesMatchableExtractor<T> implements MatchableKeyValueTupleExtractor<T> {

    public final String NAME_KEY= "Substance Name";
    public final String PRIMARY_NAME_KEY ="Primary Name";
    public final int PRIMARY_NAME_LAYER =0;
    public final int NAME_LAYER =1;

    @Override
    public void extract(T substanceObject, Consumer<MatchableKeyValueTuple> c) {
        String[] primaryName = new String[1];

        Substance substance = (Substance) substanceObject;
        substance.names.forEach(n -> {
            MatchableKeyValueTuple tuple =
                    MatchableKeyValueTuple.builder()
                            .key(NAME_KEY)
                            .value(n.name)
                            .qualifier("name type: " + n.type)
                            .layer(NAME_LAYER)
                            .build();
            c.accept(tuple);
            if( n.displayName) {
                primaryName[0]=n.name;
            }
        });
        MatchableKeyValueTuple tuple =
                MatchableKeyValueTuple.builder()
                        .key(PRIMARY_NAME_KEY)
                        .value(primaryName[0])
                        .qualifier("Display Name")
                        .layer(PRIMARY_NAME_LAYER)
                        .build();
        c.accept(tuple);
    }
}
