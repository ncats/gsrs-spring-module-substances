package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.service.GsrsEntityService;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.models.Keyword;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Name;
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

    @Test
    void updateChemicalWithNewName() throws Exception {

        File jsonFile = new ClassPathResource("testJSON/1_5-naphthyridin-3-ol.json").getFile();
        ChemicalSubstanceBuilder builder = SubstanceBuilder.from(jsonFile);
        ChemicalSubstance chem =builder.build();
        ChemicalSubstance created = (ChemicalSubstance) assertCreated(chem.toFullJsonNode());

        Name newChemicalName = new Name();
        newChemicalName.name = "Some other name for this chemical";
        newChemicalName.type = "cn";
        Keyword lang = new Keyword();
        lang.label = "lang";
        lang.term = "en";
        newChemicalName.languages.add(lang);
        newChemicalName.addReference(created.references.get(0));
        created.names.add(newChemicalName);

        JsonNode updateJson = created.toFullJsonNode();
        ChemicalSubstance updated = (ChemicalSubstance) assertUpdated(updateJson);

        assertEquals(2, updated.names.size());
    }

    @Test
    void updateRefetchedChemicalWithNewName() throws Exception {

        File jsonFile = new ClassPathResource("testJSON/1_5-naphthyridin-3-ol.json").getFile();
        ChemicalSubstanceBuilder builder = SubstanceBuilder.from(jsonFile);
        ChemicalSubstance chem = builder.build();
        ChemicalSubstance created = (ChemicalSubstance) assertCreated(chem.toFullJsonNode());

        ChemicalSubstance existing = (ChemicalSubstance) substanceEntityService.get(created.uuid).orElseThrow();
        JsonNode updateJson = existing.toFullJsonNode();
        ((com.fasterxml.jackson.databind.node.ArrayNode) updateJson.get("names")).add(
                mapper.readTree("""
                        {
                          "references": ["ba459ffe-4fd9-4be3-8c11-98a79d0da0ca"],
                          "access": [],
                          "languages": ["en"],
                          "type": "cn",
                          "name": "Some other name for this chemical"
                        }
                        """));

        ChemicalSubstance updated = (ChemicalSubstance) assertUpdated(updateJson);

        assertEquals(2, updated.names.size());
    }

    @Test
    void updateRefetchedChemicalWithNewNameAndNewReference() throws Exception {

        File jsonFile = new ClassPathResource("testJSON/1_5-naphthyridin-3-ol.json").getFile();
        ChemicalSubstanceBuilder builder = SubstanceBuilder.from(jsonFile);
        ChemicalSubstance chem = builder.build();
        ChemicalSubstance created = (ChemicalSubstance) assertCreated(chem.toFullJsonNode());

        ChemicalSubstance existing = (ChemicalSubstance) substanceEntityService.get(created.uuid).orElseThrow();
        JsonNode updateJson = existing.toFullJsonNode();

        ((com.fasterxml.jackson.databind.node.ArrayNode) updateJson.get("references")).add(
                mapper.readTree("""
                        {
                          "tags": [],
                          "access": [],
                          "docType": "CODEX Alimentarius",
                          "citation": "987",
                          "publicDomain": true,
                          "uuid": "05b45c53-b18b-47c4-a36f-fb9cb5f50e31"
                        }
                        """));
        ((com.fasterxml.jackson.databind.node.ArrayNode) updateJson.get("names")).add(
                mapper.readTree("""
                        {
                          "references": ["05b45c53-b18b-47c4-a36f-fb9cb5f50e31"],
                          "access": [],
                          "languages": ["en"],
                          "type": "cn",
                          "name": "alt 1,5-naphthyridin-3-ol"
                        }
                        """));

        ChemicalSubstance updated = (ChemicalSubstance) assertUpdated(updateJson);

        assertEquals(2, updated.names.size());
        assertEquals(2, updated.references.size());
    }

    @Test
    void updateUsingCapturedWebAfterPayload() throws Exception {
        JsonNode beforeJson = mapper.readTree(new ClassPathResource("testJSON/chemical_before_update.json").getInputStream());
        JsonNode afterJson = mapper.readTree(new ClassPathResource("testJSON/updated chemical.json").getInputStream());

        ChemicalSubstance created = (ChemicalSubstance) assertCreated(sanitizeCapturedChemicalForCreate(beforeJson.deepCopy()));
        ((ObjectNode) afterJson).put("uuid", created.uuid.toString());
        ((ObjectNode) afterJson).put("version", created.version);
        GsrsEntityService.UpdateResult<ix.ginas.models.v1.Substance> result = substanceEntityService.updateEntity(afterJson);
        assertEquals(GsrsEntityService.UpdateResult.STATUS.UPDATED, result.getStatus(),
                () -> "captured web payload update should succeed, but status was "
                        + result.getStatus()
                        + ", throwable=" + result.getThrowable()
                        + ", validation="
                        + (result.getValidationResponse() == null ? null : result.getValidationResponse().getValidationMessages()));

        ChemicalSubstance updated = (ChemicalSubstance) result.getUpdatedEntity();
        assertEquals(created.uuid, updated.uuid);
        assertEquals(2, updated.names.size());
        assertEquals(2, updated.references.size());
    }

    private JsonNode sanitizeCapturedChemicalForCreate(JsonNode json) {
        ObjectNode root = (ObjectNode) json;
        root.remove("created");
        root.remove("createdBy");
        root.remove("lastEdited");
        root.remove("lastEditedBy");
        root.remove("changeReason");
        root.remove("_self");
        root.remove("_name");
        root.remove("_approvalIDDisplay");

        ArrayNode names = (ArrayNode) root.get("names");
        if (names != null) {
            names.forEach(this::sanitizeAuditFields);
        }

        ArrayNode references = (ArrayNode) root.get("references");
        if (references != null) {
            references.forEach(this::sanitizeAuditFields);
        }

        ObjectNode structure = (ObjectNode) root.get("structure");
        if (structure != null) {
            sanitizeAuditFields(structure);
            structure.remove("properties");
            structure.remove("links");
            structure.remove("id");
            structure.remove("uuid");
        }

        ArrayNode moieties = (ArrayNode) root.get("moieties");
        if (moieties != null) {
            moieties.forEach(moietyNode -> {
                ObjectNode moiety = (ObjectNode) moietyNode;
                sanitizeAuditFields(moiety);
                moiety.remove("properties");
                moiety.remove("links");
                moiety.remove("id");
                moiety.remove("uuid");
                ObjectNode countAmount = (ObjectNode) moiety.get("countAmount");
                if (countAmount != null) {
                    sanitizeAuditFields(countAmount);
                    countAmount.remove("uuid");
                }
            });
        }

        return root;
    }

    private void sanitizeAuditFields(JsonNode node) {
        if (!(node instanceof ObjectNode objectNode)) {
            return;
        }
        objectNode.remove("created");
        objectNode.remove("createdBy");
        objectNode.remove("lastEdited");
        objectNode.remove("lastEditedBy");
    }

}
