package gsrs.dataexchange.extractors;

import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class CodeMatchableExtractor implements MatchableKeyValueTupleExtractor<Substance> {
    //todo: move the code systems to config
    private final List<String> requiredCodeSystems;
    private final String REQUIRED_CODE_TYPE;
    private String CODE_KEY;
    public final int CODE_LAYER=1;

    public CodeMatchableExtractor(List<String> reqCodeSystems, String codeType,  String codeKey) {
        requiredCodeSystems= reqCodeSystems;
        REQUIRED_CODE_TYPE= codeType;
        CODE_KEY=codeKey;
    }
    public CodeMatchableExtractor(String reqCodeSystem) {
        this(Collections.singletonList(reqCodeSystem), "PRIMARY", reqCodeSystem);
    }

    @Override
    public void extract(Substance substance, Consumer<MatchableKeyValueTuple> c) {
        substance.codes.forEach(code->{
            if( requiredCodeSystems.contains(code.codeSystem) && code.type.equalsIgnoreCase(REQUIRED_CODE_TYPE)) {

                log.trace("creating matchable with key: {}; value: {}; qual: {}", CODE_KEY, code.code,  code.codeSystem);
                MatchableKeyValueTuple tuple =
                        MatchableKeyValueTuple.builder()
                                .key(CODE_KEY)
                                .value(code.code)
                                .layer(CODE_LAYER)
                                .qualifier(code.codeSystem)
                                .build();
                c.accept(tuple);
            }
        });
    }


}
