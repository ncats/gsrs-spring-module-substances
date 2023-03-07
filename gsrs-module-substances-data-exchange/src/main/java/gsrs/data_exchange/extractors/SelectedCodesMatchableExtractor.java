package gsrs.dataexchange.extractors;

import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import ix.ginas.models.v1.Substance;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SelectedCodesMatchableExtractor implements MatchableKeyValueTupleExtractor<Substance> {
    private final String KEY="CODE";
    public final int SELECTED_CODE_LAYER=0;

    private final List<String> SelectedCodeSystems = Arrays.asList("CAS", "ChemBL", "NCI"); //todo: pull this from config

    @Override
    public void extract(Substance substance, Consumer<MatchableKeyValueTuple> c) {
        substance.codes.stream()
                .filter(code->SelectedCodeSystems.contains( code.codeSystem))
                .forEach(code->{
                    MatchableKeyValueTuple tuple =
                            MatchableKeyValueTuple.builder()
                                    .key(KEY)
                                    .value(code.code)
                                    .qualifier(String.format("Code system: %s; type: %s", code.codeSystem, code.type))
                                    .layer(SELECTED_CODE_LAYER)
                                    .build();
                    c.accept(tuple);
                });
    }
}
