package ix.ginas.utils.validation.validators;

import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.validator.ValidatorCallback;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import gov.nih.ncats.molwitch.Chemical;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for molecular-formula validation behavior in ChemicalValidator.
 */
public class ChemicalValidatorMolecularFormulaTest {

    private ValidatorCallback mockCallback;
    private StructureProcessor mockStructureProcessor;

    private static final String VALID_METHANE_MOLFILE =
            "\n  Mrv2341 01012100002D\n\n  1  0  0  0  0  0  0  0  0  0  0 V2000\n" +
                    "    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "M  END\n";

    private static final String VALID_ETHANE_MOLFILE =
            "\n  Mrv2341 01012100002D\n\n  2  1  0  0  0  0  0  0  0  0  0 V2000\n" +
                    "    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "    1.5000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  1  2  1  0  0  0  0\n" +
                    "M  END\n";

    @BeforeEach
    public void setUp() {
        mockCallback = mock(ValidatorCallback.class);
        mockStructureProcessor = mock(StructureProcessor.class);

        when(mockStructureProcessor.instrument(anyString(), any(), anyBoolean()))
                .thenAnswer(invocation -> {
                    String molfile = invocation.getArgument(0);
                    List<Structure> moietiesList = invocation.getArgument(1);

                    // Compute the real formula from the molfile so tests see realistic values
                    String computedFormula = "CH4"; // safe default
                    try {
                        Chemical chem = Chemical.parseMol(molfile);
                        String f = chem.getFormula();
                        if (f != null && !f.isEmpty()) {
                            computedFormula = f;
                        }
                    } catch (Exception ignored) {
                        // invalid molfile – keep the default
                    }

                    GinasChemicalStructure structure = new GinasChemicalStructure();
                    structure.molfile = molfile;
                    structure.smiles = "C";
                    structure.formula = computedFormula;
                    structure.charge = 0;
                    structure.stereoCenters = 0;
                    structure.definedStereo = 0;
                    structure.ezCenters = 0;
                    structure.mwt = 16.0;
                    structure.stereoChemistry = Structure.Stereo.ACHIRAL;
                    structure.opticalActivity = Structure.Optical.NONE;

                    // Populate the moieties list the way the real StructureProcessor would
                    if (moietiesList != null) {
                        GinasChemicalStructure moiety = new GinasChemicalStructure();
                        moiety.molfile = molfile;
                        moiety.smiles = "C";
                        moiety.formula = computedFormula;
                        moiety.charge = 0;
                        moiety.stereoCenters = 0;
                        moiety.definedStereo = 0;
                        moiety.ezCenters = 0;
                        moiety.mwt = 16.0;
                        moiety.stereoChemistry = Structure.Stereo.ACHIRAL;
                        moiety.opticalActivity = Structure.Optical.NONE;
                        moietiesList.add(moiety);
                    }

                    return structure;
                });
    }

    private ChemicalSubstance createTestSubstance(String molfile, String formula) {
        ChemicalSubstance substance = new ChemicalSubstance();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.molfile = molfile;
        structure.formula = formula;
        structure.smiles = "C";
        substance.setStructure(structure);
        return substance;
    }

    @Test
    @DisplayName("Flag: allowFormulaToBeDifferent=true skips validation")
    public void testSkipValidationWhenFlagIsTrue() {
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(mockStructureProcessor);
        validator.setAllowFormulaToBeDifferent(true);
        validator.setAllow0AtomStructures(true);

        ChemicalSubstance substance = createTestSubstance(VALID_METHANE_MOLFILE, "C2H6");

        validator.validate(substance, null, mockCallback);

        verify(mockCallback, never()).addMessage(org.mockito.ArgumentMatchers.argThat(msg ->
                msg != null
                        && msg.getMessage() != null
                        && msg.getMessage().contains("autogenerated from the structure")
        ));
    }

    @Test
    @DisplayName("No error when formula matches parsed molfile")
    public void testNoErrorWhenFormulaMatches() {
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(mockStructureProcessor);
        validator.setAllowFormulaToBeDifferent(false);
        validator.setAllow0AtomStructures(true);

        ChemicalSubstance substance = createTestSubstance(VALID_METHANE_MOLFILE, "CH4");

        validator.validate(substance, null, mockCallback);

        verify(mockCallback, never()).addMessage(org.mockito.ArgumentMatchers.argThat(msg ->
                msg != null
                        && msg.getMessage() != null
                        && msg.getMessage().contains("autogenerated from the structure")
        ));
    }

    @Test
    @DisplayName("Error raised when formula does not match molfile")
    public void testErrorWhenFormulaMismatch() {
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(mockStructureProcessor);
        validator.setAllowFormulaToBeDifferent(false);
        validator.setAllow0AtomStructures(true);

        ChemicalSubstance substance = createTestSubstance(VALID_METHANE_MOLFILE, "C2H6");

        validator.validate(substance, null, mockCallback);

        verify(mockCallback, atLeastOnce()).addMessage(org.mockito.ArgumentMatchers.argThat(msg ->
                msg != null
                        && msg.getMessage() != null
                        && msg.getMessage().contains("autogenerated from the structure")
                        && msg.getMessage().contains("Expected formula")
        ));
    }

    @Test
    @DisplayName("Error message includes expected formula")
    public void testErrorMessageIncludesExpectedFormula() {
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(mockStructureProcessor);
        validator.setAllowFormulaToBeDifferent(false);
        validator.setAllow0AtomStructures(true);

        ChemicalSubstance substance = createTestSubstance(VALID_METHANE_MOLFILE, "WRONG");

        validator.validate(substance, null, mockCallback);

        verify(mockCallback, atLeastOnce()).addMessage(org.mockito.ArgumentMatchers.argThat(msg ->
                msg != null
                        && msg.getMessage() != null
                        && msg.getMessage().contains("Expected formula")
        ));
    }

    @Test
    @DisplayName("Null formula is skipped (no error)")
    public void testNullFormulaSkipsValidation() {
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(mockStructureProcessor);
        validator.setAllowFormulaToBeDifferent(false);
        validator.setAllow0AtomStructures(true);

        ChemicalSubstance substance = createTestSubstance(VALID_METHANE_MOLFILE, null);

        validator.validate(substance, null, mockCallback);

        verify(mockCallback, never()).addMessage(org.mockito.ArgumentMatchers.argThat(msg ->
                msg != null
                        && msg.getMessage() != null
                        && msg.getMessage().contains("autogenerated from the structure")
        ));
    }

    @Test
    @DisplayName("Empty formula string triggers validation")
    public void testEmptyFormulaStringIsValidated() {
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(mockStructureProcessor);
        validator.setAllowFormulaToBeDifferent(false);
        validator.setAllow0AtomStructures(true);

        ChemicalSubstance substance = createTestSubstance(VALID_METHANE_MOLFILE, "");

        validator.validate(substance, null, mockCallback);

        verify(mockCallback, atLeastOnce()).addMessage(org.mockito.ArgumentMatchers.argThat(msg ->
                msg != null
                        && msg.getMessage() != null
                        && msg.getMessage().contains("autogenerated from the structure")
        ));
    }

    @Test
    @DisplayName("Formula comparison is case-sensitive")
    public void testFormulaComparisonIsCaseSensitive() {
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(mockStructureProcessor);
        validator.setAllowFormulaToBeDifferent(false);
        validator.setAllow0AtomStructures(true);

        ChemicalSubstance substance = createTestSubstance(VALID_METHANE_MOLFILE, "ch4");

        validator.validate(substance, null, mockCallback);

        verify(mockCallback, atLeastOnce()).addMessage(org.mockito.ArgumentMatchers.argThat(msg ->
                msg != null
                        && msg.getMessage() != null
                        && msg.getMessage().contains("autogenerated from the structure")
        ));
    }

    @Test
    @DisplayName("Invalid molfile does not crash validation")
    public void testInvalidMolfileThrowsRuntimeException() {
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(mockStructureProcessor);
        validator.setAllowFormulaToBeDifferent(false);
        validator.setAllow0AtomStructures(true);

        ChemicalSubstance substance = createTestSubstance("INVALID MOLFILE", "CH4");

        assertDoesNotThrow(() -> validator.validate(substance, null, mockCallback));
    }

    @Test
    @DisplayName("Detects mismatch with different molecules")
    public void testFormulaMismatchWithEthane() {
        ChemicalValidator validator = new ChemicalValidator();
        validator.setStructureProcessor(mockStructureProcessor);
        validator.setAllowFormulaToBeDifferent(false);
        validator.setAllow0AtomStructures(true);

        ChemicalSubstance substance = createTestSubstance(VALID_ETHANE_MOLFILE, "CH4");

        validator.validate(substance, null, mockCallback);

        verify(mockCallback, atLeastOnce()).addMessage(org.mockito.ArgumentMatchers.argThat(msg ->
                msg != null
                        && msg.getMessage() != null
                        && msg.getMessage().contains("autogenerated from the structure")
        ));
    }
}
