package gsrs.module.substance.misc.emasmsfhir;

import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmaSmsSimpleRecordFhirMapper Tests")
class EmaSmsSimpleRecordFhirMapperTest {

    private EmaSmsSimpleRecordFhirMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EmaSmsSimpleRecordFhirMapper();
    }

    @Test
    @DisplayName("Null substance throws")
    void nullSubstanceThrows() {
        assertThrows(NullPointerException.class, () -> mapper.generateEmaSmsSimpleRecordFromSubstance(null));
    }

    @Test
    @DisplayName("Display name fields are mapped")
    void displayNameFieldsAreMapped() {
        Substance substance = createChemicalSubstanceWithDisplayName("Sodium Chloride");

        EmaSmsSimpleRecord record = mapper.generateEmaSmsSimpleRecordFromSubstance(substance);

        assertEquals("Sodium Chloride", record.getSubstanceName().getValue());
        assertEquals("en", record.getLanguage2().getValue());
        assertEquals("FDA SUBSTANCE REGISTRATION SYSTEM", record.getNameSource().getValue());
        assertTrue(record.getIsPreferredName().booleanValue());
        assertEquals("chemical", record.getSubstanceType().getValue());
    }

    @Test
    @DisplayName("Code fields are mapped and non-primary code keeps type suffix")
    void codeFieldsAreMapped() {
        Substance substance = createChemicalSubstanceWithDisplayName("Acetaminophen");
        substance.codes.add(createCode("EVMPD", "EV123", "PRIMARY"));
        substance.codes.add(createCode("FDA UNII", "451W47IQ8X", "PRIMARY"));
        substance.codes.add(createCode("INN", "INN-123", "SECONDARY"));
        substance.codes.add(createCode("ECHA (EC/EINECS)", "231-959-4", "PRIMARY"));

        EmaSmsSimpleRecord record = mapper.generateEmaSmsSimpleRecordFromSubstance(substance);

        assertEquals("EV123", record.getEvCode().getValue());
        assertEquals("451W47IQ8X", record.getUnii().getValue());
        assertEquals("INN-123 [SECONDARY]", record.getInnNumber().getValue());
        assertEquals("231-959-4", record.getEcListNumber().getValue());
    }

    @Test
    @DisplayName("Missing display name only omits display-name specific fields")
    void missingDisplayNameStillMapsCoreFields() {
        Substance substance = new SubstanceBuilder().asChemical().generateNewUUID().build();

        EmaSmsSimpleRecord record = mapper.generateEmaSmsSimpleRecordFromSubstance(substance);

        assertNull(record.getSubstanceName());
        assertNull(record.getLanguage2());
        assertNull(record.getNameSource());
        assertEquals("chemical", record.getSubstanceType().getValue());
        assertEquals("", record.getEvCode().getValue());
    }

    @Test
    @DisplayName("Code-system lookup is case-insensitive")
    void codeSystemLookupIsCaseInsensitive() {
        Substance substance = createChemicalSubstanceWithDisplayName("Case Test");
        substance.codes.add(createCode("inn", "INN-LOWER", "PRIMARY"));

        EmaSmsSimpleRecord record = mapper.generateEmaSmsSimpleRecordFromSubstance(substance);

        assertEquals("INN-LOWER", record.getInnNumber().getValue());
    }

    @Test
    @DisplayName("Serialized GSRS payload is always populated")
    void serializedGsrsPayloadIsPopulated() {
        Substance substance = createChemicalSubstanceWithDisplayName("Payload Test");

        EmaSmsSimpleRecord record = mapper.generateEmaSmsSimpleRecordFromSubstance(substance);

        assertNotNull(record.getGsrsSubstance());
        assertTrue(record.getGsrsSubstance().getValue().contains("\"substanceClass\""));
    }

    private static Substance createChemicalSubstanceWithDisplayName(String name) {
        return new SubstanceBuilder()
                .asChemical()
                .generateNewUUID()
                .addName(name, n -> {
                    n.displayName = true;
                    n.addLanguage("en");
                })
                .build();
    }

    private static Code createCode(String codeSystem, String code, String type) {
        Code c = new Code(codeSystem, code);
        c.type = type;
        return c;
    }
}

