package gsrs.module.substance.processors;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import ix.core.models.Principal;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;

public class FakePrincipalDeserializer extends ValueDeserializer<Principal> {

    private final JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();

    @Override
    public Principal deserialize(JsonParser jsonParser, tools.jackson.databind.DeserializationContext ctxt) throws JacksonException {
        JsonToken token = jsonParser.currentToken();
        if (JsonToken.START_OBJECT == token) {
            JsonNode tree = mapper.readTree(jsonParser);
            /* this is really inconsistent with below in that we don't
             * register this principal if it's not already in the
             * persistence store..
             */
            return mapper.treeToValue(tree, Principal.class);
        }else{ // JsonToken.VALUE_STRING:
            String username = jsonParser.getValueAsString();
            return new Principal(username);
        }
    }
}
