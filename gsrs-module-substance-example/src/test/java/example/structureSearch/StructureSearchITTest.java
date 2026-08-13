package example.structureSearch;

import com.fasterxml.jackson.databind.JsonNode;
import example.GsrsModuleSubstanceApplication;
import example.substance.FlexAndExactSearchFullStackTest;
import gov.nih.ncats.common.sneak.Sneak;
import gov.nih.ncats.structureIndexer.StructureIndexer;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.controllers.SubstanceController;
import gsrs.module.substance.indexers.ChemicalSubstanceStructureHashIndexValueMaker;
import gsrs.module.substance.services.SubstanceStructureSearchService;
import gsrs.services.PrincipalServiceImpl;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestIndexValueMakerFactory;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.core.models.ETag;
import ix.core.search.SearchResultContext;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@Tag("fullstack")
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class StructureSearchITTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private StructureIndexerService indexer;

    @Autowired
    private SubstanceController substanceController;

    @Autowired
    private SubstanceStructureSearchService substanceStructureSearchService;

    @Autowired
    private TestIndexValueMakerFactory testIndexValueMakerFactory;


    @Autowired
    private PrincipalServiceImpl principalService;

    @BeforeEach
    public void clearIndexers() throws IOException {
        indexer.removeAll();
        principalService.clearCache();
    }

    private void registerStructureHashIndexer() {
        ChemicalSubstanceStructureHashIndexValueMaker structureHashIndexer =
                new ChemicalSubstanceStructureHashIndexValueMaker();
        AutowireHelper.getInstance().autowire(structureHashIndexer);
        testIndexValueMakerFactory.addIndexValueMaker(structureHashIndexer);
    }

    private static String anyBondMolfile() {
        return "\n" +
                "  Ketcher  8122621122D 1   1.00000     0.00000     0\n" +
                "\n" +
                "  2  1  0  0  0  0            999 V2000\n" +
                "    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    1.5000    0.0000    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  8  0  0  0  0\n" +
                "M  END";
    }

    private static String singleBondMolfile() {
        return anyBondMolfile().replace("  1  2  8  0  0  0  0",
                "  1  2  1  0  0  0  0");
    }

    private static String replaceFirstSingleBondType(String molfile, int bondType) {
        return molfile.replaceFirst("  1  2  1  0  0  0  0",
                "  1  2  " + bondType + "  0  0  0  0");
    }

    private static String complexAnyBondMolfile() throws IOException {
        return Files.readString(new ClassPathResource("molfiles/substance_structure_any_bond.mol").getFile().toPath());
    }

    private static String replaceComplexQueryBondType(String molfile, int bondType) {
        return molfile.replace(" 19 20  8  0  0  0  0",
                " 19 20  " + bondType + "  0  0  0  0");
    }

    private static String benzeneMolfile() {
        return "\n" +
                "  Ketcher  8122621122D 1   1.00000     0.00000     0\n" +
                "\n" +
                "  6  6  0  0  0  0            999 V2000\n" +
                "    0.0000    1.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    1.2990    0.7500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    1.2990   -0.7500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "    0.0000   -1.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   -1.2990   -0.7500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   -1.2990    0.7500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  2  0  0  0  0\n" +
                "  3  4  1  0  0  0  0\n" +
                "  4  5  2  0  0  0  0\n" +
                "  5  6  1  0  0  0  0\n" +
                "  6  1  2  0  0  0  0\n" +
                "M  END";
    }

    private static String aromaticBenzeneQueryMolfile() {
        return benzeneMolfile()
                .replace("  1  2  1  0  0  0  0", "  1  2  4  0  0  0  0")
                .replace("  2  3  2  0  0  0  0", "  2  3  4  0  0  0  0")
                .replace("  3  4  1  0  0  0  0", "  3  4  4  0  0  0  0")
                .replace("  4  5  2  0  0  0  0", "  4  5  4  0  0  0  0")
                .replace("  5  6  1  0  0  0  0", "  5  6  4  0  0  0  0")
                .replace("  6  1  2  0  0  0  0", "  6  1  4  0  0  0  0");
    }

    private SearchResultContext substructureSearch(String molfile) throws Exception {
        MultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
        queryMap.put("type", Collections.singletonList("sub"));
        queryMap.put("q", Collections.singletonList(molfile));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/substances/structureSearch");
        request.addParameter("type", "sub");
        request.addParameter("q", molfile);

        Object results = substanceController.structureSearchPost(
                queryMap,
                request,
                new FlexAndExactSearchFullStackTest.MockRedirectAttributes());

        assertNotNull(results);
        ResponseEntity responseEntity = (ResponseEntity) results;
        SearchResultContext result = (SearchResultContext) responseEntity.getBody();
        result.getDeterminedFuture().get();
        return result;
    }

    private SearchResultContext substructureServiceSearch(String molfile) throws Exception {
        SubstanceStructureSearchService.SanitizedSearchRequest searchRequest =
                SubstanceStructureSearchService.SearchRequest.builder()
                        .type(SubstanceStructureSearchService.StructureSearchType.SUBSTRUCTURE)
                        .q(molfile)
                        .build()
                        .sanitize();

        SearchResultContext result = substanceStructureSearchService.search(searchRequest, UUID.randomUUID().toString());
        result.getDeterminedFuture().get();
        return result;
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void saveChemicalAndSearchForStructureShouldFind() throws Exception {
        String structure = "C1CCCCC1";
        UUID uuid = UUID.randomUUID();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s -> {
            new ChemicalSubstanceBuilder()

                    .setStructureWithDefaultReference(structure)
                    .addName("Test")
                    .setUUID(uuid)
                    .buildJsonAnd(this::assertCreated);
        });


        StructureIndexer.ResultEnumeration result = indexer.substructure(structure);
        assertTrue(result.hasMoreElements());
        assertEquals(uuid.toString(), result.nextElement().getId());
        assertFalse(result.hasMoreElements());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void saveMultipleChemicalsSearchShouldOnlyFindMatch() throws Exception {
        String structure = "C1CCCCC1";
        UUID uuid = UUID.randomUUID();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s -> {
            new ChemicalSubstanceBuilder()

                    .setStructureWithDefaultReference(structure)
                    .addName("Test")
                    .setUUID(uuid)
                    .buildJsonAnd(this::assertCreated);

            new ChemicalSubstanceBuilder()

                    .setStructureWithDefaultReference("[Na+][Cl-]")
                    .addName("something Else")
                    .buildJsonAnd(this::assertCreated);
        });


        StructureIndexer.ResultEnumeration result = indexer.substructure(structure);
        assertTrue(result.hasMoreElements());
        assertEquals(uuid.toString(), result.nextElement().getId());
        assertFalse(result.hasMoreElements());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void ensureIsobutaneSSSDoesntReturnIsoPentene() throws Exception {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s -> {
                    UUID uuid = UUID.randomUUID();
                    String mol1 = "\n" +
                            "   JSDraw209182020002D\n" +
                            "\n" +
                            "  4  3  0  0  0  0              0 V2000\n" +
                            "   23.1921   -7.4013    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                            "   23.6531   -8.8915    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                            "   25.1741   -9.2375    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                            "   22.5929  -10.0359    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                            "  1  2  1  0  0  0  0\n" +
                            "  2  3  1  0  0  0  0\n" +
                            "  2  4  1  0  0  0  0\n" +
                            "M  END";
                    new ChemicalSubstanceBuilder()

                            .setStructureWithDefaultReference(mol1)
                            .addName("Test")
                            .setUUID(uuid)
                            .buildJsonAnd(this::assertCreated);
                });

        String mol2 = "\n" +
                "   JSDraw209182020002D\n" +
                "\n" +
                "  5  4  0  0  0  0              0 V2000\n" +
                "   23.1921   -7.4013    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   23.6531   -8.8915    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   25.1741   -9.2375    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   22.5929  -10.0359    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   26.2344   -8.0932    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  2  0  0  0  0\n" +
                "  2  3  1  0  0  0  0\n" +
                "  2  4  1  0  0  0  0\n" +
                "  3  5  1  0  0  0  0\n" +
                "M  END";

        StructureIndexer.ResultEnumeration result = indexer.substructure(mol2);
        assertFalse(result.hasMoreElements());

    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void createStructureAndSubstructureSearchForItselfShouldWork() throws Exception {


        UUID uuid = UUID.randomUUID();
        String mol1 = "\n" +
                "   JSDraw209182020002D\n" +
                "\n" +
                "  4  3  0  0  0  0              0 V2000\n" +
                "   23.1921   -7.4013    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   23.6531   -8.8915    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   25.1741   -9.2375    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "   22.5929  -10.0359    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                "  1  2  1  0  0  0  0\n" +
                "  2  3  1  0  0  0  0\n" +
                "  2  4  1  0  0  0  0\n" +
                "M  END";

        new ChemicalSubstanceBuilder()

                .setStructureWithDefaultReference(mol1)
                .addName("Test")
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);


        StructureIndexer.ResultEnumeration result = indexer.substructure(mol1);
        assertTrue(result.hasMoreElements());

    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void registeredMolfileWithExplicitStereoHydrogensShouldFindItselfBySubstructureSearch() throws Exception {
        registerStructureHashIndexer();
        String molfile = Files.readString(new ClassPathResource("molfiles/d8a979a7-f6b7-423a-be7b-c62e8651eb92.mol").getFile().toPath());
        UUID uuid = UUID.randomUUID();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s -> {
            new ChemicalSubstanceBuilder()
                    .setStructureWithDefaultReference(molfile)
                    .addName("Explicit stereo hydrogens")
                    .setUUID(uuid)
                    .buildJsonAnd(this::assertCreated);
        });

        MultiValueMap<String, String> exactQueryMap = new LinkedMultiValueMap<>();
        exactQueryMap.put("type", Collections.singletonList("exact"));
        exactQueryMap.put("q", Collections.singletonList(molfile));
        Object exactResults = substanceController.structureSearchPost(
                exactQueryMap,
                new MockHttpServletRequest(),
                new FlexAndExactSearchFullStackTest.MockRedirectAttributes());
        ETag exactResult = (ETag) ((ResponseEntity) exactResults).getBody();
        assertEquals(1, exactResult.count);

        MultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
        queryMap.put("type", Collections.singletonList("sub"));
        queryMap.put("q", Collections.singletonList(molfile));

        Object results = substanceController.structureSearchPost(
                queryMap,
                new MockHttpServletRequest(),
                new FlexAndExactSearchFullStackTest.MockRedirectAttributes());

        assertNotNull(results);
        ResponseEntity responseEntity = (ResponseEntity) results;
        SearchResultContext result = (SearchResultContext) responseEntity.getBody();
        result.getDeterminedFuture().get();
        assertEquals(1, result.getCount());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void anyBondMolfileQueryShouldPrepareAndRunSubstructureSearch() throws Exception {
        String molfile = anyBondMolfile();

        ResponseEntity<Object> interpreted = substanceController.interpretStructure(
                molfile,
                Collections.emptyMap());
        assertTrue(interpreted.getStatusCode().is2xxSuccessful());
        JsonNode interpretedBody = (JsonNode) interpreted.getBody();
        assertTrue(interpretedBody.has("structure"));

        SearchResultContext result = substructureSearch(molfile);
        assertEquals(0, result.getCount());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void queryBondMolfileShouldFindConcreteStartingStructureBySubstructureSearch() throws Exception {
        String molfile = singleBondMolfile();
        UUID uuid = UUID.randomUUID();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s -> {
            new ChemicalSubstanceBuilder()
                    .setStructureWithDefaultReference(molfile)
                    .addName("Query bond searchable concrete structure")
                    .setUUID(uuid)
                    .buildJsonAnd(this::assertCreated);
        });

        assertEquals(1, substructureServiceSearch(molfile).getCount());
        assertEquals(1, substructureServiceSearch(replaceFirstSingleBondType(molfile, 8)).getCount());
        assertEquals(1, substructureServiceSearch(replaceFirstSingleBondType(molfile, 5)).getCount());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void complexQueryBondMolfileShouldFindConcreteStartingStructureBySubstructureSearch() throws Exception {
        String anyBondQuery = complexAnyBondMolfile();
        String molfile = replaceComplexQueryBondType(anyBondQuery, 1);
        UUID uuid = UUID.randomUUID();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s -> {
            new ChemicalSubstanceBuilder()
                    .setStructureWithDefaultReference(molfile)
                    .addName("Complex query bond searchable concrete structure")
                    .setUUID(uuid)
                    .buildJsonAnd(this::assertCreated);

            new ChemicalSubstanceBuilder()
                    .setStructureWithDefaultReference("C1CCCCC1")
                    .addName("Unrelated query bond non-match")
                    .buildJsonAnd(this::assertCreated);
        });

        assertEquals(1, substructureServiceSearch(molfile).getCount());
        assertEquals(1, substructureServiceSearch(anyBondQuery).getCount());
        assertEquals(1, substructureServiceSearch(replaceComplexQueryBondType(anyBondQuery, 5)).getCount());
        assertEquals(0, substructureServiceSearch(replaceComplexQueryBondType(anyBondQuery, 4)).getCount());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void aromaticBondMolfileQueryShouldFindAromaticStartingStructureBySubstructureSearch() throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s -> {
            new ChemicalSubstanceBuilder()
                    .setStructureWithDefaultReference(benzeneMolfile())
                    .addName("Aromatic query bond searchable structure")
                    .buildJsonAnd(this::assertCreated);

            new ChemicalSubstanceBuilder()
                    .setStructureWithDefaultReference("C1CCCCC1")
                    .addName("Non-aromatic query bond non-match")
                    .buildJsonAnd(this::assertCreated);
        });

        assertEquals(1, substructureServiceSearch(aromaticBenzeneQueryMolfile()).getCount());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void ensureSubstructureSearchHasBasicSmartsSupport() throws Exception {
        UUID uuid = UUID.randomUUID();
        new ChemicalSubstanceBuilder()

                .setStructureWithDefaultReference("COC1=CC=C(O)C2=C(O)C(C)=C3OC(C)(O)C(=O)C3=C12")
                .addName("Test")
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);

        UUID uuid2 = UUID.randomUUID();
        new ChemicalSubstanceBuilder()

                .setStructureWithDefaultReference("CC1=C2OC(C)(O)C(=O)C2=C3C4=C(C=C(O)C3=C1O)N5C=CC=CC5=N4")
                .addName("Test2")
                .setUUID(uuid2)
                .buildJsonAnd(this::assertCreated);

        // Search for the specific bicyclic structure in first compound
        // Using a SMARTS pattern that matches oxygen or nitrogen attached to aromatic rings
        StructureIndexer.ResultEnumeration result = indexer.substructure("[#7,#8]c1ccc(O)c2c(O)c([#6])c3OC([#6])(O)C(=O)c3c12");
        assertTrue(result.hasMoreElements());
        Set<UUID> matches = new LinkedHashSet<>();
        while(result.hasMoreElements()){
            matches.add(UUID.fromString(result.nextElement().getId()));
        }
        // Only the first structure matches this specific bicyclic pattern
        // The second structure has a different ring system (tricyclic with N5)
        assertEquals(new LinkedHashSet<>(Arrays.asList(uuid)), matches);
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void smartsAtomListShouldMatchRegisteredStructureBySubstructureSearch() throws Exception {
        new ChemicalSubstanceBuilder()
                .setStructureWithDefaultReference("CCNCC")
                .addName("Atom list SMARTS positive")
                .buildJsonAnd(this::assertCreated);

        new ChemicalSubstanceBuilder()
                .setStructureWithDefaultReference("CCOCC")
                .addName("Atom list SMARTS negative")
                .buildJsonAnd(this::assertCreated);

        assertEquals(1, substructureServiceSearch("CC[#6,#7]C").getCount());
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void explicitHShouldWork() throws Exception {


        UUID uuid = UUID.randomUUID();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s -> {
                    new ChemicalSubstanceBuilder()

                            .setStructureWithDefaultReference("C(=CC=C1)C=C1")
                            .addName("Test")
                            .setUUID(uuid)
                            .buildJsonAnd(this::assertCreated);
                });

        transactionTemplate.executeWithoutResult(s -> {
            StructureIndexer.ResultEnumeration result = null;
            try {
                result = indexer.substructure("C(=C(C(=C1[H])[H])[H])(C(=C1[H])[H])[H]");

            assertTrue(result.hasMoreElements());
            assertEquals(uuid.toString(), result.nextElement().getId());
            assertFalse(result.hasMoreElements());
            } catch (Exception e) {
                Sneak.sneakyThrow(e);
            }
        });

    }
}
