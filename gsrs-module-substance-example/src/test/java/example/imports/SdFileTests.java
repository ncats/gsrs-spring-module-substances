package example.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.dataexchange.model.MappingAction;
import gsrs.importer.PropertyBasedDataRecordContext;
import gsrs.imports.ImportAdapterStatistics;
import gsrs.module.substance.importers.SDFImportAdapter;
import gsrs.module.substance.importers.SDFImportAdapterFactory;
import gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase;
import gsrs.module.substance.importers.model.ChemicalBackedSDRecordContext;
import ix.ginas.importers.InputFieldStatistics;
import ix.ginas.importers.InputFileStatistics;
import ix.ginas.modelBuilders.AbstractSubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase.ACTION_NAME;
import static gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase.ACTION_PARAMETERS;
import static gsrs.module.substance.importers.importActionFactories.SubstanceImportAdapterFactoryBase.FILE_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class SdFileTests {

    private static final String NAME_FIELD = "PREFERRED_NAME";
    private static final String CODE_FIELD = "REGISTRY_CODE";

    @Test
    void adapterMetadataAndAccessors() {
        SDFImportAdapterFactory adapterFactory = new SDFImportAdapterFactory();

        assertEquals("SDF Adapter", adapterFactory.getAdapterName());
        assertEquals("SDF", adapterFactory.getAdapterKey());
        assertEquals(Arrays.asList("sdf", "sd"), adapterFactory.getSupportedFileExtensions());

        adapterFactory.setSupportedFileExtensions(Collections.singletonList("sdfx"));
        assertEquals(Collections.singletonList("sdfx"), adapterFactory.getSupportedFileExtensions());

        adapterFactory.setFileName("sample.sdf");
        assertEquals("sample.sdf", adapterFactory.getFileName());

        adapterFactory.setDescription("custom description");
        assertEquals("custom description", adapterFactory.getDescription());

        adapterFactory.setStagingAreaService(String.class);
        assertEquals(String.class, adapterFactory.getStagingAreaService());
        assertEquals(String.class, adapterFactory.getStagingAreaEntityService());
        adapterFactory.setStagingAreaEntityService(Integer.class);
        assertEquals(Integer.class, adapterFactory.getStagingAreaService());

        adapterFactory.setEntityServiceClass(Long.class);
        assertEquals(Long.class, adapterFactory.getEntityServiceClass());
        assertNull(adapterFactory.getEntityServices());
    }

    @Test
    @DisplayName("Default SDF action generation covers structure, field actions, and reference")
    void defaultSdfInstructionsIncludeStableStructureAndReferenceActions() {
        JsonNode importInfo = newAdapterFactory().createDefaultSdfFileImport(fieldStats(
                "CAS",
                "select_name",
                "melting point",
                "free text note"
        ));

        List<JsonNode> actions = actions(importInfo);
        assertEquals(6, actions.size());
        assertEquals("structure_and_moieties", actions.get(0).get(ACTION_NAME).asText());
        assertEquals("public_reference", actions.get(actions.size() - 1).get(ACTION_NAME).asText());
        assertEquals("[[UUID_1]]", actions.get(actions.size() - 1).get(ACTION_PARAMETERS).get("uuid").asText());
    }

    @ParameterizedTest
    @CsvSource({
            "Preferred Name,common_name",
            "synonym list,common_name",
            "melting point,property_import",
            "molecular formula,property_import",
            "CAS,code_import",
            "RN,code_import",
            "free text,note_import"
    })
    void defaultSdfInstructionsChooseExpectedActionForField(String fieldName, String expectedAction) {
        JsonNode importInfo = newAdapterFactory().createDefaultSdfFileImport(fieldStats(fieldName));

        JsonNode action = actionForField(importInfo, fieldName);

        assertEquals(expectedAction, action.get(ACTION_NAME).asText());
        assertNotNull(action.get(ACTION_PARAMETERS));
    }

    @Test
    void predictSettingsFromGeneratedSdfIncludesSchemaSettingsAndRecordCount() throws Exception {
        SDFImportAdapterFactory adapterFactory = newAdapterFactory();
        adapterFactory.setFileName("generated.sdf");

        try (InputStream sdf = sdfStream(testChemical("Octane", "BD-1"))) {
            ImportAdapterStatistics settings = adapterFactory.predictSettings(sdf, null);

            assertNotNull(settings);
            assertEquals(1, settings.getAdapterSettings().get("RecordCount").asInt());
            assertEquals("generated.sdf", settings.getAdapterSchema().get("fileName").asText());
            assertTrue(settings.getAdapterSettings().get("actions").size() >= 3);
        }
    }

    @Test
    void predictSettingsReturnsNullWhenInputCannotBeRead() {
        ImportAdapterStatistics settings = new ThrowingSdfImportAdapterFactory()
                .predictSettings(new ByteArrayInputStream(new byte[0]), null);

        assertNull(settings);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void parseChemicalAppliesDirectUnitActionsWithoutSpringContext(boolean includeEncoding) throws Exception {
        SDFImportAdapter adapter = new SDFImportAdapter(Arrays.asList(
                addNameFrom(NAME_FIELD),
                addPrimaryCodeFrom(CODE_FIELD, "BDNUM")
        ));
        ObjectNode runtimeSettings = JsonNodeFactory.instance.objectNode();
        if (includeEncoding) {
            runtimeSettings.put("Encoding", StandardCharsets.UTF_8.name());
        }

        List<Substance> substances = parse(adapter, testChemical("Octane", "BD-1"), runtimeSettings);

        assertEquals(1, substances.size());
        Substance substance = substances.get(0);
        assertEquals(Substance.SubstanceClass.chemical, substance.substanceClass);
        assertEquals("Octane", substance.names.get(0).name);
        assertTrue(substance.codes.stream()
                .anyMatch(code -> "BDNUM".equals(code.codeSystem)
                        && "BD-1".equals(code.code)
                        && "PRIMARY".equals(code.type)));
    }

    @Test
    void parseContinuesWhenActionThrowsAndFileEncodingAccessorsRoundTrip() throws Exception {
        SDFImportAdapter adapter = new SDFImportAdapter(Collections.singletonList((builder, context) -> {
            throw new IllegalStateException("action failure");
        }));
        adapter.setFileEncoding(StandardCharsets.UTF_8.name());

        List<Substance> substances = parseSilencingSystemErr(
                adapter,
                testChemical("Octane", "BD-1"),
                JsonNodeFactory.instance.objectNode());

        assertEquals(StandardCharsets.UTF_8.name(), adapter.getFileEncoding());
        assertEquals(1, substances.size());
        assertEquals(Substance.SubstanceClass.chemical, substances.get(0).substanceClass);
        assertTrue(substances.get(0).names.isEmpty());
    }

    @Test
    void resolveParametersResolvesSdPropertiesMolfileAndStableSpecialTokens() throws Exception {
        ChemicalBackedSDRecordContext context = sdContext(testChemical("Octane", "BD-1"));

        List<String> resolved = SDFImportAdapterFactory.resolveParameters(context,
                Arrays.asList("{{" + NAME_FIELD + "}}", "{{molfile_name}}", "[[UUID_1]]", "[[UUID_1]]"));

        assertEquals("Octane", resolved.get(0));
        assertEquals("Octane", resolved.get(1));
        assertEquals(resolved.get(2), resolved.get(3));
        assertNotNull(UUID.fromString(resolved.get(2)));
    }

    @Test
    void resolveParameterLeavesOriginalTextWhenNoTokenCanBeResolved() throws Exception {
        ChemicalBackedSDRecordContext context = sdContext(testChemical("Octane", "BD-1"));

        assertEquals("plain text", SDFImportAdapterFactory.resolveParameter(context, "plain text"));
        assertEquals("{{missing}}", SDFImportAdapterFactory.resolveParameter(context, "{{missing}}"));
    }

    @Test
    void resolveParameterUsesEncoderForSdAndNonSdContexts() throws Exception {
        ChemicalBackedSDRecordContext sdContext = sdContext(testChemical("Octane", "BD-1"));
        SimplePropertyContext propertyContext = new SimplePropertyContext(Collections.singletonMap("FOO", "BAR"));

        assertEquals("[Octane]", SubstanceImportAdapterFactoryBase.resolveParameter(
                sdContext, "{{" + NAME_FIELD + "}}", value -> "[" + value + "]"));
        assertEquals("[BAR]", SubstanceImportAdapterFactoryBase.resolveParameter(
                propertyContext, "{{FOO}}", value -> "[" + value + "]"));
    }

    @Test
    void resolveParametersMapPreservesRepeatedSpecialValuesAndReplacesProperties() throws Exception {
        ChemicalBackedSDRecordContext context = sdContext(testChemical("Octane", "BD-1"));
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("molfile", "{{molfile}}");
        settings.put("name", "{{" + NAME_FIELD + "}}");
        settings.put("uuid", "[[UUID_1]]");
        settings.put("sameUuid", "[[UUID_1]]");
        settings.put("otherUuid", "[[UUID_2]]");

        Map<String, Object> resolved = newAdapterFactory().resolveParametersMap(context, settings);

        assertTrue(resolved.get("molfile").toString().contains("V2000"));
        assertEquals("Octane", resolved.get("name"));
        assertEquals(resolved.get("uuid"), resolved.get("sameUuid"));
        assertNotEquals(resolved.get("uuid"), resolved.get("otherUuid"));
    }

    @ParameterizedTest
    @CsvSource({
            "melting point,true",
            "TPSA,true",
            "description,false"
    })
    void propertyHeuristicIdentifiesLikelyProperties(String fieldName, boolean expected) {
        assertEquals(expected, newAdapterFactory().isPropertyField(fieldName));
    }

    @ParameterizedTest
    @CsvSource({
            "CAS,true",
            "PubChem_Compound_CID,true",
            "RN,true",
            "internal registry,false"
    })
    void codeHeuristicIdentifiesLikelyCodes(String fieldName, boolean expected) {
        assertEquals(expected, newAdapterFactory().isCodeField(fieldName));
    }

    @ParameterizedTest
    @CsvSource({
            "protein sequence,protein",
            "nucleic acid sequence,nucleic",
            "canonical smiles,smiles",
            "molfile,molfile"
    })
    void sequenceAndStructureHeuristicsIdentifySpecialFields(String fieldName, String expectedType) {
        TestableSdfImportAdapterFactory factory = newAdapterFactory();

        assertEquals("protein".equals(expectedType), factory.isProteinSequenceField(fieldName));
        assertEquals("nucleic".equals(expectedType), factory.isNucleicAcidSequenceField(fieldName));
        assertEquals("smiles".equals(expectedType), factory.isSmilesField(fieldName));
        assertEquals("molfile".equals(expectedType), factory.isMolfileField(fieldName));
    }

    private static TestableSdfImportAdapterFactory newAdapterFactory() {
        return new TestableSdfImportAdapterFactory();
    }

    private static Map<String, InputFieldStatistics> fieldStats(String... fieldNames) {
        Map<String, InputFieldStatistics> fields = new LinkedHashMap<>();
        Arrays.stream(fieldNames).forEach(field -> fields.put(field, new InputFieldStatistics(field)));
        return fields;
    }

    private static List<JsonNode> actions(JsonNode importInfo) {
        List<JsonNode> actions = new ArrayList<>();
        importInfo.get("actions").forEach(actions::add);
        return actions;
    }

    private static JsonNode actionForField(JsonNode importInfo, String fieldName) {
        for (JsonNode action : importInfo.get("actions")) {
            if (action.has(FILE_FIELD) && fieldName.equals(action.get(FILE_FIELD).asText())) {
                return action;
            }
        }
        return fail("No action found for field: " + fieldName);
    }

    private static Chemical testChemical(String name, String code) throws IOException {
        Chemical chemical = Chemical.parse("CCCCCCCC");
        chemical.setName(name);
        chemical.setProperty(NAME_FIELD, name);
        chemical.setProperty(CODE_FIELD, code);
        return chemical;
    }

    private static ChemicalBackedSDRecordContext sdContext(Chemical chemical) {
        return new ChemicalBackedSDRecordContext(chemical);
    }

    private static InputStream sdfStream(Chemical chemical) throws IOException {
        return new ByteArrayInputStream(chemical.toSd().getBytes(StandardCharsets.UTF_8));
    }

    private static List<Substance> parse(SDFImportAdapter adapter,
                                         Chemical chemical,
                                         ObjectNode runtimeSettings) throws IOException {
        try (InputStream sdf = sdfStream(chemical)) {
            return adapter.parse(sdf, runtimeSettings, null).collect(Collectors.toList());
        }
    }

    private static List<Substance> parseSilencingSystemErr(SDFImportAdapter adapter,
                                                           Chemical chemical,
                                                           ObjectNode runtimeSettings) throws IOException {
        java.io.PrintStream originalErr = System.err;
        try {
            System.setErr(new java.io.PrintStream(new java.io.ByteArrayOutputStream()));
            return parse(adapter, chemical, runtimeSettings);
        } finally {
            System.setErr(originalErr);
        }
    }

    private static MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> addNameFrom(String field) {
        return (builder, context) -> {
            String value = context.getProperty(field).orElseThrow(IllegalStateException::new);
            Name name = new Name();
            name.name = value;
            name.stdName = value;
            name.displayName = true;
            return builder.addName(name);
        };
    }

    private static MappingAction<AbstractSubstanceBuilder, PropertyBasedDataRecordContext> addPrimaryCodeFrom(String field,
                                                                                                             String codeSystem) {
        return (builder, context) -> {
            Code code = new Code();
            code.codeSystem = codeSystem;
            code.code = context.getProperty(field).orElseThrow(IllegalStateException::new);
            code.type = "PRIMARY";
            return builder.addCode(code);
        };
    }

    private static final class TestableSdfImportAdapterFactory extends SDFImportAdapterFactory {
        private boolean isPropertyField(String fieldName) {
            return looksLikeProperty(fieldName);
        }

        private boolean isCodeField(String fieldName) {
            return looksLikeCode(fieldName);
        }

        private boolean isProteinSequenceField(String fieldName) {
            return looksLikeProteinSequence(fieldName);
        }

        private boolean isNucleicAcidSequenceField(String fieldName) {
            return looksLikeNucleicAcidSequence(fieldName);
        }

        private boolean isSmilesField(String fieldName) {
            return looksLikeSmiles(fieldName);
        }

        private boolean isMolfileField(String fieldName) {
            return looksLikeMolfile(fieldName);
        }
    }

    private static final class ThrowingSdfImportAdapterFactory extends SDFImportAdapterFactory {
        @Override
        public InputFileStatistics getFieldsForFile(InputStream input) throws IOException {
            throw new IOException("forced read failure");
        }
    }

    private static final class SimplePropertyContext implements PropertyBasedDataRecordContext {
        private final Map<String, String> properties;

        private SimplePropertyContext(Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        public Optional<String> getProperty(String name) {
            return Optional.ofNullable(properties.get(name));
        }

        @Override
        public List<String> getProperties() {
            return properties.keySet().stream().collect(Collectors.toList());
        }

        @Override
        public Map<String, String> getSpecialPropertyMap() {
            return Collections.emptyMap();
        }
    }

}
