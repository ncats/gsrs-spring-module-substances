package gsrs.module.substance.misc.emasmsfhir;

import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmaSmsFhrUtils Tests")
class EmaSmsFhrUtilsTest {

    // -----------------------------------------------------------------------
    // findCodeByCodeSystem
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Returns code for PRIMARY type (no bracket suffix)")
    void findCode_primaryType_returnsCodeOnly() {
        Substance s = substanceWithCode("EVMPD", "EV-001", "PRIMARY");
        assertEquals("EV-001", EmaSmsFhrUtils.findCodeByCodeSystem("EVMPD", s));
    }

    @Test
    @DisplayName("Returns bracketed code for non-PRIMARY type")
    void findCode_nonPrimaryType_returnsBracketedCode() {
        Substance s = substanceWithCode("INN", "INN-99", "SECONDARY");
        assertEquals("INN-99 [SECONDARY]", EmaSmsFhrUtils.findCodeByCodeSystem("INN", s));
    }

    @Test
    @DisplayName("Returns empty string when code system not found")
    void findCode_unknownSystem_returnsEmpty() {
        Substance s = substanceWithCode("FDA UNII", "UNII-1", "PRIMARY");
        assertEquals("", EmaSmsFhrUtils.findCodeByCodeSystem("MISSING", s));
    }

    @Test
    @DisplayName("Lookup is case-insensitive for code system")
    void findCode_caseInsensitive() {
        Substance s = substanceWithCode("FDA UNII", "UNII-X", "PRIMARY");
        assertEquals("UNII-X", EmaSmsFhrUtils.findCodeByCodeSystem("fda unii", s));
        assertEquals("UNII-X", EmaSmsFhrUtils.findCodeByCodeSystem("FDA UNII", s));
        assertEquals("UNII-X", EmaSmsFhrUtils.findCodeByCodeSystem("FdA UnIi", s));
    }

    @Test
    @DisplayName("Returns empty string when substance has no codes")
    void findCode_noCodes_returnsEmpty() {
        Substance s = new SubstanceBuilder().asChemical().generateNewUUID().build();
        assertEquals("", EmaSmsFhrUtils.findCodeByCodeSystem("EVMPD", s));
    }

    @Test
    @DisplayName("Returns first matching code when multiple codes share a code system")
    void findCode_multipleMatchingCodes_returnsFirst() {
        Substance s = new SubstanceBuilder().asChemical().generateNewUUID().build();
        s.codes.add(makeCode("INN", "FIRST", "PRIMARY"));
        s.codes.add(makeCode("INN", "SECOND", "PRIMARY"));
        String result = EmaSmsFhrUtils.findCodeByCodeSystem("INN", s);
        assertEquals("FIRST", result);
    }

    // -----------------------------------------------------------------------
    // gsrsSubstanceToQuotedJson
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Serializes substance to valid JSON string")
    void gsrsSubstanceToQuotedJson_returnsJsonString() {
        Substance s = new SubstanceBuilder().asChemical().generateNewUUID().build();
        String json = EmaSmsFhrUtils.gsrsSubstanceToQuotedJson(s);
        assertNotNull(json);
        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("\"substanceClass\""));
    }

    @Test
    @DisplayName("Serialized JSON contains uuid when uuid is set")
    void gsrsSubstanceToQuotedJson_containsUuid() {
        Substance s = new SubstanceBuilder().asChemical().generateNewUUID().build();
        String uuid = s.getUuid().toString();
        String json = EmaSmsFhrUtils.gsrsSubstanceToQuotedJson(s);
        assertTrue(json.contains(uuid));
    }

    // -----------------------------------------------------------------------
    // getEmaSmsSubstanceTypeFromGsrsSubstanceClass
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "class={0} -> type={1}")
    @CsvSource({
        "chemical,            chemical",
        "protein,             protein",
        "nucleicAcid,         nucleicAcid",
        "polymer,             polymer",
        "mixture,             mixture",
        "specifiedSubstanceG1,specifiedSubstanceGroup1",
        "specifiedSubstanceG2,SpecifiedSubstanceGroup2",
        "specifiedSubstanceG3,SpecifiedSubstance Group3",
        "specifiedSubstanceG4,SpecifiedSubstanceGroup4"
    })
    @DisplayName("Maps known substance class correctly")
    void getEmaSmsSubstanceType_knownClass_returnsMappedType(String substanceClass, String expectedType) {
        assertEquals(expectedType.trim(),
                EmaSmsFhrUtils.getEmaSmsSubstanceTypeFromGsrsSubstanceClass(substanceClass.trim()));
    }

    @Test
    @DisplayName("Returns UNDEFINED for unknown substance class")
    void getEmaSmsSubstanceType_unknownClass_returnsUNDEFINED() {
        assertEquals("UNDEFINED", EmaSmsFhrUtils.getEmaSmsSubstanceTypeFromGsrsSubstanceClass("concept"));
        assertEquals("UNDEFINED", EmaSmsFhrUtils.getEmaSmsSubstanceTypeFromGsrsSubstanceClass("reference"));
        assertEquals("UNDEFINED", EmaSmsFhrUtils.getEmaSmsSubstanceTypeFromGsrsSubstanceClass("structurallyDiverse"));
        assertEquals("UNDEFINED", EmaSmsFhrUtils.getEmaSmsSubstanceTypeFromGsrsSubstanceClass("UNKNOWN_TYPE"));
    }

    @Test
    @DisplayName("Returns UNDEFINED for null substance class")
    void getEmaSmsSubstanceType_nullClass_returnsUNDEFINED() {
        assertEquals("UNDEFINED", EmaSmsFhrUtils.getEmaSmsSubstanceTypeFromGsrsSubstanceClass(null));
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private static Substance substanceWithCode(String codeSystem, String code, String type) {
        Substance s = new SubstanceBuilder().asChemical().generateNewUUID().build();
        s.codes.add(makeCode(codeSystem, code, type));
        return s;
    }

    private static Code makeCode(String codeSystem, String code, String type) {
        Code c = new Code(codeSystem, code);
        c.type = type;
        return c;
    }
}

