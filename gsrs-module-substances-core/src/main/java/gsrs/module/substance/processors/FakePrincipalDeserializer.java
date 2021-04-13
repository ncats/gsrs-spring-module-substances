package gsrs.module.substance.processors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import ix.core.models.Principal;

import java.io.IOException;

public class FakePrincipalDeserializer extends JsonDeserializer<Principal> {
    @Override
    public Principal deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        JsonToken token = jsonParser.getCurrentToken();
        if (JsonToken.START_OBJECT == token) {
            JsonNode tree = jsonParser.getCodec().readTree(jsonParser);
            /* this is really inconsistent with below in that we don't
             * register this principal if it's not already in the
             * persistence store..
             */
            return jsonParser.getCodec().treeToValue(tree, Principal.class);
        }else{ // JsonToken.VALUE_STRING:
                String username = jsonParser.getValueAsString();
                return new Principal(username);
        }
    }
}
