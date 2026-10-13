package example.substance.validation;

import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.definitional.ChemicalSubstanceDefinitionalElementImpl;
import gsrs.module.substance.definitional.DefinitionalElement;
import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.services.DefinitionalElementFactory;
import ix.core.chem.InchiStandardizer;
import ix.core.chem.InchiStructureHasher;
import ix.core.chem.StructureProcessor;
import ix.core.models.Structure;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResult;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.*;
import ix.ginas.utils.validation.DefHashCalcRequirements;
import ix.ginas.utils.validation.ValidationUtils;
import ix.ginas.utils.validation.validators.SaltValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Test
    void existenceQueryExcludesTargetAndRequestsOneResult() throws Exception {
        UUID targetUuid = UUID.randomUUID();

        ChemicalSubstanceBuilder chemicalSubstanceBuilder = new ChemicalSubstanceBuilder();
        GinasChemicalStructure structure = new GinasChemicalStructure();
        structure.smiles ="OC(=O)c1ccccc1";
        structure.count = 1;
        structure.stereoChemistry = Structure.Stereo.ACHIRAL;
        structure.opticalActivity = Structure.Optical.NONE;

        chemicalSubstanceBuilder.setStructure(structure);
        Moiety chemMoiety = new Moiety();
        chemMoiety.structure = structure;
        Amount moietyAmount = new Amount();
        moietyAmount.average = 1.0;
        chemMoiety.setCountAmount(moietyAmount);
        chemicalSubstanceBuilder.addMoiety(chemMoiety);
        chemicalSubstanceBuilder.addName("benzoic acid");
        ChemicalSubstance target = chemicalSubstanceBuilder.build();

        target.uuid = targetUuid;

        DefinitionalElementFactory elementFactory =
                mock(DefinitionalElementFactory.class);
        SubstanceLegacySearchService searchService =
                mock(SubstanceLegacySearchService.class);
        PlatformTransactionManager transactionManager =
                mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus =
                 mock(TransactionStatus.class);
        SearchResult searchResult =
                mock(SearchResult.class);

        StructureProcessor structureProcessor =
                new StructureProcessor(
                        new InchiStandardizer(),
                        new InchiStructureHasher());

        ChemicalSubstanceDefinitionalElementImpl calculator =
                new ChemicalSubstanceDefinitionalElementImpl();

        ReflectionTestUtils.setField(
                calculator,
                "structureProcessor",
                structureProcessor);
        List<DefinitionalElement> elements = new ArrayList<>();
        calculator.computeDefinitionalElements(target, elements::add);
        DefinitionalElements expectedElements = new DefinitionalElements(elements);

        String expectedHash =
                expectedElements.getDefinitionalHashLayers().get(0);

        when(elementFactory.computeDefinitionalElementsFor(target))
                .thenReturn(expectedElements);

        when(transactionManager.getTransaction(
                any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);

        when(searchService.search(
                anyString(),
                any(SearchOptions.class)))
                .thenReturn(searchResult);

        when(searchResult.getMatches())
                .thenReturn(Collections.emptyList());

        DefHashCalcRequirements requirements =
                new DefHashCalcRequirements(
                        elementFactory,
                        searchService,
                        transactionManager);

        boolean found =
                ValidationUtils.hasLayerNDefinitionalDuplicateCandidate(
                        target, 0, requirements);

        assertFalse(found);

        ArgumentCaptor<String> queryCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SearchOptions> optionsCaptor =
                ArgumentCaptor.forClass(SearchOptions.class);

        verify(searchService).search(
                queryCaptor.capture(),
                optionsCaptor.capture());

        assertEquals(
                "root_definitional_hash_layer_1:" + expectedHash
                        + " AND NOT root_uuid:\"^" + targetUuid + "$\"",
                queryCaptor.getValue());

        assertEquals(1, optionsCaptor.getValue().getTop());
    }


    private ValidationResponse<Substance> validateSalt(ChemicalSubstance chemical,
                                                       List<Substance> layer1Matches,
                                                       List<Substance> fullMatches) {
        SaltValidator validator = newValidator();
        try (MockedStatic<ValidationUtils> validationUtils = mockStatic(ValidationUtils.class)) {
            validationUtils.when(()->ValidationUtils.hasFullDefinitionalDuplicateCandidate(
                            any(Substance.class), any(DefHashCalcRequirements.class)))
                    .thenReturn(!fullMatches.isEmpty());
            validationUtils.when(()->ValidationUtils.hasLayer1DefinitionalDuplicateCandidate(
                    any(Substance.class), any(DefHashCalcRequirements.class)))
                    .thenReturn(!layer1Matches.isEmpty());
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
