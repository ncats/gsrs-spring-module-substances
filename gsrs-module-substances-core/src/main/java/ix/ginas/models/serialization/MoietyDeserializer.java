package ix.ginas.models.serialization;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import ix.core.controllers.EntityFactory.EntityMapper;
import ix.ginas.models.v1.Amount;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Moiety;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

public class MoietyDeserializer extends ValueDeserializer<Moiety> {

    private final static JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();

    public MoietyDeserializer () {
    }

    @Override
    public Moiety deserialize(JsonParser parser, DeserializationContext ctxt) throws JacksonException {
        JsonNode tree = mapper.readTree(parser);
        Moiety moiety = new Moiety();
        moiety.structure = mapper.treeToValue(tree, GinasChemicalStructure.class);
        JsonNode n = tree.get("count");
        if (n != null) {
            try{
                moiety.setCount(n.asInt());
            }catch(Exception e){
                Amount amnt= EntityMapper.FULL_ENTITY_MAPPER().treeToValue(n, Amount.class);
                moiety.setCountAmount(amnt);
            }
        }
        JsonNode namnt = tree.get("countAmount");
        if (namnt != null) {
            try{
                Amount amnt= EntityMapper.FULL_ENTITY_MAPPER().treeToValue(namnt, Amount.class);
                moiety.setCountAmount(amnt);
            }catch(Exception e){
                System.err.println(e.getMessage());
            }
        }
        JsonNode innerUuid = tree.get("innerUuid");
        if (innerUuid != null && !innerUuid.isNull() && !innerUuid.asText().isEmpty()) {
            moiety.innerUuid = innerUuid.asText();
        }
        moiety.enforce();
        return moiety;
    }

}
