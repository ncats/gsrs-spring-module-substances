package example.substance.datasearch;

import example.TestUtil;
import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.util.EntityUtils;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author mitch
 */
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class NumericRangeSearchTest extends AbstractSubstanceJpaFullStackEntityTest {

    public NumericRangeSearchTest() {
    }

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    private DefinitionalElementFactory definitionalElementFactory;

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    StructureProcessor structureProcessor;

    @Autowired
    private TestGsrsValidatorFactory factory;

    private String fileName = "rep18.gsrs";

    @BeforeEach
    public void clearIndexers() throws IOException {
        SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
        AutowireHelper.getInstance().autowire(hashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
        {
            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);
        }

        File dataFile = new ClassPathResource(fileName).getFile();
        loadGsrsFile(dataFile);
    }

    @Test
    public void testSearchByDateRange() {

        log.trace("starting in testSearchByDateRange");
        String start = "1620121913352";
        String end = "1620121913354";

        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_created:%5B" + start + " TO " + end + "%5D")
                .top(Integer.MAX_VALUE)
                .build();

        log.trace("query: " + request.getQuery());
        List<Substance> substances = getSearchList(request);

        log.trace("substances size: " + substances.size());
        log.trace("matches: ");
        substances.forEach(s-> log.debug("substance " + s.uuid + "; created: " + s.created.getTime()));
        List<String> substanceIds= substances.stream()
                .map(s -> s.getUuid().toString())
                .peek(id -> log.trace(id))
                .collect(Collectors.toList());
        
        String expectedId = "ac776f92-b90f-48a0-a54e-7461a60c84b3";
        Assertions.assertTrue(substanceIds.contains(expectedId));
    }

    @Test
    public void testSearchByMwtRange() {

        log.trace("starting in testSearchByMwtRange");
        String start = "354.0";
        String end = "356.0";

        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_structure_mwt:%5B" + start + " TO " + end + "%5D")
                .top(Integer.MAX_VALUE)
                .build();

        log.trace("query: " + request.getQuery());
        List<Substance> substances = getSearchList(request);

        log.trace("substances size: " + substances.size());
        List<String> substanceIds = substances.stream()
                .map(s -> s.getUuid().toString())
                .peek(id -> log.trace(id))
                .collect(Collectors.toList());
        String expectedId = "ac776f92-b90f-48a0-a54e-7461a60c84b3";
        Assertions.assertTrue(substanceIds.contains(expectedId));
    }

    /**
     * Return a list of substances based on the {@link SearchRequest}. This
     * takes care of some tricky transaction issues.
     *
     * @param sr
     * @return
     */
    private List<Substance> getSearchList(SearchRequest sr) {
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        List<Substance> substances = transactionSearch.execute(ts -> {
            try {
                SearchResult sresult = searchService.search(sr.getQuery(), sr.getOptions());
                List<Substance> first = sresult.getMatches();
                return first.stream()
                        //force fetching
                        .peek(ss -> EntityUtils.EntityWrapper.of(ss).toInternalJson())
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new RuntimeException(e);

            }
        });
        return substances;
    }

}
