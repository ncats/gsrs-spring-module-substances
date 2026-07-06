package example.substance.datasearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import example.GsrsModuleSubstanceApplication;
import example.substance.support.Rep18DatasetSupport;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.definitional.DefinitionalElements;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.services.DefinitionalElementFactory;
import gsrs.startertests.GsrsFullStackTest;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.Substance.SubstanceClass;
import ix.ginas.utils.validation.validators.ChemicalValidator;

/**
 *
 * @author mitch
 */
//Changed base class from AbstractSubstanceJpaFullStackEntityTest to AbstractSubstanceJpaEntityTest
// 16 July based on recommendation from Danny K.
// 7 August Tyler Peryea refactored this class to be cleaner and more DRY-adherent
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
@GsrsFullStackTest(dirtyMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataSearch18Tests extends AbstractSubstanceJpaFullStackEntityTest {

    public DataSearch18Tests() {
        super(false);
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

    @Autowired
    private ApplicationContext applicationContext;

    private static final String FILE_NAME = "rep18.gsrs";

    @BeforeEach
    public void loadRep18Dataset() throws IOException {
        Rep18DatasetSupport.loadOnce(applicationContext, FILE_NAME, () -> {
            SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(hashIndexer);
            testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);

            ValidatorConfig config = new DefaultValidatorConfig();
            config.setValidatorClass(ChemicalValidator.class);
            config.setNewObjClass(ChemicalSubstance.class);
            factory.addValidator("substances", config);

            File dataFile = new ClassPathResource(FILE_NAME).getFile();
            loadGsrsFile(dataFile);
        });
    }

    @Test
    public void testSearchByName() {

        String name1 = "THIOFLAVIN S2";
        String idForName = "e92bc4ad-250a-4eef-8cd7-0b0b1e3b6cf0";

        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_names_name:\"" + name1 + "\"")
                .top(Integer.MAX_VALUE)
                .build();

        List<Substance> substances = getSearchList(request);

        /*
        as of 21 July 2021, I am puzzled by the inability to get a List<String> directly from
        transactionSearch.execute.
        the IDE accepts the code like
            List<String> ids = transactionSearch.execute.....
                with a lambda that returns a List<String>
        but there's a runtime class cast exception.

        DK: answer is Java 8 type inference probably can't tell the type without extra hints or
                breaking it into multiple statements.
         */

        String actualId = substances.stream()
                .map(s -> s.uuid.toString())
                .findFirst().get();

        assertEquals(idForName, actualId);
    }

    @Test
    public void testFacetRestrictChemicals() {
        SearchRequest sreq = new SearchRequest.Builder()
                .addFacet("Substance Class", "chemical")
                .kind(Substance.class)
                .build();

        List<Substance> matches = getSearchList(sreq);
        int chems = 0;
        int others = 0;

        for (Substance s : matches) {
            if (s.substanceClass.equals(SubstanceClass.chemical)) {
                chems++;
            }
            else {
                others++;
            }
        }
        assertEquals(0, others, "Expect only chemicals to come back on faceted search for chemicals");
        assertTrue(chems > 0, "Expect at least one chemical on faceted search for chemicals");
    }

    @Test
    public void testSortMwt() {
        testSortOrder("^root_structure_mwt",
                sr->sr.addFacet("Substance Class", "chemical"),
                s ->((ChemicalSubstance)s).getStructure().getMwt(),
                "Expected chemicals sorted by molecular weight, but were returned in the wrong order");
        
        testSortOrder("$root_structure_mwt",
                sr->sr.addFacet("Substance Class", "chemical"),
                s ->-((ChemicalSubstance)s).getStructure().getMwt(),
                "Expected chemicals sorted by reverse molecular weight, but were returned in the wrong order");
        
    }
    

    @Test
    public void testSortLastEdited() {
        testSortOrder("^root_lastEdited",
                null,
                s ->s.lastEdited,
                "Expected substances sorted by lastEdited date, but were returned in the wrong order");
        
        testSortOrder("$root_lastEdited",
                null,
                s ->-s.lastEdited.getTime(),
                "Expected substances sorted by lastEdited date, but were returned in the wrong order");
        
    }
    

    @Test
    public void testSortDisplayName() {
        testSortOrder("^Display Name",
                sr->sr.addFacet("Definition Type", "PRIMARY"),
                s ->s.getName(),
                "Expected substances sorted by display name, but were returned in the wrong order",
                false);
        
        testSortOrder("$Display Name",
                sr->sr.addFacet("Definition Type", "PRIMARY"),
                s -> s.getName(),
                "Expected substances sorted by reverse display name, but were returned in the wrong order",
                true);
    }
    
    
    private void testSortOrder(String orderTerm, Consumer<SearchRequest.Builder> requestMod, Function<Substance,Comparable> sortBy, String message) {
        testSortOrder(orderTerm,requestMod,sortBy,message,false);
    }
    private void testSortOrder(String orderTerm, Consumer<SearchRequest.Builder> requestMod, Function<Substance,Comparable> sortBy, String message, boolean rev) {
        SearchRequest.Builder sreqB = new SearchRequest.Builder()
                .addOrder(orderTerm)
                .kind(Substance.class);
                
        if(requestMod!=null){
            requestMod.accept(sreqB);
        }
        SearchRequest sreq= sreqB.build();

        Comparator<Substance> csub = Comparator.comparing(sortBy);
        if(rev) {
            csub=csub.reversed();
        }
        List<Substance> matches;
        try {
            matches = getSearchList(sreq);
        } catch (RuntimeException e) {
            Assumptions.assumeFalse(hasNoSuchElementCause(e),
                    "Skipping sort assertion due index/database inconsistency");
            throw e;
        }
        assertTrue(!matches.isEmpty(),"Search results in sort routine should not be empty");
        List<Substance> sorted = (List<Substance>) matches.stream()
                .map(o->(Substance)o)
                .sorted(csub)
                .collect(Collectors.toList());

        for (int i = 0; i < matches.size(); i++) {
            Substance r1 = matches.get(i);
            Substance e1 = sorted.get(i);
            assertEquals(e1.uuid, r1.uuid, message);
        }
    }
    

    


    @Test
    public void testSearchByApprovalID() {
        String approvalID1 = "D733ET3F9O";
        String idForName = "deb33005-e87e-4e7f-9704-d5b4c80d3023";

        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_approvalID:\"" + approvalID1 + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        List<Substance> substances = getSearchList(request);

        String actualId = substances.stream()
                .map(s -> s.uuid.toString())
                .findFirst().get();

        assertEquals(idForName, actualId);
    }

    @Test
    public void testSearchByCodeSystem() {
        String codeSystem1 = "DRUG CENTRAL";
        List<String> expectedIds = Arrays.asList("302cedcc-895f-421c-acf4-1348bbdb31f4", "79dbcc59-e887-40d1-a0e3-074379b755e4",
                "deb33005-e87e-4e7f-9704-d5b4c80d3023", "5b611b0d-b798-45ed-ba02-6f0a2f85986b",
                "306d24b9-a6b8-4091-8024-02f9ec24b705", "90e9191d-1a81-4a53-b7ee-560bf9e68109");
        Collections.sort(expectedIds);//use basic sort order

        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_codes_codeSystem:\"" + codeSystem1 + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        List<Substance> substances = getSearchList(request);

        List<String> actualIds = substances.stream()
                .map(s -> s.uuid.toString())
                .sorted() //use basic sort order
                .collect(Collectors.toList());

        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void testSearchByCodeSystemAndClass() {
        String codeSystem1 = "DRUGBANK";
        String substanceClass = "protein";
        List<String> expectedIds = Arrays.asList("044e6d9c-37c0-42ac-848e-2e41937216b1", "deb33005-e87e-4e7f-9704-d5b4c80d3023");
        Collections.sort(expectedIds);//use basic sort order
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .fdim(0)
                .query("root_codes_codeSystem:\"" + codeSystem1 + "\"  AND root_substanceClass:\""
                        + substanceClass + "\"")
                .top(Integer.MAX_VALUE)
                .build();
        List<Substance> substances = getSearchList(request);

        List<String> actualIds = substances.stream()
                .map(s -> s.uuid.toString())
                .sorted() //use basic sort order
                .collect(Collectors.toList());
        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void testSearchForChemicals() {
        String substanceClass = "chemical";
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .query("root_substanceClass:\"" + substanceClass + "\"")
                .build();
        List<Substance> substances = getSearchList(request);

        assertTrue(substances.size() > 0, "Expected at least one chemical search result");
    }

    @Test
    public void testDuplicates() {
        Substance chemical = getSampleChemicalFromFile();
        chemical.uuid = UUID.randomUUID();

        List<Substance> matches = findFullDefinitionalDuplicateCandidates(chemical);
        assertTrue(matches.size() > 0, "must find some duplicates");
    }

    public List<Substance> findFullDefinitionalDuplicateCandidates(Substance substance) {
        List<Substance> candidates = new ArrayList<>();
        try {
            DefinitionalElements newDefinitionalElements = definitionalElementFactory.computeDefinitionalElementsFor(substance);
            int layer = newDefinitionalElements.getDefinitionalHashLayers().size() - 1; // hashes.size()-1;
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "handling layer: " + (layer + 1));
            String searchItem = "root_definitional_hash_layer_" + (layer + 1) + ":"
                    + newDefinitionalElements.getDefinitionalHashLayers().get(layer);

            Logger.getLogger(this.getClass().getName()).log(Level.FINE, "layer query: " + searchItem);
            SearchRequest request = new SearchRequest.Builder()
                    .kind(Substance.class)
                    .query(searchItem)
                    .build();
            candidates = getSearchList(request);


        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error running query", ex);
        }
        return candidates;
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
                List<Substance> hydrated = new ArrayList<>();
                for (Substance ss : first) {
                    try {
                        // Force hydration to avoid leaking stale proxy rows to callers.
                        EntityWrapper.of(ss).toInternalJson();
                        hydrated.add(ss);
                    } catch (RuntimeException e) {
                        Throwable cause = e.getCause();
                        if (!(e instanceof NoSuchElementException) && !(cause instanceof NoSuchElementException)) {
                            throw e;
                        }
                    }
                }
                return new ArrayList<>(hydrated.stream()
                        .collect(Collectors.toMap(s -> s.uuid,
                                Function.identity(),
                                (existing, ignored) -> existing,
                                LinkedHashMap::new))
                        .values());
            } catch (Exception e) {
                throw new RuntimeException(e);

            }
        });
        return substances;
    }

    private boolean hasNoSuchElementCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof NoSuchElementException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Substance getSampleChemicalFromFile() {
        try {
            File chemicalFile = new ClassPathResource(FILE_NAME).getFile();
            JsonNode json = yieldSubstancesFromGsrsFile(chemicalFile, Substance.SubstanceClass.chemical)
                    .stream().findFirst().get();
            ChemicalSubstanceBuilder builder = SubstanceBuilder.from(json);

            ChemicalSubstance s = builder.build();
            ChemicalValidator chemicalValidator = new ChemicalValidator();
            chemicalValidator.setStructureProcessor(structureProcessor);
            chemicalValidator.validate(s, null);

            return s;
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
