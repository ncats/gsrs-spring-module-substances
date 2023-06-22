package gsrs.dataexchange.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class CodeMatchableExtractor implements MatchableKeyValueTupleExtractor<Substance> {
    //todo: move the code systems to config
    private List<String> requiredCodeSystems= new ArrayList<>();
    private String REQUIRED_CODE_TYPE;
    private String CODE_KEY;
    public final int CODE_LAYER=1;

    public CodeMatchableExtractor(JsonNode config){
        log.trace("in constructor with JsonNode");
        ((ArrayNode) config.get("reqCodeSystems")).forEach(n->{
            this.requiredCodeSystems.add(n.asText());
        });
        this.REQUIRED_CODE_TYPE=config.get("codeType").asText();
        this.CODE_KEY=config.get("codeKey").asText();
    }
    public CodeMatchableExtractor(List<String> reqCodeSystems, String codeType,  String codeKey) {
        requiredCodeSystems.clear();
        requiredCodeSystems.addAll( reqCodeSystems);
        REQUIRED_CODE_TYPE= codeType;
        CODE_KEY=codeKey;
    }
    public CodeMatchableExtractor(String reqCodeSystem) {
        this(Collections.singletonList(reqCodeSystem), "PRIMARY", reqCodeSystem);
    }

    public void setRequiredCodeSystems(List<String> reqCodeSystems){
        this.requiredCodeSystems= reqCodeSystems;
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
