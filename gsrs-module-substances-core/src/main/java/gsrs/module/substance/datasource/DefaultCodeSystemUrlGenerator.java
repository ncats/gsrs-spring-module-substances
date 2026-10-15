package gsrs.module.substance.datasource;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gsrs.module.substance.processors.CodeSystemUrlGenerator;
import ix.ginas.models.v1.Code;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultCodeSystemUrlGenerator implements DataSet<CodeSystemMeta>, CodeSystemUrlGenerator {

/*
    Configure the list of known code systems directly in your HOCON configuration as the
    processor's "parameters" block.  For example:

    gsrs.entityProcessors.list.CodeProcessor = {
        "entityClassName":"ix.ginas.models.v1.Code",
        "processor":"gsrs.module.substance.processors.CodeProcessor",
        "order": 1000,
        "parameters": {
            "class":"gsrs.module.substance.datasource.DefaultCodeSystemUrlGenerator",
            "codeSystems": {
                "XYZ": "http://some.url.com/$CODE$",
                ...
            }
        }
    }

    Each entry maps a code system name to the URL template used for its codes.  The URL
    template may contain "$CODE$" (replaced with the code value) and "$CODETEXT$" (replaced
    with the code text).  The code system name is the key, so it must be unique; names are
    matched case-insensitively.  No external configuration file is read; the data comes
    exclusively from the module's parameters.
*/

    @JsonIgnore
    private final Map<String, CodeSystemMeta> map = new LinkedHashMap<>();

    @JsonCreator
    public DefaultCodeSystemUrlGenerator(@JsonProperty("codeSystems") Map<String, String> codeSystems) {
        for (Map.Entry<String, String> entry : codeSystems.entrySet()) {
            CodeSystemMeta csmap = new CodeSystemMeta(entry.getKey(), entry.getValue());
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
