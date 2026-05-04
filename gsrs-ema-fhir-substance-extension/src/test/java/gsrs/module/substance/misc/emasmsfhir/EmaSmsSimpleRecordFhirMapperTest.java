package gsrs.module.substance.misc.emasmsfhir;

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
        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Sodium Chloride");

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
        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Acetaminophen");
        substance.codes.add(EmaSmsFhirTestData.code("EVMPD", "EV123", "PRIMARY"));
        substance.codes.add(EmaSmsFhirTestData.code("FDA UNII", "451W47IQ8X", "PRIMARY"));
        substance.codes.add(EmaSmsFhirTestData.code("INN", "INN-123", "SECONDARY"));
        substance.codes.add(EmaSmsFhirTestData.code("ECHA (EC/EINECS)", "231-959-4", "PRIMARY"));

        EmaSmsSimpleRecord record = mapper.generateEmaSmsSimpleRecordFromSubstance(substance);

        assertEquals("EV123", record.getEvCode().getValue());
        assertEquals("451W47IQ8X", record.getUnii().getValue());
        assertEquals("INN-123 [SECONDARY]", record.getInnNumber().getValue());
        assertEquals("231-959-4", record.getEcListNumber().getValue());
    }

    @Test
    @DisplayName("Missing display name only omits display-name specific fields")
    void missingDisplayNameStillMapsCoreFields() {
        Substance substance = EmaSmsFhirTestData.chemicalSubstance();

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
        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Case Test");
        substance.codes.add(EmaSmsFhirTestData.code("inn", "INN-LOWER", "PRIMARY"));

        EmaSmsSimpleRecord record = mapper.generateEmaSmsSimpleRecordFromSubstance(substance);

        assertEquals("INN-LOWER", record.getInnNumber().getValue());
    }

    @Test
    @DisplayName("Serialized GSRS payload is always populated")
    void serializedGsrsPayloadIsPopulated() {
        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Payload Test");

        EmaSmsSimpleRecord record = mapper.generateEmaSmsSimpleRecordFromSubstance(substance);

        assertNotNull(record.getGsrsSubstance());
        assertTrue(record.getGsrsSubstance().getValue().contains("\"substanceClass\""));
    }
}

