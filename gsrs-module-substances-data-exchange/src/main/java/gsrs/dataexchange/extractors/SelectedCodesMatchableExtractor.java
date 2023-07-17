package gsrs.dataexchange.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class SelectedCodesMatchableExtractor implements MatchableKeyValueTupleExtractor<Substance> {
    private final String KEY="CODE";
    public final int SELECTED_CODE_LAYER=0;

    public SelectedCodesMatchableExtractor(JsonNode config){
        log.trace("in JsonNode constructor");
        this.selectedCodeSystems.clear();
        config.get("codeSystems").forEach(cs->{
            log.trace("adding code system {}", cs.asText());
            this.selectedCodeSystems.add(cs.asText());
        });
    }

    private List<String> selectedCodeSystems = new ArrayList<>(Arrays.asList("CAS", "ChemBL", "NCI"));

    @Override
    public void extract(Substance substance, Consumer<MatchableKeyValueTuple> c) {
        substance.codes.stream()
                .filter(code-> selectedCodeSystems.contains( code.codeSystem))
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
