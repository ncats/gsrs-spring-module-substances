package example.structureSearch;

import gov.nih.ncats.structureIndexer.StructureIndexer;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.indexer.IndexRemoveEntityEvent;
import gsrs.indexer.IndexUpdateEntityEvent;
import gsrs.legacy.structureIndexer.LegacyStructureIndexerService;
import gsrs.legacy.structureIndexer.StructureIndexerEventListener;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

public class BasicLegacyIndexerTest {

    @TempDir
    protected File tempDir;

    private LegacyStructureIndexerService indexer;

    private StructureIndexerEventListener eventListener;
    @BeforeEach
    public void setupIndexer() throws IOException {
        indexer = new LegacyStructureIndexerService(tempDir);
        eventListener = new StructureIndexerEventListener(indexer);
    }
    @AfterEach
    public void shutdown(){
        indexer.shutdown();
    }

    @Test
    public void noRecordsShouldReturnEmptyResults() throws Exception{
        StructureIndexer.ResultEnumeration resultEnumeration = indexer.substructure("C1CCCCC1");
        assertFalse(resultEnumeration.hasMoreElements());

    }
    @Test
    public void createChemicalSubstanceAddsStructureToIndex() throws Exception{

        String structure="C1CCCCC1";
        ChemicalSubstance cs=new ChemicalSubstanceBuilder()

                .setStructure(structure)
                .addName("Test")
                .generateNewUUID()
                .build();

        eventListener.onCreate(new IndexCreateEntityEvent(EntityUtils.EntityWrapper.of(cs)));

        StructureIndexer.ResultEnumeration resultEnumeration = indexer.substructure(structure);
        assertTrue(resultEnumeration.hasMoreElements());

        assertEquals(cs.uuid.toString(), resultEnumeration.nextElement().getId());
        assertFalse(resultEnumeration.hasMoreElements());

    }

    @Test
    public void createMultipleStructuresOnlyShouldFindMatch() throws Exception{

        String structure="C1CCCCC1";
        ChemicalSubstance cs=new ChemicalSubstanceBuilder()

                .setStructure(structure)
                .addName("Test")
                .generateNewUUID()
                .build();

        eventListener.onCreate(new IndexCreateEntityEvent(EntityUtils.EntityWrapper.of(cs)));

        eventListener.onCreate(new IndexCreateEntityEvent(EntityUtils.EntityWrapper.of(new ChemicalSubstanceBuilder()

                .setStructure("[Na+].[Cl-]")
                .addName("somethingElse")
                .generateNewUUID()
                .build())));

        StructureIndexer.ResultEnumeration resultEnumeration = indexer.substructure(structure);
        assertTrue(resultEnumeration.hasMoreElements());

        assertEquals(cs.uuid.toString(), resultEnumeration.nextElement().getId());
        assertFalse(resultEnumeration.hasMoreElements());

    }

    @Test
    public void updateStructureShouldNotFindOldStructureAnymore() throws Exception{

        String structure="C1CCCCC1";
        ChemicalSubstance cs=new ChemicalSubstanceBuilder()

                .setStructure(structure)
                .addName("Test")
                .generateNewUUID()
                .build();

        eventListener.onCreate(new IndexCreateEntityEvent(EntityUtils.EntityWrapper.of(cs)));

        eventListener.onUpdate(new IndexUpdateEntityEvent(EntityUtils.EntityWrapper.of(cs.toChemicalBuilder()
                .setStructure("[Na+].[Cl-]")
                .build())));

        StructureIndexer.ResultEnumeration resultEnumeration = indexer.substructure(structure);
        assertFalse(resultEnumeration.hasMoreElements());

    }

    @Test
    public void removeChemicalSubstanceShouldNotBeInResultsAnymore() throws Exception{

        String structure="C1CCCCC1";
        ChemicalSubstance cs=new ChemicalSubstanceBuilder()

                .setStructure(structure)
                .addName("Test")
                .generateNewUUID()
                .build();

        eventListener.onCreate(new IndexCreateEntityEvent(EntityUtils.EntityWrapper.of(cs)));


        eventListener.onRemove(new IndexRemoveEntityEvent(EntityUtils.EntityWrapper.of(cs)));


        StructureIndexer.ResultEnumeration resultEnumeration = indexer.substructure(structure);
        assertFalse(resultEnumeration.hasMoreElements());

    }
}
