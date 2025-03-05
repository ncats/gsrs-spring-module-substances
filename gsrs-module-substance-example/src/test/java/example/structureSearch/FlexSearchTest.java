package example.structureSearch;

import example.GsrsModuleSubstanceApplication;
import example.substance.FlexAndExactSearchFullStackTest;
import gov.nih.ncats.molwitch.Chemical;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.controllers.SubstanceController;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.utils.ChemicalUtils;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
import ix.core.models.ETag;
import ix.core.models.Structure;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.utils.validation.validators.ChemicalValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@WithMockUser(username = "admin", roles = "Admin")
@Slf4j
public class FlexSearchTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    StructureProcessor structureProcessor;

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;

    @Autowired
    private TestGsrsValidatorFactory factory;

    @Autowired
    private StructureIndexerService indexer;

    @Autowired
    private SubstanceLegacySearchService searchService;

    @Autowired
    private ChemicalUtils chemicalUtils;

    @Autowired
    private SubstanceRepository substanceRepository;

    private String dataFileName = "testdumps/tartrate_set.gsrs";

    private boolean loadedData = false;

    @BeforeEach
    public void clearIndexers() throws IOException {
        if( !loadedData) {
            log.trace("starting to load data");
            SubstanceDefinitionalHashIndexer hashIndexer = new SubstanceDefinitionalHashIndexer();
            AutowireHelper.getInstance().autowire(hashIndexer);
            testIndexValueMakerFactory.addIndexValueMaker(hashIndexer);
            {
                ValidatorConfig config = new DefaultValidatorConfig();
                config.setValidatorClass(ChemicalValidator.class);
                config.setNewObjClass(ChemicalSubstance.class);
                factory.addValidator("substances", config);
            }

            File dataFile = new ClassPathResource(dataFileName).getFile();
            Assertions.assertTrue(dataFile.exists());
            loadGsrsFile(dataFile);
            loadedData = true;
            log.info("loaded data. total substances: {}", substanceRepository.count());
            dumpDb();
        }
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void generateFlexSearchQuery() throws Exception {
        String structure = "C(C(C(=O)O)O)(C(=O)O)O";
        Structure structureStd = createStructure(structure, "Tartaric acid");
                structureProcessor.taskFor(structureStd.molfile)
                .standardize(true)
                .build()
                .instrument()
                .getStructure();

        log.trace("created structureStd");
        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);
        log.trace("created and wired controller");

        String hash=  controller.makeFlexSearchMoietyClauses(structureStd, true) + ")";
        log.trace("search hash: {}", hash);
        assertTrue(hash.contains("root_moieties_properties_STEREO_INSENSITIVE_HASH"));
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void generateFlexPlusSearchQuery() throws Exception {
        String smiles = "C(C(C(=O)O)O)(C(=O)O)O";
        Structure structureStd = createStructure(smiles, "Tartaric acid");
        log.trace("created structureStd");
        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);
        log.trace("created and wired controller");

        String hash=  controller.makeFlexSearchMoietyClauses(structureStd, true) + ")";
        log.trace("search hash: {}", hash);
        assertTrue(hash.contains("root_moieties_properties_STEREO_INSENSITIVE_HASH"));
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void runFlexSearchQueryTartaric() throws Exception {
        String smiles = "C(C(C(=O)O)O)(C(=O)O)O";
        Structure structureStd = createStructure(smiles, "Tartaric acid");
        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);

        String hash=  controller.makeSearch(structureStd, true);
        log.trace("search hash: {}", hash);
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .query(hash)
                .build();
        List<Substance> substances = getSearchList(request);
        log.trace("Flex search hits:");
        substances.forEach(s-> log.trace("ID {} - {}", s.uuid, s.getName()));

        int expectedNumber = 7;
        assertEquals(expectedNumber, substances.size());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void runFlexSearchQuerySodiumTartrate() throws Exception {
        String molfileSource = "molfiles/sodium_tartrate.mol";
        File molfile = new ClassPathResource(molfileSource).getFile();
        Structure structureStd = structureProcessor.taskFor(Files.readString(molfile.toPath()))
                .standardize(true)
                .build()
                .instrument()
                .getStructure();
        structureStd = createStructure(Files.readString(molfile.toPath()),"sodium_tartrate");
        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);

        String hash = controller.makeSearch(structureStd, true);
        log.trace("search hash: {}", hash);
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .query(hash)
                .build();
        List<Substance> substances = getSearchList(request);
        substances.forEach(s-> log.trace("ID {} - {}", s.uuid, s.getName()));

        int expectedNumber = 3;
        assertEquals(expectedNumber, substances.size());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void runFlexPlusSearchQuerySodiumTartrate() throws Exception {
        String molfileSource = "molfiles/sodium_tartrate.mol";
        File molfile = new ClassPathResource(molfileSource).getFile();
        Structure structureStd = structureProcessor.taskFor(Files.readString(molfile.toPath()))
                .standardize(true)
                .build()
                .instrument()
                .getStructure();
        Chemical cleanedChemical = chemicalUtils.stripSalts(structureStd.toChemical());
        Structure cleanedStructure = new Structure();
        cleanedStructure.molfile = cleanedChemical.toMol();

        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);

        String hash = controller.makeSearch(cleanedStructure, true);
        log.trace("search hash: {}", hash);
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .query(hash)
                .build();
        List<Substance> substances = getSearchList(request);
        log.trace("search results: (total: {})", substances.size());
        substances.forEach(s-> log.trace("ID {} - {}", s.uuid, s.getName()));

        int expectedNumber = 7;
        assertEquals(expectedNumber, substances.size());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void runExactSearchQuerySodiumTartrate() throws Exception {
        String molfileSource = "molfiles/sodium_tartrate.mol";
        File molfile = new ClassPathResource(molfileSource).getFile();

        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);

        MultiValueMap<String,String> queryMap = new LinkedMultiValueMap<>();
        queryMap.put("type", Collections.singletonList("exact"));
        queryMap.put("q", Collections.singletonList(Files.readString(molfile.toPath())));

        HttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributes redirectAttributes = new FlexAndExactSearchFullStackTest.MockRedirectAttributes();

        Object results = controller.structureSearchPost(queryMap, request, redirectAttributes);
        assertNotNull(results);
        ResponseEntity responseEntity = (ResponseEntity) results;
        ETag result = (ETag) responseEntity.getBody();
        assertEquals(1, result.count);
        ArrayList list =(ArrayList) result.getContent();
        log.trace("search search result size: {}; element type: {}", list.size(), list.get(0).getClass().getName());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void runExactPlusSearchQuerySodiumTartrate() throws Exception {
        String molfileSource = "molfiles/sodium_tartrate.mol";
        File molfile = new ClassPathResource(molfileSource).getFile();

        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);

        MultiValueMap<String,String> queryMap = new LinkedMultiValueMap<>();
        queryMap.put("type", Collections.singletonList("exactplus"));
        queryMap.put("q", Collections.singletonList(Files.readString(molfile.toPath())));

        HttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributes redirectAttributes = new FlexAndExactSearchFullStackTest.MockRedirectAttributes();

        Object results = controller.structureSearchPost(queryMap, request, redirectAttributes);
        log.trace("search search result type: {}", results.getClass().getName());
        assertNotNull(results);
        ResponseEntity responseEntity = (ResponseEntity) results;
        log.trace("search search result type: {} - body type: {}", results.getClass().getName(),
                responseEntity.getBody().getClass().getName());
        ETag result = (ETag) responseEntity.getBody();
        assertTrue(result.count >0);
        log.trace("search search result: {}", result.getContent());
    }

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

    @Transactional
    protected void dumpDb(){
        TransactionTemplate transactionRetrieve = new TransactionTemplate(transactionManager);
        transactionRetrieve.executeWithoutResult(s->{
            substanceRepository.findAll().forEach(tr->{
                if(s instanceof ChemicalSubstance){
                    ChemicalSubstance chemicalSubstance = (ChemicalSubstance) s;
                    log.trace("UUID: {}; SMILES: {}; InChIKey: {}",
                            chemicalSubstance.getUuid(),
                            chemicalSubstance.getStructure().smiles,
                            chemicalSubstance.getStructure().getExactHash());
                }
            });
        });
    }

    private Structure createStructure(String smiles, String name) throws Exception {
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance substance= new ChemicalSubstanceBuilder()
                .setStructureWithDefaultReference(smiles)
                .addName(name)
                .setUUID(uuid)
                .build();

        log.trace("created query substance");
        Structure structureStd = structureProcessor.taskFor(substance.getStructure().molfile)
                .standardize(true)
                .build()
                .instrument()
                .getStructure();
        log.trace("created structureStd");
        return structureStd;
    }
}
