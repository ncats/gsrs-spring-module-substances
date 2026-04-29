package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.service.GsrsEntityService;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.models.Keyword;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Name;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    void fullJsonIncludesLegacyMoietyUuidAlias() throws Exception {

        File jsonFile = new ClassPathResource("testJSON/1_5-naphthyridin-3-ol.json").getFile();
        ChemicalSubstanceBuilder builder = SubstanceBuilder.from(jsonFile);
        ChemicalSubstance chem = builder.build();
        ChemicalSubstance created = (ChemicalSubstance) assertCreated(chem.toFullJsonNode());

        JsonNode fullJson = created.toFullJsonNode();
        JsonNode moietyJson = fullJson.path("moieties").path(0);

        assertNotNull(moietyJson.get("id"));
        assertNotNull(moietyJson.get("uuid"));
        assertEquals(moietyJson.get("id").asText(), moietyJson.get("uuid").asText());
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

    @Test
    void updateWithoutValidationWithChangedMoietyCollectionUsesReplacementPath() throws Exception {
        File jsonFile = new ClassPathResource("testJSON/1_5-naphthyridin-3-ol.json").getFile();
        ChemicalSubstance created = (ChemicalSubstance) assertCreated(SubstanceBuilder.from(jsonFile).build().toFullJsonNode());

        ObjectNode updateJson = (ObjectNode) created.toFullJsonNode();
        ArrayNode moieties = (ArrayNode) updateJson.get("moieties");
        assertNotNull(moieties);
        assertTrue(moieties.size() > 0);

        ObjectNode clonedMoiety = ((ObjectNode) moieties.get(0)).deepCopy();
        clonedMoiety.put("uuid", UUID.randomUUID().toString());
        clonedMoiety.put("innerUuid", UUID.randomUUID().toString());
        ObjectNode countAmount = (ObjectNode) clonedMoiety.get("countAmount");
        if (countAmount != null) {
            countAmount.put("uuid", UUID.randomUUID().toString());
        }
        moieties.add(clonedMoiety);

        GsrsEntityService.UpdateResult<ix.ginas.models.v1.Substance> result =
                substanceEntityService.updateEntityWithoutValidation(updateJson);

        assertEquals(GsrsEntityService.UpdateResult.STATUS.UPDATED, result.getStatus(),
                () -> "moiety collection change should bypass PojoDiff, but status was "
                        + result.getStatus() + ", throwable=" + result.getThrowable());
        ChemicalSubstance updated = (ChemicalSubstance) result.getUpdatedEntity();
        assertEquals(created.uuid, updated.uuid);
        assertEquals(2, updated.getMoieties().size());
    }

    @Test
    void updateWithValidationPersistsValidatorCorrectedMoieties() throws Exception {
        File jsonFile = new ClassPathResource("testJSON/1_5-naphthyridin-3-ol.json").getFile();
        ChemicalSubstance created = (ChemicalSubstance) assertCreated(SubstanceBuilder.from(jsonFile).build().toFullJsonNode());

        ObjectNode updateJson = (ObjectNode) created.toFullJsonNode();
        ArrayNode moieties = (ArrayNode) updateJson.get("moieties");
        ObjectNode clonedMoiety = ((ObjectNode) moieties.get(0)).deepCopy();
        clonedMoiety.put("uuid", UUID.randomUUID().toString());
        clonedMoiety.put("innerUuid", UUID.randomUUID().toString());
        ObjectNode countAmount = (ObjectNode) clonedMoiety.get("countAmount");
        if (countAmount != null) {
            countAmount.put("uuid", UUID.randomUUID().toString());
        }
        moieties.add(clonedMoiety);

        ValidationResponse<ix.ginas.models.v1.Substance> validationResponse =
                substanceEntityService.validateEntity(updateJson);
        ChemicalSubstance validated = (ChemicalSubstance) validationResponse.getNewObject();
        assertEquals(2, validated.getMoieties().size());

        GsrsEntityService.UpdateResult<ix.ginas.models.v1.Substance> result =
                substanceEntityService.updateEntity(updateJson, false);

        assertEquals(GsrsEntityService.UpdateResult.STATUS.UPDATED, result.getStatus(),
                () -> "validated update should succeed, but status was "
                        + result.getStatus() + ", throwable=" + result.getThrowable());
        ChemicalSubstance updated = (ChemicalSubstance) result.getUpdatedEntity();
        assertEquals(validated.getMoieties().size(), updated.getMoieties().size());
    }

    @Test
    void textOnlyChemicalUpdateDoesNotTriggerDefinitionalChangeWarning() throws Exception {
        File jsonFile = new ClassPathResource("testJSON/1_5-naphthyridin-3-ol.json").getFile();
        ChemicalSubstance created = (ChemicalSubstance) assertCreated(SubstanceBuilder.from(jsonFile).build().toFullJsonNode());

        ObjectNode updateJson = (ObjectNode) created.toFullJsonNode();
        ((ArrayNode) updateJson.get("names")).add(
                mapper.readTree("""
                        {
                          "references": ["ba459ffe-4fd9-4be3-8c11-98a79d0da0ca"],
                          "access": [],
                          "languages": ["en"],
                          "type": "cn",
                          "name": "1,5-naphthyridin-3-ol renamed"
                        }
                        """));

        ValidationResponse<ix.ginas.models.v1.Substance> response =
                substanceEntityService.validateEntity(updateJson);

        assertFalse(response.getValidationMessages().stream()
                        .map(message -> message.getMessage())
                        .anyMatch(message -> message != null && message.contains("Definitional change")),
                () -> "text-only update should not trigger definitional warning, but got "
                        + response.getValidationMessages());
    }

    @Test
    void updateChemicalWithCoordinateOnlyMolfileChangePersistsMolfile() throws Exception {
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = readMolfile("coordinate_edit_before.mol");
        ChemicalSubstance created = (ChemicalSubstance) assertCreated(new ChemicalSubstanceBuilder()
                .addName("coordinate-only molfile edit")
                .setStructure(structure)
                .build()
                .toFullJsonNode());

        ObjectNode updateJson = (ObjectNode) created.toFullJsonNode();
        ((ObjectNode) updateJson.get("structure")).put("molfile", readMolfile("coordinate_edit_after.mol"));

        ChemicalSubstance updated = (ChemicalSubstance) assertUpdated(updateJson);
        assertEditedCoordinatesPersisted(updated);

        ChemicalSubstance refetched = (ChemicalSubstance) substanceEntityService.get(created.uuid).orElseThrow();
        assertEditedCoordinatesPersisted(refetched);
    }

    private void assertEditedCoordinatesPersisted(ChemicalSubstance substance) {
        String molfile = normalizeMolfile(substance.getStructure().molfile);
        assertTrue(molfile.contains("   18.0243   -4.5961    0.0000 C"),
                () -> "edited atom coordinates were not persisted:\n" + molfile);
        assertTrue(molfile.contains("M  SDI   1  4   26.8647   -8.0600   26.8647   -5.1480"),
                () -> "edited S-group display coordinates were not persisted:\n" + molfile);
        assertFalse(molfile.contains("   20.1756   -5.6361    0.0000 C"),
                () -> "original atom coordinates were retained:\n" + molfile);
    }

    private String readMolfile(String name) throws Exception {
        try (InputStream inputStream = new ClassPathResource("molfiles/" + name).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String normalizeMolfile(String molfile) {
        return molfile == null ? "" : molfile.replace("\r\n", "\n").replace('\r', '\n');
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
