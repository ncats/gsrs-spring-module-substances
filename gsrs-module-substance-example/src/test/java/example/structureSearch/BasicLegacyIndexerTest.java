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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)

public class BasicLegacyIndexerTest {

    @TempDir
    protected File tempDir;

    private LegacyStructureIndexerService indexer;

    private StructureIndexerEventListener eventListener;
    @Mock
    private EntityManager mockEntityManager;

    @BeforeEach
    public void setupIndexer() throws IOException {
        indexer = new LegacyStructureIndexerService(tempDir);
        eventListener = new StructureIndexerEventListener(indexer, mockEntityManager);
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

        Mockito.when(mockEntityManager.find(ChemicalSubstance.class, cs.uuid)).thenReturn(cs);
        eventListener.onCreate(new IndexCreateEntityEvent(EntityUtils.EntityWrapper.of(cs).getKey()));

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

        Mockito.when(mockEntityManager.find(ChemicalSubstance.class, cs.uuid)).thenReturn(cs);
        eventListener.onCreate(new IndexCreateEntityEvent(EntityUtils.EntityWrapper.of(cs).getKey()));

        ChemicalSubstance cs2 = new ChemicalSubstanceBuilder()

                .setStructure("[Na+].[Cl-]")
                .addName("somethingElse")
                .generateNewUUID()
                .build();

        Mockito.when(mockEntityManager.find(ChemicalSubstance.class, cs2.uuid)).thenReturn(cs2);
        eventListener.onCreate(new IndexCreateEntityEvent(EntityUtils.EntityWrapper.of(cs2).getKey()));

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

        Mockito.when(mockEntityManager.find(ChemicalSubstance.class, cs.uuid)).thenReturn(cs);
        eventListener.onCreate(new IndexCreateEntityEvent(EntityUtils.EntityWrapper.of(cs).getKey()));

        ChemicalSubstance updatedCs = cs.toChemicalBuilder()
                .setStructure("[Na+].[Cl-]")
                .build();

        Mockito.when(mockEntityManager.find(ChemicalSubstance.class, cs.uuid)).thenReturn(updatedCs);

        eventListener.onUpdate(new IndexUpdateEntityEvent(EntityUtils.EntityWrapper.of(updatedCs).getKey()));

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

        Mockito.when(mockEntityManager.find(ChemicalSubstance.class, cs.uuid)).thenReturn(cs);
        eventListener.onCreate(new IndexCreateEntityEvent(EntityUtils.EntityWrapper.of(cs).getKey()));


        eventListener.onRemove(new IndexRemoveEntityEvent(EntityUtils.EntityWrapper.of(cs)));


        StructureIndexer.ResultEnumeration resultEnumeration = indexer.substructure(structure);
        assertFalse(resultEnumeration.hasMoreElements());

    }
}
