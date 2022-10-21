package gsrs.module.substance.datasource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


import gsrs.module.substance.processors.CodeSystemUrlGenerator;
import ix.ginas.models.v1.Code;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultCodeSystemUrlGenerator implements DataSet<CodeSystemMeta>, CodeSystemUrlGenerator {

    @JsonIgnore
    private final Map<String, CodeSystemMeta> map = new LinkedHashMap<>();

    @JsonCreator
    public DefaultCodeSystemUrlGenerator(@JsonProperty("codeSystems") Map<String, Map<String, String>> tree) throws IOException {
        for (Map<String, String> entry : tree.values()) {
            CodeSystemMeta csmap = new CodeSystemMeta(entry.get("codeSystem"), entry.get("url"));
            map.put(csmap.codeSystem.toLowerCase(), csmap);
        }
    }

    @Override
    public Iterator<CodeSystemMeta> iterator() {
        return map.values().iterator();
    }

    @Override
    public boolean contains(CodeSystemMeta k) {
        return map.containsKey(k.codeSystem.toLowerCase());
    }

    public CodeSystemMeta fetch(String codesystem) {
        return this.map.get(codesystem.toLowerCase());
    }

    @Override
    public Optional<String> generateUrlFor(Code code) {
        log.trace("DefaultCodeSystemUrlGenerator generateUrlFor");
        if (code.code == null) {
            return Optional.empty();
        }
        CodeSystemMeta meta = fetch(code.codeSystem);
        if (meta == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(meta.generateUrlFor(code));
    }

    //used for testing --making sure the Map gets instantiated after a call to the constructor
    public Map getMap() {
        return map;
    }

}
