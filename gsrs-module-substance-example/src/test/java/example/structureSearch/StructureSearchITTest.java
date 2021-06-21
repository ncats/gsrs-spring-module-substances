package example.structureSearch;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.search.MolSearcherFactory;
import gov.nih.ncats.structureIndexer.StructureIndexer;
import gsrs.legacy.structureIndexer.StructureIndexerEventListener;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.services.SubstanceStructureSearchService;
import gsrs.services.PrincipalService;
import gsrs.services.PrincipalServiceImpl;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StructureSearchITTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private StructureIndexerService indexer;


    @Autowired
    private SubstanceEntityService substanceEntityService;

    @Autowired
    private PrincipalServiceImpl principalService;

    @BeforeEach
    public void clearIndexers() throws IOException {
        indexer.removeAll();
        principalService.clearCache();
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void saveChemicalAndSearchForStructureShouldFind() throws Exception {
        String structure = "C1CCCCC1";
        UUID uuid = UUID.randomUUID();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s -> {
            new ChemicalSubstanceBuilder()

                    .setStructure(structure)
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

                    .setStructure(structure)
                    .addName("Test")
                    .setUUID(uuid)
                    .buildJsonAnd(this::assertCreated);

            new ChemicalSubstanceBuilder()

                    .setStructure("[Na+][Cl-]")
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

                .setStructure(mol1)
                .addName("Test")
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);

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

                .setStructure(mol1)
                .addName("Test")
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);


        StructureIndexer.ResultEnumeration result = indexer.substructure(mol1);
        assertTrue(result.hasMoreElements());

    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void ensureSubstructureSearchHasBasicSmartsSupport() throws Exception {
        UUID uuid = UUID.randomUUID();
        new ChemicalSubstanceBuilder()

                .setStructure("COC1=CC=C(O)C2=C(O)C(C)=C3OC(C)(O)C(=O)C3=C12")
                .addName("Test")
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);

        UUID uuid2 = UUID.randomUUID();
        new ChemicalSubstanceBuilder()

                .setStructure("CC1=C2OC(C)(O)C(=O)C2=C3C4=C(C=C(O)C3=C1O)N5C=CC=CC5=N4")
                .addName("Test2")
                .setUUID(uuid2)
                .buildJsonAnd(this::assertCreated);

        StructureIndexer.ResultEnumeration result = indexer.substructure("[#7,#8]c1ccc(O)c2c(O)c([#6])c3OC([#6])(O)C(=O)c3c12");
        assertTrue(result.hasMoreElements());
        Set<UUID> matches = new LinkedHashSet<>();
        while(result.hasMoreElements()){
            matches.add(UUID.fromString(result.nextElement().getId()));
        }
        assertEquals(new LinkedHashSet<>(Arrays.asList(uuid)), matches);
    }

    @Test
    @WithMockUser(value = "admin", roles = "Admin")
    public void explicitHShouldWork() throws Exception {


        UUID uuid = UUID.randomUUID();
        new ChemicalSubstanceBuilder()

                .setStructure("C(=CC=C1)C=C1")
                .addName("Test")
                .setUUID(uuid)
                .buildJsonAnd(this::assertCreated);


        StructureIndexer.ResultEnumeration result = indexer.substructure("C(=C(C(=C1[H])[H])[H])(C(=C1[H])[H])[H]");
        assertTrue(result.hasMoreElements());
        assertEquals(uuid.toString(), result.nextElement().getId());
        assertFalse(result.hasMoreElements());
    }
}
