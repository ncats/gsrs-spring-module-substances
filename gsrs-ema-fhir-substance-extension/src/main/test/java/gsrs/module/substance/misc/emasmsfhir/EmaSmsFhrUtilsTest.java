package gsrs.module.substance.misc.emasmsfhir;

import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmaSmsFhrUtilsTest {

    @Test
    void findCodeByCodeSystem_handlesPrimarySecondaryAndMissing() {
        Substance substance = new SubstanceBuilder().asChemical().generateNewUUID().build();

        Code primary = new Code("EVMPD", "EV-001");
        primary.type = "PRIMARY";
        Code secondary = new Code("INN", "INN-001");
        secondary.type = "SECONDARY";

        substance.codes.add(primary);
        substance.codes.add(secondary);

        assertEquals("EV-001", EmaSmsFhrUtils.findCodeByCodeSystem("EVMPD", substance));
        assertEquals("INN-001 [SECONDARY]", EmaSmsFhrUtils.findCodeByCodeSystem("INN", substance));
        assertEquals("", EmaSmsFhrUtils.findCodeByCodeSystem("UNKNOWN", substance));
    }

    @Test
    void gsrsSubstanceToQuotedJson_serializesSubstance() {
        Substance substance = new SubstanceBuilder().asChemical().generateNewUUID().build();
        String payload = EmaSmsFhrUtils.gsrsSubstanceToQuotedJson(substance);

        assertTrue(payload.contains("substanceClass"));
    }

    @Test
    void getEmaSmsSubstanceTypeFromGsrsSubstanceClass_returnsKnownOrUndefined() {
        assertEquals("chemical", EmaSmsFhrUtils.getEmaSmsSubstanceTypeFromGsrsSubstanceClass("chemical"));
        assertEquals("UNDEFINED", EmaSmsFhrUtils.getEmaSmsSubstanceTypeFromGsrsSubstanceClass("not-mapped"));
    }
}

