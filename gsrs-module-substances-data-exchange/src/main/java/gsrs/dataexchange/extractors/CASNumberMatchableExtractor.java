package gsrs.dataexchange.extractors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

public class CASNumberMatchableExtractor extends CodeMatchableExtractor {

    public CASNumberMatchableExtractor(JsonNode config) {
        super(casCodeSystems, CAS_TYPE, CAS_KEY);
        casCodeSystems.clear();
        ((ArrayNode) config.get("casCodeSystems")).forEach(cs -> {
            casCodeSystems.add(cs.asText());
        });
        setRequiredCodeSystems(casCodeSystems);
    }

    private static List<String> casCodeSystems = new ArrayList<>(); //Arrays.asList("CAS", "CASNum", "CASNo");
    private final static String CAS_TYPE = "PRIMARY";
    private final static String CAS_KEY = "CAS NUMBER";

    public CASNumberMatchableExtractor() {
        super(casCodeSystems, CAS_TYPE, CAS_KEY);
    }

}
