package example.substance.validation;

import com.fasterxml.jackson.databind.JsonNode;
import example.substance.AbstractSubstanceJpaEntityTest;
import example.substance.AbstractSubstanceJpaFullStackEntityTest;
import gsrs.module.substance.repository.KeywordRepository;
import ix.core.chem.StructureProcessor;
import ix.core.controllers.EntityFactory;
import ix.core.models.Keyword;
import ix.core.models.Structure;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.ChemicalDuplicateFinder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
@WithMockUser(username = "admin", roles="Admin")
//@Disabled("substance repository query doesn't work yet")
public class DuplicateChemicalStructureFinderTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    ChemicalDuplicateFinder sut;

    @Autowired
    StructureProcessor structureProcessor;

    @Autowired
    KeywordRepository keywordRepository;

    @Test
    public void noRecordsLoadedShouldNotFindAnyDups(){
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance s = new ChemicalSubstanceBuilder()
                                .setUUID(uuid)
                                .setStructure("C1CC=CC=C1")
                                .addName("a name")
                                .build();
        //have to structure process first to generate hashes
        Structure structure = structureProcessor.instrument(s.getStructure().toChemical(), true);
        s.getStructure().updateStructureFields(structure);

        assertTrue(sut.findPossibleDuplicatesFor(s.asSubstanceReference()).isEmpty());
    }

    @Test
    public void loadOneRecordAndSearchForItShouldFindIt(){
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance s = new ChemicalSubstanceBuilder()
                .setUUID(uuid)
                .setStructure("C1CC=CC=C1")
                .addName("a name")
                .build();
        //have to structure process first to generate hashes

        s.getStructure().updateStructureFields(structureProcessor.instrument(s.getStructure().toChemical(), true));


        JsonNode json = EntityFactory.EntityMapper.JSON_DIFF_ENTITY_MAPPER().toJsonNode(s);
        assertCreated(json);


        assertTrue(keywordRepository.count() > 0);

        List<Keyword> all = keywordRepository.findAll();
        for(Keyword k : all){
            System.out.println(k);
        }
        ChemicalSubstance s2 = new ChemicalSubstanceBuilder()

                .setStructure("C1CC=CC=C1")
                .addName("different name")
                .build();
        //have to structure process first to generate hashes
        s2.getStructure().updateStructureFields( structureProcessor.instrument(s2.getStructure().toChemical(), true));

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference());
        assertEquals(Arrays.asList(uuid.toString()),
                possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toList()));
    }
    @Test
    public void searchForOurselvesShouldNotFindDuplicate(){
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance s = new ChemicalSubstanceBuilder()
                .setUUID(uuid)
                .setStructure("C1CC=CC=C1")
                .addName("a name")
                .build();
        //have to structure process first to generate hashes

        s.getStructure().updateStructureFields(structureProcessor.instrument(s.getStructure().toChemical(), true));


        JsonNode json = EntityFactory.EntityMapper.JSON_DIFF_ENTITY_MAPPER().toJsonNode(s);
        Substance saved = assertCreated(json);


        assertTrue(keywordRepository.count() > 0);



        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(saved.asSubstanceReference());
        assertTrue(possibleDuplicatesFor.isEmpty());
    }

    @Test
    public void searchForDifferentRecordShouldNotFindIt(){
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance s = new ChemicalSubstanceBuilder()
                .setUUID(uuid)
                .setStructure("C1CC=CC=C1")
                .addName("a name")
                .build();
        Structure structure = structureProcessor.instrument(s.getStructure().toChemical(), true);
        s.getStructure().properties = structure.properties;

        JsonNode json = EntityFactory.EntityMapper.JSON_DIFF_ENTITY_MAPPER().toJsonNode(s);
        Substance saved = assertCreated(json);

        ChemicalSubstance s2 = new ChemicalSubstanceBuilder()

                .setStructure("[Na+].[Cl-]")
                .addName("different structure")
                .build();
        //have to structure process first to generate hashes
        Structure structure2 = structureProcessor.instrument(s2.getStructure().toChemical(), true);
        s2.getStructure().properties = structure2.properties;

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference());
        assertTrue(possibleDuplicatesFor.isEmpty());
    }

}
