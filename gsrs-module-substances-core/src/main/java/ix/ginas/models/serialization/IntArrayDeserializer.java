package ix.ginas.models.serialization;

import ix.core.models.VIntArray;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.ArrayList;
import java.util.List;

public class IntArrayDeserializer extends ValueDeserializer<VIntArray> {
    public IntArrayDeserializer () { }

    public VIntArray deserialize (JsonParser parser, DeserializationContext ctx)
    {
        VIntArray array = null;
        JsonToken token = parser.currentToken();
        if (JsonToken.START_ARRAY == token) {
            List<Integer> list = new ArrayList<>();
            while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
                if (token == JsonToken.VALUE_NUMBER_INT) {
                    list.add(parser.getIntValue());
                }
            }
            int[] ary = new int[list.size()];
            for (int i = 0; i < ary.length; ++i)
                ary[i] = list.get(i);
            
            array = new VIntArray(null, ary);
        }
        return array;
    }
}
