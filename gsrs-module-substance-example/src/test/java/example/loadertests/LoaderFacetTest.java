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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
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

        System.out.println("facetNames size: " + facetNames.size());
        facetNames.forEach(n -> System.out.println("facet: " + n));
        assertTrue(facetNames.contains(codeSystem1) && facetNames.contains(TestableFacetIndexValueMaker.LETTER_DIGIT_RATIO_NAME));
    }

    @Test
    public void testSearchByCodeSystemFacetValues() {
        String codeSystem1 = "DRUG CENTRAL";
        String expectedSubstanceClass ="chemical";
        int expectedChemicalCount =5;
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(10)
                .query("root_codes_codeSystem:\"" + codeSystem1 + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        List<List<FV>> values = getSearchFacetValues(request, TestableFacetIndexValueMaker.LETTER_DIGIT_RATIO_NAME);
        final AtomicInteger listNum = new AtomicInteger(0);
        values.forEach(vList->{
            log.trace("values for " + listNum.incrementAndGet());
            vList.forEach(i-> log.trace(i.getLabel() + ":" + i.getCount()));
        });
        
        List<List<FV>> stringValues = getSearchFacetValues(request, "Substance Class");
        listNum.set(0);
        stringValues.forEach(vList->{
            log.trace("values for " + listNum.incrementAndGet());
            vList.forEach(i-> log.trace(i.getLabel() + ":" + i.getCount()));
        });
        assertTrue(values.size() >0);
        
        assertTrue(stringValues.stream()
                .anyMatch(p->p.stream().anyMatch(p2->p2.getLabel().equals(expectedSubstanceClass) && p2.getCount()==expectedChemicalCount)));
    }

    @Test
    public void testSearchByNameFacetValues() {
        String nameToSearch="PLASMALYTE A";
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(10)
                .query("root_names_name:\"" + nameToSearch + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        final AtomicInteger listNum = new AtomicInteger(0);
        
        List<List<FV>> stringValues = getSearchFacetValues(request, "Code System");
        listNum.set(0);
        stringValues.forEach(vList->{
            log.trace("values for " + listNum.incrementAndGet());
            vList.forEach(i-> log.trace(i.getLabel() + ":" + i.getCount()));
        });
        
        List<String> expectedCodeSystems = Arrays.asList("CAS", "FDA UNII");
        assertTrue(expectedCodeSystems.stream()
                .allMatch(codeSystem -> stringValues.stream()
                        .anyMatch(p->p.stream()
                                .anyMatch(p2->p2.getLabel().equals(codeSystem) ))));
    }

    @Test
    public void testSearchByStereoFacetValues() {
        /*
        search for structure field and verify that all results have the class facet with the expected value
        */
        String stereochemistry="ACHIRAL";
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(10)
                .query("root_structure_stereoChemistry:\"" + stereochemistry + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        final AtomicInteger listNum = new AtomicInteger(0);
        
        List<List<FV>> stringValues = getSearchFacetValues(request, "Substance Class");
        listNum.set(0);
        stringValues.forEach(vList->{
            log.trace("values for " + listNum.incrementAndGet());
            vList.forEach(i-> log.trace(i.getLabel() + ":" + i.getCount()));
        });
        
        List<String> expectedValues = Arrays.asList("chemical");
        assertTrue(expectedValues.stream()
                .allMatch(codeSystem -> stringValues.stream()
                        .allMatch(p->p.stream()
                                .allMatch(p2->p2.getLabel().equals(codeSystem) ))));
    }

    @Test
    public void testSearchBySourceMaterialFacetValues() {
        /*
        Search for source material type == 'VIRUS' and retrieve the corresponding facet.
        Make sure the values are the same
        */
        String strDivSourceMatType="VIRUS";
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(10)
                .query("root_structure_structurallyDiverse_sourceMaterialType:\"" + strDivSourceMatType + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        final AtomicInteger listNum = new AtomicInteger(0);
        
        List<List<FV>> stringValues = getSearchFacetValues(request, "Material Type");
        listNum.set(0);
        stringValues.forEach(vList->{
            log.trace("values for " + listNum.incrementAndGet());
            vList.forEach(i-> log.trace(i.getLabel() + ":" + i.getCount()));
        });
        
        List<String> expectedValues = Arrays.asList(strDivSourceMatType);
        assertTrue(expectedValues.stream()
                .allMatch(type -> stringValues.stream()
                        .allMatch(p->p.stream()
                                .allMatch(p2->p2.getLabel().equals(type) ))));
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
                        .map(f->f.getValues())
                        .collect(Collectors.toList());
            } catch (Exception e1) {
                return new ArrayList<>();
            }
        });
    }

}
