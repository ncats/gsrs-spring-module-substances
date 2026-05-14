package gsrs.module.substance.misc.emasmsfhir;

import ix.ginas.models.v1.Substance;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.SubstanceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmaSmsSubstanceDefinitionFhirMapper Tests")
class EmaSmsSubstanceDefinitionFhirMapperTest {

    private EmaSmsSubstanceDefinitionFhirMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EmaSmsSubstanceDefinitionFhirMapper();
        mapper.setEmaSmsFhirConfiguration(EmaSmsFhirTestData.configuration());
    }

    @Test
    @DisplayName("Maps classification, name, and all configured codes")
    void mapsClassificationNameAndCodes() {
        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Sodium Chloride");
        substance.codes.add(EmaSmsFhirTestData.code("ECHA (EC/EINECS)", "231-959-4", "PRIMARY"));
        substance.codes.add(EmaSmsFhirTestData.code("EVMPD", "EV123", "PRIMARY"));
        substance.codes.add(EmaSmsFhirTestData.code("INN", "INN-123", "PRIMARY"));
        substance.codes.add(EmaSmsFhirTestData.code("SMS ID", "SMS-987", "PRIMARY"));
        substance.codes.add(EmaSmsFhirTestData.code("FDA UNII", "451W47IQ8X", "PRIMARY"));

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
        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("No Codes");

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertNotNull(sd);
        assertTrue(sd.getCode().isEmpty());
    }

    @Test
    @DisplayName("Skips code entries when key is not configured")
    void skipsWhenCodeKeyIsMissingFromConfiguration() {
        EmaSmsFhirConfiguration cfg = EmaSmsFhirTestData.configuration();
        cfg.getCodeConfigs().remove("evCode");
        mapper.setEmaSmsFhirConfiguration(cfg);

        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Partial Config");
        substance.codes.add(EmaSmsFhirTestData.code("ECHA (EC/EINECS)", "231-959-4", "PRIMARY"));
        substance.codes.add(EmaSmsFhirTestData.code("EVMPD", "EV123", "PRIMARY"));

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertEquals(1, sd.getCode().size());
        assertEquals("EC-TID", sd.getCode().get(0).getCode().getCodingFirstRep().getCode());
    }

    @Test
    @DisplayName("Uses empty code/system values when configuration values are null")
    void usesEmptyFallbackForNullCodeAndSystem() {
        EmaSmsFhirConfiguration cfg = EmaSmsFhirTestData.configuration();
        cfg.getCodeConfigs().get("evCode").put("smsTermId", null);
        cfg.getCodeConfigs().get("evCode").put("smsUrl", null);
        mapper.setEmaSmsFhirConfiguration(cfg);

        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Null Config Values");
        substance.codes.add(EmaSmsFhirTestData.code("EVMPD", "EV123", "PRIMARY"));

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
        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Extension Test");

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertEquals(1, sd.getExtension().size());
        Extension ext = sd.getExtension().get(0);
        assertEquals("https://gsrs.ncats.nih.gov/api/v1/substances", ext.getUrl());
        assertTrue(ext.getValue().primitiveValue().contains("\"substanceClass\""));
    }

    @Test
    @DisplayName("Handles no display name by omitting name section")
    void omitsNameWhenDisplayNameMissing() {
        Substance substance = EmaSmsFhirTestData.chemicalSubstance();

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertTrue(sd.getName().isEmpty());
        assertEquals(1, sd.getClassification().size());
    }

    @Test
    @DisplayName("Uses bracketed display for non-primary code types")
    void usesBracketedDisplayForNonPrimaryCodeTypes() {
        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("Non Primary");
        substance.codes.add(EmaSmsFhirTestData.code("INN", "INN-XYZ", "SECONDARY"));

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertEquals(1, sd.getCode().size());
        assertEquals("INN-TID", sd.getCode().get(0).getCode().getCodingFirstRep().getCode());
        assertEquals("INN-XYZ [SECONDARY]", sd.getCode().get(0).getCode().getCodingFirstRep().getDisplay());
    }

    @Test
    @DisplayName("Does not add extension when toggle is disabled")
    void doesNotAddExtensionWhenDisabled() {
        mapper.setIncludeGsrsSubstanceExtension(false);
        Substance substance = EmaSmsFhirTestData.chemicalSubstanceWithDisplayName("No Extension");

        SubstanceDefinition sd = mapper.generateEmaSmsSubstanceDefinitionFromSubstance(substance);

        assertTrue(sd.getExtension().isEmpty());
    }

    @Test
    @DisplayName("Configuration Lombok methods are exercised")
    void configurationLombokMethodsAreExercised() {
        EmaSmsFhirConfiguration a = EmaSmsFhirTestData.configuration();
        EmaSmsFhirConfiguration b = EmaSmsFhirTestData.configuration();

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

        EmaSmsFhirConfiguration cfg = EmaSmsFhirTestData.configuration();
        a.setEmaSmsFhirConfiguration(cfg);
        b.setEmaSmsFhirConfiguration(EmaSmsFhirTestData.configuration());
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
}


