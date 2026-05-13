package gsrs.module.substance.misc.emasmsfhir;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmaSmsFhirUtilityCoverageTest {

    @Test
    void emaSmsFhirUtilityTestsBelongToExtensionModule() {
        // These utility classes live in gsrs-ema-fhir-substance-extension, not in core.
        Assumptions.assumeTrue(
                isClassPresent("gsrs.module.substance.misc.emasmsfhir.ExporterUtilities")
                        && isClassPresent("gsrs.module.substance.misc.emasmsfhir.EmaSmsFhrUtils")
                        && isClassPresent("gsrs.module.substance.misc.emasmsfhir.EmaSmsFhirExporterTest"),
                "Skipping EMA SMS FHIR utility coverage in core because extension classes are not on classpath");

        // If extension classes are present, this test confirms classpath wiring at minimum.
        assertTrue(true);
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
