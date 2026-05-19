package example.substance.validation;

import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.services.DefinitionalElementFactory;
import ix.core.validator.ValidationResponse;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.GinasChemicalStructure;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.DefHashCalcRequirements;
import ix.ginas.utils.validation.ValidationUtils;
import ix.ginas.utils.validation.validators.SaltValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Fast unit coverage for the salt-fragment warning logic without loading the full rep18 dataset.
 */
@Timeout(10)
public class SaltValidatorTest {

    @Test
    void warnsWhenNeitherFragmentHasAnyDatabaseMatch() {
        ValidationResponse<Substance> response = validateSalt(
                chemicalWithDefaultMoieties(),
                Collections.emptyList(),
                Collections.emptyList());

        assertTrue(hasMessage(response,
                "Each fragment should be present as a separate record in the database."));
    }

    @Test
    void warnsWhenFragmentsOnlyHaveLayer1Matches() {
        ValidationResponse<Substance> response = validateSalt(
                chemicalWithDefaultMoieties(),
                Collections.singletonList(new Substance()),
                Collections.emptyList());

        assertTrue(hasMessage(response,
                "This fragment is present as a separate record in the database but in a different form."));
    }

    @Test
    void staysQuietWhenEachFragmentHasAFullMatch() {
        ValidationResponse<Substance> response = validateSalt(
                chemicalWithDefaultMoieties(),
                Collections.singletonList(new Substance()),
                Collections.singletonList(new Substance()));

        assertTrue(response.getValidationMessages().stream().noneMatch(
                m -> m.getMessage().contains("Each fragment should be present as a separate record in the database.")));
        assertTrue(response.getValidationMessages().stream().noneMatch(
                m -> m.getMessage().contains("This fragment is present as a separate record in the database but in a different form.")));
    }

    private ValidationResponse<Substance> validateSalt(ChemicalSubstance chemical,
                                                       List<Substance> layer1Matches,
                                                       List<Substance> fullMatches) {
        SaltValidator validator = newValidator();
        try (MockedStatic<ValidationUtils> validationUtils = mockStatic(ValidationUtils.class)) {
            validationUtils.when(() -> ValidationUtils.findDefinitionaLayer1lDuplicateCandidates(
                            any(ChemicalSubstance.class), any(DefHashCalcRequirements.class)))
                    .thenReturn(layer1Matches);
            validationUtils.when(() -> ValidationUtils.findFullDefinitionalDuplicateCandidates(
                            any(ChemicalSubstance.class), any(DefHashCalcRequirements.class)))
                    .thenReturn(fullMatches);
            return validator.validate(chemical, null);
        }
    }

    private SaltValidator newValidator() {
        SaltValidator validator = new SaltValidator();
        ReflectionTestUtils.setField(validator, "definitionalElementFactory", mock(DefinitionalElementFactory.class));
        ReflectionTestUtils.setField(validator, "searchService", mock(SubstanceLegacySearchService.class));
        ReflectionTestUtils.setField(validator, "transactionManager", mock(PlatformTransactionManager.class));
        return validator;
    }

    private ChemicalSubstance chemicalWithDefaultMoieties() {
        ChemicalSubstance chemical = new ChemicalSubstance();
        chemical.moieties = new ArrayList<>();
        chemical.moieties.add(moiety("CC"));
        chemical.moieties.add(moiety("O"));
        return chemical;
    }

    private Moiety moiety(String smiles) {
        Moiety moiety = new Moiety();
        moiety.structure = new GinasChemicalStructure();
        moiety.structure.smiles = smiles;
        return moiety;
    }

    private boolean hasMessage(ValidationResponse<Substance> response, String text) {
        return response.getValidationMessages().stream().anyMatch(
                m -> m.getMessage().contains(text));
    }
}
