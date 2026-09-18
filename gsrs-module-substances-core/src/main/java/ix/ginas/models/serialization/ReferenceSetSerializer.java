package ix.ginas.models.serialization;

import ix.core.models.Keyword;
import ix.ginas.models.GinasCommonSubData;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.LinkedHashSet;
import java.util.Set;

public class ReferenceSetSerializer extends ValueSerializer<Set<Keyword>> {
    public ReferenceSetSerializer () {}

    @Override
    public void serialize(Set<Keyword> list, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
        Set<String> refs = new LinkedHashSet<String>();
        for (Keyword val : list) {
            if (GinasCommonSubData.REFERENCE.equals(val.label) || val.label==null) {
                Keyword kw = (Keyword)val;
                refs.add(kw.term);
            }
        }
        ctxt.writeValue(jgen, refs);
    }
}
	