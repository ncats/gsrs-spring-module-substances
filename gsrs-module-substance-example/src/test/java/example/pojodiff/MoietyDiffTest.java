package example.pojodiff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.ChemicalSubstance;
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
        old.structure.smiles = "CC";
        old.structure.id = uuid2;

        old.enforce();

        Moiety newMoiety = new Moiety();
        newMoiety.uuid = uuid;
        newMoiety.structure = new GinasChemicalStructure();
        newMoiety.structure.smiles = "CCO";
        newMoiety.structure.id = uuid2;

        newMoiety.enforce();

        PojoPatch<Moiety> patch = PojoDiff.getDiff(old, newMoiety);
        patch.apply(old);

        JsonMatches(newMoiety, old);
    }

    @Test
    public void changeMoietyFieldWhenSerializedIdDiffersFromEntityKey() throws Exception {
        UUID serializedStructureId = UUID.randomUUID();
        String jpaKey = UUID.randomUUID().toString();
        UUID moietyUuid = UUID.randomUUID();

        ChemicalSubstance oldChemical = new ChemicalSubstance();
        oldChemical.uuid = UUID.randomUUID();
        oldChemical.moieties.add(moiety(serializedStructureId, jpaKey, moietyUuid, "old-digest"));

        ChemicalSubstance newChemical = new ChemicalSubstance();
        newChemical.uuid = oldChemical.uuid;
        newChemical.moieties.add(moiety(serializedStructureId, jpaKey, moietyUuid, "new-digest"));

        PojoPatch<ChemicalSubstance> patch = PojoDiff.getDiff(oldChemical, newChemical);
        patch.apply(oldChemical);

        assertEquals("new-digest", oldChemical.moieties.get(0).structure.digest);
        assertEquals(jpaKey, oldChemical.moieties.get(0).innerUuid);
    }

    private Moiety moiety(UUID structureId, String innerUuid, UUID moietyUuid, String digest) {
        Moiety moiety = new Moiety();
        moiety.uuid = moietyUuid;
        moiety.innerUuid = innerUuid;
        moiety.structure = new GinasChemicalStructure();
        moiety.structure.id = structureId;
        moiety.structure.digest = digest;
        moiety.structure.smiles = "CC";
        return moiety;
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
