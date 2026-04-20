package example.pojodiff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import ix.core.controllers.EntityFactory.EntityMapper;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Moiety;
import ix.utils.pojopatch.PojoDiff;
import ix.utils.pojopatch.PojoPatch;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by katzelda on 5/13/16.
 */
public class MoietyDiffTest{

    ObjectMapper mapper = new ObjectMapper();


    @Test
    public void changeNestedField() throws Exception{
        Moiety old = new Moiety();
        UUID uuid = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        old.uuid = uuid;
        old.structure = new GinasChemicalStructure();
        old.structure.smiles = "c1cccc1";
        old.structure.id = uuid2;

        old.enforce();

        Moiety newMoiety = new Moiety();
        newMoiety.uuid = uuid;
        newMoiety.structure = new GinasChemicalStructure();
        newMoiety.structure.smiles = "c1cccc1OH";
        newMoiety.structure.id = uuid2;

        newMoiety.enforce();

        PojoPatch<Moiety> patch = PojoDiff.getDiff(old, newMoiety);
        patch.apply(old);

        JsonMatches(newMoiety, old);
    }

    @Test
    public void deserializerPreservesMoietyUuid() throws Exception {
        UUID moietyUuid = UUID.randomUUID();
        UUID structureUuid = UUID.randomUUID();
        String json = "{"
                + "\"uuid\":\"" + moietyUuid + "\","
                + "\"id\":\"" + structureUuid + "\","
                + "\"smiles\":\"c1cccc1\","
                + "\"countAmount\":{\"type\":\"MOL RATIO\",\"average\":1,\"units\":\"MOL RATIO\"}"
                + "}";

        Moiety moiety = EntityMapper.FULL_ENTITY_MAPPER().readValue(json, Moiety.class);

        assertEquals(moietyUuid, moiety.uuid);
        assertEquals(structureUuid, moiety.structure.id);
        assertEquals(structureUuid.toString(), moiety.innerUuid);
    }

    private void JsonMatches(Object expected, Object actual){
        JsonNode js1=mapper.valueToTree(expected);
        JsonNode js2=mapper.valueToTree(actual);
        try{
            assertEquals(js1,js2);
        }catch(Throwable e){
            System.out.println(JsonDiff.asJson(js1, js2));
            throw e;
        }
    }
}
