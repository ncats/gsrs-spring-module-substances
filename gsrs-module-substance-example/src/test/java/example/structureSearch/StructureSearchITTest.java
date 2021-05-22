package example.structureSearch;

import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gov.nih.ncats.structureIndexer.StructureIndexer;
import gsrs.legacy.structureIndexer.StructureIndexerEventListener;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.services.SubstanceStructureSearchService;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StructureSearchITTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private SubstanceStructureSearchService structureSearchService;
    @Autowired
    private StructureIndexerService indexer;
    @Autowired
    private StructureIndexerEventListener eventListener;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @BeforeEach
    public void clearIndexers() throws IOException {
        indexer.removeAll();
    }

    @Test
    @WithMockUser(value = "admin" , roles = "Admin")
    public void saveChemicalAndSearchForStructureShouldFind() throws Exception {
        String structure="C1CCCCC1";
        UUID uuid = UUID.randomUUID();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s->{
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
    @WithMockUser(value = "admin" , roles = "Admin")
    public void saveMultipleChemicalsSearchShouldOnlyFindMatch() throws Exception {
        String structure="C1CCCCC1";
        UUID uuid = UUID.randomUUID();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s->{
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
}
