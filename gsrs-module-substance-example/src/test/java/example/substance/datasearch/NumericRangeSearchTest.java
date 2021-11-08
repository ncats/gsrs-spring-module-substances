package example.substance.datasearch;

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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
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
        Date now = new Date();
        long nowMillis = now.getTime();
        long startRange = nowMillis - 1000;
        long endRange = nowMillis + 1000;
        String start = "" + (startRange);
        String end = "" + (endRange);
        
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_created:[" + start + " TO " + end + "] ")
                .top(Integer.MAX_VALUE)
                .build();
        
        log.trace("query: " + request.getQuery());
        List<Substance> substances = getSearchList(request);
        
        log.trace("substances size: " + substances.size());
        substances.forEach(s -> {
            String msg = String.format("ID: %s; created: %d", s.uuid, s.created.getTime());            
            log.trace(msg);
        });
        Assertions.assertTrue(substances.stream().allMatch(s -> (s.created.getTime() > startRange) && (s.created.getTime() < endRange)));
    }

    @Test
    public void testSearchBy2DateRanges() {
        //AND logic
        log.trace("starting in testSearchByDateRange");
        Date now = new Date();
        long nowMillis = now.getTime();
        long startRange = nowMillis - 1000;
        long endRange = nowMillis + 1000;
        String start = "" + (startRange);
        String end = "" + (endRange);
        
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_created:[" + start + " TO " + end + "] OR root_lastEdited:[" + start + " TO " + end + "]")
                .top(Integer.MAX_VALUE)
                .build();
        
        log.trace("query: " + request.getQuery());
        List<Substance> substances = getSearchList(request);
        
        log.trace("substances size: " + substances.size());
        substances.forEach(s -> {
            String msg = String.format("ID: %s; created: %d", s.uuid, s.created.getTime());            
            log.trace(msg);
        });
        Assertions.assertTrue( substances.size() >0 && substances.stream().allMatch(s -> (s.created.getTime() > startRange) && (s.created.getTime() < endRange)));
    }
    
    @Test
    public void testSearchBy2DateRanges2() {
        
        log.trace("starting in testSearchBy2DateRanges2");
        Date now = new Date();
        long nowMillis = now.getTime();
        long startRange = nowMillis - 1000;
        long endRange = nowMillis + 1000;
        String start = "" + (startRange);
        String end = "" + (endRange);
        
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_created:[" + start + " TO " + end + "] AND root_lastEdited:[" + start + " TO " + end + "]")
                .top(Integer.MAX_VALUE)
                .build();
        
        log.trace("query: " + request.getQuery());
        List<Substance> substances = getSearchList(request);
        
        log.trace("substances size: " + substances.size());
        substances.forEach(s -> {
            String msg = String.format("ID: %s; created: %d", s.uuid, s.created.getTime());            
            log.trace(msg);
        });
        Assertions.assertTrue( substances.size() >0 && substances.stream().allMatch(s -> (s.created.getTime() > startRange) && (s.created.getTime() < endRange)));
    }
    
    @Test
    public void testSearchByMwtRange() {
        
        log.trace("starting in testSearchByMwtRange");
        String start = "354.0";
        String end = "356.0";
        
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                //.query("root_structure_mwt:%5B" + start + " TO " + end + "%5D")
                .query("root_structure_mwt:[" + start + " TO " + end + "]")
                .top(Integer.MAX_VALUE)
                .build();
        
        log.trace("query: " + request.getQuery());
        List<Substance> substances = getSearchList(request);
        
        log.trace("substances size: " + substances.size());
        
        List<String> substanceIds = substances.stream()
                .peek(s2 -> log.trace("id: " + s2.approvalID + "; class " + s2.substanceClass.toString()
                + (s2.substanceClass.toString().toUpperCase().startsWith("CHEMICAL") ? ((ChemicalSubstance) s2).getStructure().mwt : "  ")))
                .map(s -> s.getUuid().toString())
                .collect(Collectors.toList());
        String expectedId = "ac776f92-b90f-48a0-a54e-7461a60c84b3";
        Assertions.assertTrue(substanceIds.contains(expectedId));
    }

    @Test
    public void testSearchByStereoRange() {
        log.trace("starting in testSearchByStereoRange");
        int startRange =3;
        int endRange =5;
        String start = ""+startRange;
        String end = "" + endRange;
        
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                //.query("root_structure_mwt:%5B" + start + " TO " + end + "%5D")
                .query("root_structure_mwt:[" + start + " TO " + end + "]")
                .top(Integer.MAX_VALUE)
                .build();
        
        log.trace("query: " + request.getQuery());
        List<Substance> substances = getSearchList(request);
        
        log.trace("substances size: " + substances.size());
        
        Assertions.assertTrue(substances.stream().allMatch(s-> ((ChemicalSubstance)s).getStructure().stereoCenters >= startRange &&
        ((ChemicalSubstance)s).getStructure().stereoCenters <= endRange));
    }
    
    @Test
    public void testSearchSimple() {
        
        log.trace("starting in testSearchSimple");
        String nameToSearch = "SODIUM GLUCONATE";
        
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_names_name:\"^" + nameToSearch + "$\"")
                .top(Integer.MAX_VALUE)
                .build();
        
        log.trace("query: " + request.getQuery());
        List<Substance> substances = getSearchList(request);
        
        log.trace("substances size: " + substances.size());
        List<String> substanceIds = substances.stream()
                .map(s -> s.getUuid().toString())
                .collect(Collectors.toList());
        String expectedId = "90e9191d-1a81-4a53-b7ee-560bf9e68109";
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
                System.err.println("Error within getSearchList: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
                
            }
        });
        return substances;
    }
    
}
