package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Moiety;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UpdateChemicalWithPersistedMoietyAmountTest extends AbstractSubstanceJpaEntityTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void updateChemicalWithPersistedMoietyAmountUuid() throws Exception {

        File jsonFile = new ClassPathResource("testJSON/1_5-naphthyridin-3-ol.json").getFile();
        ChemicalSubstanceBuilder builder = SubstanceBuilder.from(jsonFile);
        ChemicalSubstance chem =builder.build();
        ChemicalSubstance created = (ChemicalSubstance) assertCreated(chem.toFullJsonNode());
        Moiety createdMoiety = created.moieties.get(0);
        assertNotNull(createdMoiety.getCountAmount());
        assertNotNull(createdMoiety.getCountAmount().uuid);

        JsonNode updateJson = created.toFullJsonNode();
        ((com.fasterxml.jackson.databind.node.ArrayNode) updateJson.get("names")).add(
                mapper.readTree("""
                        {
                          "references": ["ba459ffe-4fd9-4be3-8c11-98a79d0da0ca"],
                          "access": [],
                          "languages": ["en"],
                          "type": "cn",
                          "name": "1,5-naphthyridin-3-ol B"
                        }
                        """));

        ChemicalSubstance updated = (ChemicalSubstance) assertUpdated(updateJson);

        assertEquals(2, updated.names.size());
        assertEquals(createdMoiety.getCountAmount().uuid, updated.moieties.get(0).getCountAmount().uuid);
    }
}
