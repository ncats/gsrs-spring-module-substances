package gsrs.module.substance.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

public interface CryptoService {

    public boolean isReady();

    public String sign(String str, Map<String, Object> metadata);

    public JsonNode verify(String jwsCompactStr);

    public void encrypt(ObjectNode node, List<String> recipients);

    public void decrypt(ObjectNode node);
}
