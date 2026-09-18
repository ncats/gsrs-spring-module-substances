package ix.ginas.models.serialization;

import tools.jackson.core.JsonParser;
import ix.core.models.Keyword;
import ix.ginas.models.GinasCommonSubData;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.LinkedHashSet;
import java.util.Set;

public class ReferenceSetDeserializer extends ValueDeserializer<Set<Keyword>> {
    public ReferenceSetDeserializer () {
    }

    public Set<Keyword> deserialize
        (JsonParser parser, DeserializationContext ctx)
         {
    	Set<Keyword> refs = null;
        JsonToken token = parser.currentToken();
        if (JsonToken.START_ARRAY == token) {
            refs = new LinkedHashSet<Keyword>();
            while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
                if (token == JsonToken.VALUE_STRING) {
                    refs.add(new Keyword
                             (GinasCommonSubData.REFERENCE, parser.getValueAsString()));
                }
            }
        }
        return refs;
    }
}
