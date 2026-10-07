package gsrs.module.substance.services;

import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.repository.ProcessingRecordRepository;
import ix.core.processing.GinasSubstancePersisterFactory;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.ValidationUtils;
import ix.ginas.utils.validation.validators.StandardNameDuplicateValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkLoadRegressionEndToEndTest {

    @Mock
    private ProcessingRecordRepository processingRecordRepository;

    @Mock
    private SubstanceEntityService substanceEntityService;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private SubstanceLegacySearchService searchService;

    @Test
    void substanceBulkLoadServiceConfigurationShouldWireRequiredFactories() {
        SubstanceBulkLoadServiceConfiguration config = new SubstanceBulkLoadServiceConfiguration();
        GinasSubstancePersisterFactory persisterFactory = mock(GinasSubstancePersisterFactory.class);
        ReflectionTestUtils.setField(config, "persister", persisterFactory);

        assertNotNull(config.getRecordExtractorFactory());
        assertNotNull(config.getRecordTransformFactory());
        assertSame(persisterFactory, config.getRecordPersisterFactory());

        assertNotNull(config.ginasSubstancePersisterFactory());
        assertNotNull(config.substanceLobSchemaCompatibilityInitializer(dataSource));
        assertNotNull(config.substanceNameLookup(dataSource));
    }

    @Test
    void validationUtilsShouldSkipStaleSearchMatches() throws Exception {
        SearchResult searchResult = mock(SearchResult.class);
        List<Object> staleMatches = new AbstractList<Object>() {
            @Override
            public int size() {
                return 2;
            }

            @Override
            public Object get(int index) {
                if (index == 1) {
                    throw new RuntimeException(new NoSuchElementException("Entry number:0 could not be found in list of size 2"));
                }
                return new Substance();
            }
        };
        when(searchResult.getMatches()).thenReturn(staleMatches);

        Method method = ValidationUtils.class.getDeclaredMethod("safeMatches", SearchResult.class, Class.class, String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Substance> matches = (List<Substance>) method.invoke(null, searchResult, Substance.class, "root_names_stdName:\"test$\"");

        assertEquals(1, matches.size());
        assertNotNull(matches.get(0));
    }

    @Test
    void standardNameDuplicateValidatorShouldSkipStaleSearchResultRows() throws Exception {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        StandardNameDuplicateValidator validator = new StandardNameDuplicateValidator();
        ReflectionTestUtils.setField(validator, "transactionManager", transactionManager);
        ReflectionTestUtils.setField(validator, "searchService", searchService);

        SearchResult searchResult = mock(SearchResult.class);
        List<Substance> staleMatches = new AbstractList<Substance>() {
            @Override
            public int size() {
                return 2;
            }

            @Override
            public Substance get(int index) {
                if (index == 1) {
                    throw new RuntimeException(new NoSuchElementException("Entry number:0 could not be found in list of size 2"));
                }
                return new Substance();
            }
        };
        when(searchResult.getMatches()).thenReturn(staleMatches);
        when(searchService.search(anyString(), any(SearchOptions.class))).thenReturn(searchResult);

        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .simpleSearchOnly(true)
                .query("root_names_stdName:\"test$\"")
                .top(10)
                .build();

        Method method = StandardNameDuplicateValidator.class.getDeclaredMethod("getSearchList", SearchRequest.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Substance> matches = (List<Substance>) method.invoke(validator, request);

        assertEquals(1, matches.size());
        assertNotNull(matches.get(0));
    }
}
