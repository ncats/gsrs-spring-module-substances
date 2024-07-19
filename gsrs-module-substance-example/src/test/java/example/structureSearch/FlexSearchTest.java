package example.structureSearch;

import example.GsrsModuleSubstanceApplication;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.controllers.SubstanceController;
import gsrs.module.substance.controllers.SubstanceLegacySearchService;
import gsrs.module.substance.indexers.SubstanceDefinitionalHashIndexer;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.chem.StructureProcessor;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    private String fileName = "testdumps/tartrate_set.gsrs";

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

            File dataFile = new ClassPathResource(fileName).getFile();
            Assertions.assertTrue(dataFile.exists());
            loadGsrsFile(dataFile);
            loadedData = true;
            log.info("loaded data");
        }
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void generateFlexSearchQuery() throws Exception {
        String structure = "C(C(C(=O)O)O)(C(=O)O)O";
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance substance= new ChemicalSubstanceBuilder()
                    .setStructureWithDefaultReference(structure)
                    .addName("Tartaric acid")
                    .setUUID(uuid)
                    .build();

        log.trace("created query substance");
        Structure structureStd = structureProcessor.taskFor(substance.getStructure().molfile)
                .standardize(true)
                .build()
                .instrument()
                .getStructure();
        log.trace("created structureStd");
        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);
        log.trace("created and wired controller");

        String hash=  controller.makeFlexSearchMoietyClauses(structureStd, false) + ")";
        log.trace("search hash: {}", hash);
        assertTrue(hash.contains("root_moieties_properties_STEREO_INSENSITIVE_HASH"));
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void generateFlexPlusSearchQuery() throws Exception {
        String structure = "C(C(C(=O)O)O)(C(=O)O)O";
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance substance= new ChemicalSubstanceBuilder()
                .setStructureWithDefaultReference(structure)
                .addName("Tartaric acid")
                .setUUID(uuid)
                .build();

        log.trace("created query substance");
        Structure structureStd = structureProcessor.taskFor(substance.getStructure().molfile)
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
        assertTrue(hash.contains("root_moieties_properties_EXACT_HASH"));
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void runFlexSearchQueryTartaric() throws Exception {
        String structure = "C(C(C(=O)O)O)(C(=O)O)O";
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance substance= new ChemicalSubstanceBuilder()
                .setStructureWithDefaultReference(structure)
                .addName("Tartaric acid")
                .setUUID(uuid)
                .build();

        Structure structureStd = structureProcessor.taskFor(substance.getStructure().molfile)
                .standardize(true)
                .build()
                .instrument()
                .getStructure();
        String sins= structureStd.getStereoInsensitiveHash();
        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);

        String hash=  controller.makeFlexSearch(structureStd, false);
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
        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);

        String hash = controller.makeFlexSearch(structureStd, false);
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
        SubstanceController controller = new SubstanceController();
        AutowireHelper.getInstance().autowireAndProxy(controller);

        String hash = controller.makeFlexSearch(structureStd,true);
        log.trace("search hash: {}", hash);
        SearchRequest request = new SearchRequest.Builder()
                .kind(Substance.class)
                .query(hash)
                .build();
        List<Substance> substances = getSearchList(request);
        log.trace("search results: (total: {})", substances.size());
        substances.forEach(s-> log.trace("ID {} - {}", s.uuid, s.getName()));

        int expectedNumber = 1;
        assertEquals(expectedNumber, substances.size());
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

}
