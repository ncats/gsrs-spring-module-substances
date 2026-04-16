package gsrs.module.substance.misc.emasmsfhir;

import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.SubstanceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmaSmsSubstanceDefinitionFhirMapper Tests")
class EmaSmsSubstanceDefinitionFhirMapperTest {

    private EmaSmsSubstanceDefinitionFhirMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EmaSmsSubstanceDefinitionFhirMapper();
        mapper.setEmaSmsFhirConfiguration(createConfiguration());
    }

    @Test
    @DisplayName("Maps classification, name, and all configured codes")
    void mapsClassificationNameAndCodes() {
        Substance substance = createChemicalSubstanceWithDisplayName("Sodium Chloride");
        substance.codes.add(createCode("ECHA (EC/EINECS)", "231-959-4", "PRIMARY"));
        substance.codes.add(createCode("EVMPD", "EV123", "PRIMARY"));
        substance.codes.add(createCode("INN", "INN-123", "PRIMARY"));
        substance.codes.add(createCode("SMS ID", "SMS-987", "PRIMARY"));
        substance.codes.add(createCode("FDA UNII", "451W47IQ8X", "PRIMARY"));

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertEquals("example", sd.getId());
        assertEquals(1, sd.getClassification().size());
        assertEquals("CHEM-TID", sd.getClassification().get(0).getCodingFirstRep().getCode());
        assertEquals("https://sms/types/chemical", sd.getClassification().get(0).getCodingFirstRep().getSystem());

        assertEquals(1, sd.getName().size());
        assertEquals("Sodium Chloride", sd.getName().get(0).getName());
        assertEquals("en", sd.getName().get(0).getLanguage().get(0).getCodingFirstRep().getCode());

        assertEquals(5, sd.getCode().size());
    }

    @Test
    @DisplayName("Skips code entries when display code is absent")
    void skipsCodeWhenDisplayIsMissing() {
        Substance substance = createChemicalSubstanceWithDisplayName("No Codes");

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertNotNull(sd);
        assertTrue(sd.getCode().isEmpty());
    }

    @Test
    @DisplayName("Skips code entries when key is not configured")
    void skipsWhenCodeKeyIsMissingFromConfiguration() {
        EmaSmsFhirConfiguration cfg = createConfiguration();
        cfg.getCodeConfigs().remove("evCode");
        mapper.setEmaSmsFhirConfiguration(cfg);

        Substance substance = createChemicalSubstanceWithDisplayName("Partial Config");
        substance.codes.add(createCode("ECHA (EC/EINECS)", "231-959-4", "PRIMARY"));
        substance.codes.add(createCode("EVMPD", "EV123", "PRIMARY"));

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertEquals(1, sd.getCode().size());
        assertEquals("EC-TID", sd.getCode().get(0).getCode().getCodingFirstRep().getCode());
    }

    @Test
    @DisplayName("Uses empty code/system values when configuration values are null")
    void usesEmptyFallbackForNullCodeAndSystem() {
        EmaSmsFhirConfiguration cfg = createConfiguration();
        cfg.getCodeConfigs().get("evCode").put("smsTermId", null);
        cfg.getCodeConfigs().get("evCode").put("smsUrl", null);
        mapper.setEmaSmsFhirConfiguration(cfg);

        Substance substance = createChemicalSubstanceWithDisplayName("Null Config Values");
        substance.codes.add(createCode("EVMPD", "EV123", "PRIMARY"));

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertEquals(1, sd.getCode().size());
        assertNull(sd.getCode().get(0).getCode().getCodingFirstRep().getCode());
        assertNull(sd.getCode().get(0).getCode().getCodingFirstRep().getSystem());
        assertEquals("EV123", sd.getCode().get(0).getCode().getCodingFirstRep().getDisplay());
    }

    @Test
    @DisplayName("Adds GSRS substance extension when enabled")
    void addsGsrsExtensionWhenEnabled() {
        mapper.setIncludeGsrsSubstanceExtension(true);
        Substance substance = createChemicalSubstanceWithDisplayName("Extension Test");

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertEquals(1, sd.getExtension().size());
        Extension ext = sd.getExtension().get(0);
        assertEquals("https://gsrs.ncats.nih.gov/api/v1/substances", ext.getUrl());
        assertTrue(ext.getValue().primitiveValue().contains("\"substanceClass\""));
    }

    @Test
    @DisplayName("Handles no display name by omitting name section")
    void omitsNameWhenDisplayNameMissing() {
        Substance substance = new SubstanceBuilder().asChemical().generateNewUUID().build();

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertTrue(sd.getName().isEmpty());
        assertEquals(1, sd.getClassification().size());
    }

    @Test
    @DisplayName("Uses bracketed display for non-primary code types")
    void usesBracketedDisplayForNonPrimaryCodeTypes() {
        Substance substance = createChemicalSubstanceWithDisplayName("Non Primary");
        substance.codes.add(createCode("INN", "INN-XYZ", "SECONDARY"));

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertEquals(1, sd.getCode().size());
        assertEquals("INN-TID", sd.getCode().get(0).getCode().getCodingFirstRep().getCode());
        assertEquals("INN-XYZ [SECONDARY]", sd.getCode().get(0).getCode().getCodingFirstRep().getDisplay());
    }

    @Test
    @DisplayName("Does not add extension when toggle is disabled")
    void doesNotAddExtensionWhenDisabled() {
        mapper.setIncludeGsrsSubstanceExtension(false);
        Substance substance = createChemicalSubstanceWithDisplayName("No Extension");

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertTrue(sd.getExtension().isEmpty());
    }

    @Test
    @DisplayName("Configuration Lombok methods are exercised")
    void configurationLombokMethodsAreExercised() {
        EmaSmsFhirConfiguration a = createConfiguration();
        EmaSmsFhirConfiguration b = createConfiguration();

        assertEquals(a.getCodeConfigs(), b.getCodeConfigs());
        assertEquals(a.getSubstanceTypeConfigs(), b.getSubstanceTypeConfigs());
        assertEquals(a.getMiscDefaultConfigs(), b.getMiscDefaultConfigs());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.toString().contains("EmaSmsFhirConfiguration"));

        b.getCodeConfigs().get("evCode").put("smsTermId", "CHANGED");
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("Configuration Lombok methods with null fields")
    void configurationLombokWithNullFields() {
        EmaSmsFhirConfiguration a = new EmaSmsFhirConfiguration();
        EmaSmsFhirConfiguration b = new EmaSmsFhirConfiguration();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        b.setCodeConfigs(new HashMap<>());
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("Mapper Lombok-generated methods are exercised")
    void mapperLombokMethodsAreExercised() {
        EmaSmsSubstanceDefinitionFhirMapper a = new EmaSmsSubstanceDefinitionFhirMapper();
        EmaSmsSubstanceDefinitionFhirMapper b = new EmaSmsSubstanceDefinitionFhirMapper();

        EmaSmsFhirConfiguration cfg = createConfiguration();
        a.setEmaSmsFhirConfiguration(cfg);
        b.setEmaSmsFhirConfiguration(createConfiguration());
        a.setIncludeGsrsSubstanceExtension(true);
        b.setIncludeGsrsSubstanceExtension(true);

        assertEquals(cfg, a.getEmaSmsFhirConfiguration());
        assertTrue(a.isIncludeGsrsSubstanceExtension());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.canEqual(b));
        assertFalse(a.canEqual("not-a-mapper"));
        assertTrue(a.toString().contains("EmaSmsSubstanceDefinitionFhirMapper"));

        b.setIncludeGsrsSubstanceExtension(false);
        assertNotEquals(a, b);
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

    private static EmaSmsFhirConfiguration createConfiguration() {
        EmaSmsFhirConfiguration cfg = new EmaSmsFhirConfiguration();

        Map<String, Map<String, String>> codeConfigs = new HashMap<>();
        codeConfigs.put("ecListNumber", codeConfig("EC-TID", "ECHA (EC/EINECS)", "https://sms/codes/ec"));
        codeConfigs.put("evCode", codeConfig("EV-TID", "EVMPD", "https://sms/codes/ev"));
        codeConfigs.put("innNumber", codeConfig("INN-TID", "INN", "https://sms/codes/inn"));
        codeConfigs.put("smsId", codeConfig("SMS-TID", "SMS ID", "https://sms/codes/sms"));
        codeConfigs.put("unii", codeConfig("UNII-TID", "FDA UNII", "https://sms/codes/unii"));
        cfg.setCodeConfigs(codeConfigs);

        Map<String, Map<String, String>> substanceTypeConfigs = new HashMap<>();
        Map<String, String> chemical = new HashMap<>();
        chemical.put("SMS Term ID", "CHEM-TID");
        chemical.put("SMS URL", "https://sms/types/chemical");
        substanceTypeConfigs.put("chemical", chemical);
        cfg.setSubstanceTypeConfigs(substanceTypeConfigs);

        Map<String, Map<String, String>> miscDefaults = new HashMap<>();
        Map<String, String> nameLanguageCoding = new HashMap<>();
        nameLanguageCoding.put("system", "urn:ietf:bcp:47");
        miscDefaults.put("name_language_coding", nameLanguageCoding);

        Map<String, String> nameStatusCoding = new HashMap<>();
        nameStatusCoding.put("code", "official");
        nameStatusCoding.put("system", "https://sms/name-status");
        nameStatusCoding.put("display", "Official");
        miscDefaults.put("name_status_coding", nameStatusCoding);

        cfg.setMiscDefaultConfigs(miscDefaults);
        return cfg;
    }

    private static Map<String, String> codeConfig(String smsTermId, String gsrsCvTerm, String smsUrl) {
        Map<String, String> m = new HashMap<>();
        m.put("smsTermId", smsTermId);
        m.put("gsrsCvTerm", gsrsCvTerm);
        m.put("smsUrl", smsUrl);
        return m;
    }
}


