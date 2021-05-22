package example.structureSearch;

import com.fasterxml.jackson.databind.JsonNode;
import example.substance.AbstractSubstanceJpaEntityTest;
import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gov.nih.ncats.structureIndexer.StructureIndexer;
import gsrs.indexer.IndexCreateEntityEvent;
import gsrs.legacy.structureIndexer.LegacyStructureIndexerService;
import gsrs.legacy.structureIndexer.StructureIndexerEventListener;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.module.substance.services.SubstanceStructureSearchService;
import gsrs.service.GsrsEntityService;
import gsrs.startertests.GsrsFullStackTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@RecordApplicationEvents
//@Import(LegacyStructureIndexerService.class)
public class StructureSearchITTest extends AbstractSubstanceJpaFullStackEntityTest {

    @Autowired
    private SubstanceStructureSearchService structureSearchService;
    @Autowired
    private StructureIndexerService indexer;
    @Autowired
    private StructureIndexerEventListener eventListener;

    @Autowired
    private SubstanceEntityService substanceEntityService;

    @Test
    @WithMockUser(value = "admin" , roles = "Admin")
    public void saveChemicalAndSearchForStructureShouldFind(@Autowired ApplicationEvents applicationEvents) throws Exception {
        String structure="C1CCCCC1";
        UUID uuid = UUID.randomUUID();

        applicationEvents.clear();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(s->{
            new ChemicalSubstanceBuilder()

                    .setStructure(structure)
                    .addName("Test")
                    .setUUID(uuid)
                    .buildJsonAnd(this::assertCreated);
        });


        List<IndexCreateEntityEvent> indexCreateEntityEventList = applicationEvents.stream(IndexCreateEntityEvent.class).collect(Collectors.toList());
        assertFalse(indexCreateEntityEventList.isEmpty());

        StructureIndexer.ResultEnumeration result = indexer.substructure(structure);
        assertTrue(result.hasMoreElements());
        assertEquals(uuid.toString(), result.nextElement().getId());
        assertFalse(result.hasMoreElements());
    }
}
