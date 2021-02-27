package ix.ginas.models.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import ix.core.models.VIntArray;

import java.io.IOException;

public class IntArraySerializer extends JsonSerializer<VIntArray> {
    public IntArraySerializer () {}
    public void serialize (VIntArray array, JsonGenerator jgen,
                           SerializerProvider provider)
        throws IOException, JsonProcessingException {
        if (array != null) {
            int[] ary = array.getArray();
            jgen.writeStartArray();
            for (int i = 0; i < ary.length; ++i)
                jgen.writeNumber(ary[i]);
            jgen.writeEndArray();
        }
        else {
            jgen.writeNull();
        }
    }
}
