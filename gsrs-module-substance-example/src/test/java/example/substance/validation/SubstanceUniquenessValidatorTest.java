package example.substance.validation;

import example.substance.support.TestTransactionManagers;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.definitional.DefinitionalElement;
import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.services.DefinitionalElementFactory;
import ix.core.search.SearchOptions;
import ix.core.search.SearchResult;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.validators.SubstanceUniquenessValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubstanceUniquenessValidatorTest {

    @Mock
    private DefinitionalElementFactory definitionalElementFactory;

    @Mock
    private SubstanceLegacySearchService searchService;

    private PlatformTransactionManager transactionManager;

    private SubstanceUniquenessValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SubstanceUniquenessValidator();
        transactionManager = TestTransactionManagers.mockTransactionManager();

        ReflectionTestUtils.setField(validator, "definitionalElementFactory", definitionalElementFactory);
        ReflectionTestUtils.setField(validator, "searchService", searchService);
        ReflectionTestUtils.setField(validator, "transactionManager", transactionManager);
    }

    @ParameterizedTest
    @EnumSource(value = Substance.SubstanceClass.class, names = {
            "protein",
            "nucleicAcid",
            "specifiedSubstanceG2",
            "specifiedSubstanceG3",
            "specifiedSubstanceG4",
            "unspecifiedSubstance",
            "reference"
    })
    @DisplayName("Skips unsupported substance classes")
    void skipsUnsupportedClass(Substance.SubstanceClass substanceClass) {
        Substance testSubstance = createSubstance("Unsupported", substanceClass);

        ValidationResponse<Substance> response = validator.validate(testSubstance, null);

        assertTrue(response.getValidationMessages().isEmpty());
        verifyNoInteractions(definitionalElementFactory, searchService);
    }

    @Test
    @DisplayName("Skips validation when dependencies have not been injected")
    void skipsWhenDefinitionalElementFactoryIsMissing() {
        SubstanceUniquenessValidator unconfiguredValidator = new SubstanceUniquenessValidator();

        ValidationResponse<Substance> response = unconfiguredValidator.validate(
                createSubstance("No Dependency", Substance.SubstanceClass.chemical), null);

        assertTrue(response.getValidationMessages().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = Substance.SubstanceClass.class, names = {
            "chemical",
            "mixture",
            "structurallyDiverse",
            "polymer",
            "concept",
            "specifiedSubstanceG1"
    })
    @DisplayName("Skips validation when definitional elements are empty")
    void skipsWhenNoDefinitionalElements(Substance.SubstanceClass substanceClass) {
        Substance testSubstance = createSubstance("No Def Elements", substanceClass);
        when(definitionalElementFactory.computeDefinitionalElementsFor(testSubstance))
                .thenReturn(new DefinitionalElements(Collections.emptyList()));

        ValidationResponse<Substance> response = validator.validate(testSubstance, null);

        assertTrue(response.getValidationMessages().isEmpty());
    }

    @Test
    @DisplayName("Reports full duplicate candidates as errors")
    void reportsFullDuplicates() throws Exception {
        Substance testSubstance = createSubstance("Test", Substance.SubstanceClass.chemical);
        when(definitionalElementFactory.computeDefinitionalElementsFor(testSubstance))
                .thenReturn(defElementsWithTwoLayers());

        Substance duplicate = mockSubstanceMatch("Known Duplicate");
        SearchResult fullSearchResult = searchResultWith(duplicate, "non-substance-hit");
        SearchResult emptySearchResult = searchResultWith();

        when(searchService.search(anyString(), any(SearchOptions.class)))
                .thenAnswer(invocation -> {
                    String query = invocation.getArgument(0, String.class);
                    if (query.contains("root_definitional_hash_layer_2")) {
                        return fullSearchResult;
                    }
                    return emptySearchResult;
                });

        ValidationResponse<Substance> response = validator.validate(testSubstance, null);

        assertEquals(1, response.getValidationMessages().size());
        String message = ((GinasProcessingMessage) response.getValidationMessages().get(0)).getMessage();
        assertTrue(message.contains("appears to be a full duplicate"));
    }

    @Test
    @DisplayName("Reports layer-1 candidates as warnings when no full duplicate exists")
    void reportsPossibleDuplicatesWhenOnlyLayerOneMatches() throws Exception {
        Substance testSubstance = createSubstance("Test", Substance.SubstanceClass.chemical);
        when(definitionalElementFactory.computeDefinitionalElementsFor(testSubstance))
                .thenReturn(defElementsWithTwoLayers());

        Substance possibleDuplicate = mockSubstanceMatch("Possible Duplicate");
        SearchResult emptySearchResult = searchResultWith();
        SearchResult possibleSearchResult = searchResultWith(possibleDuplicate);

        when(searchService.search(anyString(), any(SearchOptions.class)))
                .thenAnswer(invocation -> {
                    String query = invocation.getArgument(0, String.class);
                    if (query.contains("root_definitional_hash_layer_2")) {
                        return emptySearchResult;
                    }
                    if (query.contains("root_definitional_hash_layer_1")) {
                        return possibleSearchResult;
                    }
                    return emptySearchResult;
                });

        ValidationResponse<Substance> response = validator.validate(testSubstance, null);

        assertEquals(1, response.getValidationMessages().size());
        String message = ((GinasProcessingMessage) response.getValidationMessages().get(0)).getMessage();
        assertTrue(message.contains("is a possible duplicate"));
    }

    @Test
    @DisplayName("Does not report duplicate when search only returns the same substance")
    void doesNotReportItselfAsDuplicate() throws Exception {
        Substance testSubstance = createSubstance("Self", Substance.SubstanceClass.chemical);
        when(definitionalElementFactory.computeDefinitionalElementsFor(testSubstance))
                .thenReturn(defElementsWithTwoLayers());

        SearchResult selfOnlyResult = searchResultWith(testSubstance);
        when(searchService.search(anyString(), any(SearchOptions.class))).thenReturn(selfOnlyResult);

        ValidationResponse<Substance> response = validator.validate(testSubstance, null);

        assertTrue(response.getValidationMessages().isEmpty());
    }

    @Test
    @DisplayName("Search failures are contained and do not create false duplicate messages")
    void searchFailureDoesNotCreateDuplicateMessages() throws Exception {
        Substance testSubstance = createSubstance("Search Failure", Substance.SubstanceClass.chemical);
        when(definitionalElementFactory.computeDefinitionalElementsFor(testSubstance))
                .thenReturn(defElementsWithTwoLayers());
        when(searchService.search(anyString(), any(SearchOptions.class)))
                .thenThrow(new IllegalStateException("index unavailable"));

        ValidationResponse<Substance> response = validateSilencingSystemErr(validator, testSubstance);

        assertTrue(response.getValidationMessages().isEmpty());
    }

    private static Substance createSubstance(String name, Substance.SubstanceClass cls) {
        Substance s = new SubstanceBuilder()
                .asChemical()
                .setUUID(UUID.randomUUID())
                .addName(name, n -> {
                    n.name = name;
                    n.stdName = name;
                    n.displayName = true;
                })
                .build();
        s.substanceClass = cls;
        return s;
    }

    private static DefinitionalElements defElementsWithTwoLayers() {
        return new DefinitionalElements(Arrays.asList(
                DefinitionalElement.of("k1", "v1", 1),
                DefinitionalElement.of("k2", "v2", 2)
        ));
    }

    private static SearchResult searchResultWith(Object... matches) throws Exception {
        SearchResult result = mock(SearchResult.class);
        doNothing().when(result).waitForFinish();
        when(result.getMatches()).thenReturn(Arrays.asList(matches));
        return result;
    }

    private static ValidationResponse<Substance> validateSilencingSystemErr(SubstanceUniquenessValidator validator,
                                                                            Substance substance) {
        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            return validator.validate(substance, null);
        } finally {
            System.setErr(originalErr);
        }
    }

    private static Substance mockSubstanceMatch(String name) {
        Substance match = mock(Substance.class);
        SubstanceReference reference = new SubstanceReference();
        reference.refuuid = UUID.randomUUID().toString();
        reference.refPname = name;

        when(match.getName()).thenReturn(name);
        when(match.asSubstanceReference()).thenReturn(reference);
        when(match.getOrGenerateUUID()).thenReturn(UUID.randomUUID());
        return match;
    }
}
