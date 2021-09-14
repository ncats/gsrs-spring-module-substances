package example.loadertests;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.models.FV;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
@Slf4j
@WithMockUser(username = "admin", roles = "Admin")
public class LoaderFacetTest extends AbstractSubstanceJpaFullStackEntityTest {

    public LoaderFacetTest() {
    }

    private final String fileName = "rep18.gsrs";

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @BeforeEach
    public void clearIndexers() throws IOException {
        TestableFacetIndexValueMaker indexer = new TestableFacetIndexValueMaker();
        AutowireHelper.getInstance().autowire(indexer);
        testIndexValueMakerFactory.addIndexValueMaker(indexer);
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
    public void testSearchByCodeSystemFacets() {
        String codeSystem1 = "DRUG CENTRAL";
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(10)
                .query("root_codes_codeSystem:\"" + codeSystem1 + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        List< String> facetNames = getSearchFacetNames(request);

        assertTrue(facetNames.contains(TestableFacetIndexValueMaker.LETTER_DIGIT_RATIO_NAME),
                "Facets returned from search must contain configured facet");
        assertTrue(facetNames.contains(codeSystem1),
                "Facets returned from search must contain configured facet");
    }

    @Test
    public void testSearchByCodeSystemFacetValues() {
        String codeSystem1 = "DRUG CENTRAL";
        String expectedSubstanceClass = "chemical";
        int expectedChemicalCount = 5;
        int expectedProteinCount =1;
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(10)
                .query("root_codes_codeSystem:\"" + codeSystem1 + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        List<List<FV>> values = getSearchFacetValues(request, TestableFacetIndexValueMaker.LETTER_DIGIT_RATIO_NAME);
        final AtomicInteger listNum = new AtomicInteger(0);
        values.forEach(vList -> {
            log.trace("values for " + listNum.incrementAndGet());
            vList.forEach(i -> log.trace(i.getLabel() + ":" + i.getCount()));
        });

        List<List<FV>> stringValues = getSearchFacetValues(request, "Substance Class");
        listNum.set(0);
        stringValues.forEach(vList -> {
            log.trace("(testSearchByCodeSystemFacetValues) values for " + listNum.incrementAndGet());
            vList.forEach(i -> log.trace(i.getLabel() + ":" + i.getCount()));
        });
        assertTrue(values.size() > 0, "Must return at least one facet");

        //add up all facets with the required value
        AtomicInteger actualChemicalCount = new AtomicInteger(0);
        AtomicInteger actualProteinCount = new AtomicInteger(0);
        stringValues.forEach(p -> {
            p.forEach(p2 -> {
                if (p2.getLabel().equals(expectedSubstanceClass)) {
                    actualChemicalCount.addAndGet(p2.getCount());
                } else if(p2.getLabel().equals("protein")) {
                    actualProteinCount.addAndGet(p2.getCount());
                }
            });
        });
        Assertions.assertEquals(expectedChemicalCount, actualChemicalCount.get(), "Counts of chemical substance type must agree");
        Assertions.assertEquals(expectedProteinCount, actualProteinCount.get(), "Counts of protein substance type must agree");
    }

    @Test
    public void testSearchByNameFacetValues() {
        String nameToSearch = "PLASMALYTE A";
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(10)
                .query("root_names_name:\"" + nameToSearch + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        final AtomicInteger listNum = new AtomicInteger(0);

        List<List<FV>> stringValues = getSearchFacetValues(request, "Code System");
        listNum.set(0);
        stringValues.forEach(vList -> {
            log.trace("values for " + listNum.incrementAndGet());
            vList.forEach(i -> log.trace(i.getLabel() + ":" + i.getCount()));
        });

        List<String> expectedCodeSystems = Arrays.asList("CAS", "FDA UNII");
        assertTrue(expectedCodeSystems.stream()
                .allMatch(codeSystem -> stringValues.stream()
                .anyMatch(p -> p.stream()
                .anyMatch(p2 -> p2.getLabel().equals(codeSystem)))), 
                "Results must contain each expected code system");
    }

    @Test
    public void testSearchByStereoFacetValues() {
        /*
        search for structure field and verify that all results have the class facet with the expected value
         */
        String stereochemistry = "ACHIRAL";
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(10)
                .query("root_structure_stereoChemistry:\"" + stereochemistry + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        final AtomicInteger listNum = new AtomicInteger(0);

        List<List<FV>> stringValues = getSearchFacetValues(request, "Substance Class");
        listNum.set(0);
        stringValues.forEach(vList -> {
            log.trace("values for " + listNum.incrementAndGet());
            vList.forEach(i -> log.trace(i.getLabel() + ":" + i.getCount()));
        });

        Set<String> expectedValues = new HashSet<>();
        expectedValues.add("chemical");

        Set<String> uniqueClasses = new HashSet<>();

        stringValues.forEach(sv -> {
            uniqueClasses.addAll(sv.stream().map(s -> s.getLabel()).collect(Collectors.toSet()));
        });
        assertEquals(expectedValues, uniqueClasses,
                "Search on chemicals must produce hits that have the same value for the 'Substance Class' facet.");
    }

    @Test
    public void testSearchBySourceMaterialFacetValues() {
        /*
        Search for source material type == 'VIRUS' and retrieve the corresponding facet.
        Make sure the values are the same
         */
        String strDivSourceMatType = "VIRUS";
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(10)
                .query("root_structurallyDiverse_sourceMaterialType:\"" + strDivSourceMatType + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        final AtomicInteger listNum = new AtomicInteger(0);

        List<List<FV>> stringValues = getSearchFacetValues(request, "Material Type");
        listNum.set(0);
        stringValues.forEach(vList -> {
            log.trace("values for " + listNum.incrementAndGet());
            vList.forEach(i -> log.trace(i.getLabel() + ":" + i.getCount()));
        });

        List<String> expectedValues = Arrays.asList(strDivSourceMatType);
        List<String> actualValues = new ArrayList<>();
        
        stringValues.forEach(sv-> {
            sv.forEach(s-> actualValues.add(s.getLabel()));
        });
        
        assertEquals(expectedValues, actualValues, "Facets from search must include 'Material Type'");
    }
    
    private List<String> getSearchFacetNames(SearchRequest sr) {
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        return transactionSearch.execute(ts -> {
            try {
                SearchResult sresult = searchService.search(sr.getQuery(), sr.getOptions());
                return sresult.getFacets().stream()
                        .map(f -> f.getName())
                        .collect(Collectors.toList());
            } catch (Exception e1) {
                return new ArrayList<>();
            }
        });
    }

    
    private List<List<FV>> getSearchFacetValues(SearchRequest sr, String facetName) {
        TransactionTemplate transactionSearch = new TransactionTemplate(transactionManager);
        return transactionSearch.execute(ts -> {
            try {
                SearchResult sresult = searchService.search(sr.getQuery(), sr.getOptions());
                return sresult.getFacets().stream()
                        .filter(f -> f.getName().equals(facetName))
                        .map(f -> f.getValues())
                        .collect(Collectors.toList());
            } catch (Exception e1) {
                return new ArrayList<>();
            }
        });
    }

}
