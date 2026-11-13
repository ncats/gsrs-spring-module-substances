package ix.ginas.models.serialization;

import ix.core.models.VIntArray;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class IntArraySerializer extends ValueSerializer<VIntArray> {
    public IntArraySerializer () {}

    @Override
    public void serialize(VIntArray array, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
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
