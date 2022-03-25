package example.substance.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.JsonNode;

import gov.nih.ncats.common.sneak.Sneak;
import gsrs.module.substance.repository.KeywordRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.chem.StructureProcessor;
import ix.core.controllers.EntityFactory;
import ix.core.models.Structure;
import ix.ginas.modelBuilders.ChemicalSubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Moiety;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.SubstanceReference;
import ix.ginas.utils.validation.ChemicalDuplicateFinder;
@WithMockUser(username = "admin", roles="Admin")
//@Disabled("substance repository query doesn't work yet")
public class DuplicateChemicalStructureFinderTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private ChemicalDuplicateFinder sut;

    @Autowired
    private StructureProcessor structureProcessor;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private SubstanceRepository substanceRepository;
    private int nameCounter=0;

    @BeforeEach
    public void resetNameCounter(TestInfo info){
        nameCounter=0;
        System.out.println("Starting next test:" + info.getDisplayName());
    }

    @Test
    public void noRecordsLoadedShouldNotFindAnyDups(){
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance s = new ChemicalSubstanceBuilder()
                                .setUUID(uuid)
                                .setStructureWithDefaultReference("C1CC=CC=C1")
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
                .setStructureWithDefaultReference("C1CC=CC=C1")
                .addName("a name")
                .build();
        //have to structure process first to generate hashes

        s.getStructure().updateStructureFields(structureProcessor.instrument(s.getStructure().toChemical(), true));


        JsonNode json = EntityFactory.EntityMapper.JSON_DIFF_ENTITY_MAPPER().toJsonNode(s);
        assertCreated(json);


        assertTrue(keywordRepository.count() > 0);


        ChemicalSubstance s2 = new ChemicalSubstanceBuilder()

                .setStructureWithDefaultReference("C1CC=CC=C1")
                .addName("different name")
                .build();
        //have to structure process first to generate hashes
        s2.getStructure().updateStructureFields( structureProcessor.instrument(s2.getStructure().toChemical(), true));

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference());
        assertEquals(Arrays.asList(uuid.toString()),
                possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toList()));
    }
    @Test
    public void loadOneRecordWithMoieityAndSearchForItShouldFindIt() throws IOException {
        UUID uuid = createAndPersistChemicalSubstanceWithMoietyStructures("[Na+][Cl-]", "C1CC=CC=C1");


        assertTrue(keywordRepository.count() > 0);


        ChemicalSubstance s2 = new ChemicalSubstanceBuilder()

                .setStructureWithDefaultReference("C1CC=CC=C1")
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
                .setStructureWithDefaultReference("C1CC=CC=C1")
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
    public void searchForOurselvesMoeityShouldNotFindDuplicate(){
        UUID uuid = createAndPersistChemicalSubstanceWithMoietyStructures("[Na+][Cl-]", "C1CC=CC=C1");



        assertTrue(keywordRepository.count() > 0);



        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(
                substanceRepository.findSummaryByUuid(uuid).get().toSubstanceReference());
        assertTrue(possibleDuplicatesFor.isEmpty());
    }

    @Test
    public void searchForDifferentRecordShouldNotFindIt(){
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance s = new ChemicalSubstanceBuilder()
                .setUUID(uuid)
                .setStructureWithDefaultReference("C1CC=CC=C1")
                .addName("a name")
                .build();
        Structure structure = structureProcessor.instrument(s.getStructure().toChemical(), true);
        s.getStructure().properties = structure.properties;

        JsonNode json = EntityFactory.EntityMapper.JSON_DIFF_ENTITY_MAPPER().toJsonNode(s);
        Substance saved = assertCreated(json);

        ChemicalSubstance s2 = new ChemicalSubstanceBuilder()

                .setStructureWithDefaultReference("[Na+].[Cl-]")
                .addName("different structure")
                .build();
        //have to structure process first to generate hashes
        Structure structure2 = structureProcessor.instrument(s2.getStructure().toChemical(), true);
        s2.getStructure().properties = structure2.properties;

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference());
        assertTrue(possibleDuplicatesFor.isEmpty());
    }

    private UUID createAndPersistChemicalSubstanceWithStructure(String smiles){
        ChemicalSubstance s = createChemicalSubstanceWithStructure(smiles);


        JsonNode json = EntityFactory.EntityMapper.JSON_DIFF_ENTITY_MAPPER().toJsonNode(s);
        assertCreated(json);
        return s.uuid;
    }

    private ChemicalSubstance createChemicalSubstanceWithStructure(String smiles) {
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance s = new ChemicalSubstanceBuilder()
                .setUUID(uuid)
                .setStructureWithDefaultReference(smiles)
                .addName("a name" + (++nameCounter))
                .build();
        //have to structure process first to generate hashes

        s.getStructure().updateStructureFields(structureProcessor.instrument(s.getStructure().toChemical(), true));
        return s;
    }
    private UUID createAndPersistChemicalSubstanceWithMoietyStructures(String smiles, String... moietySmiles){
        try {
            ChemicalSubstance s = createChemicalSubstanceWithMoietyStructures(smiles, moietySmiles);
            JsonNode json = EntityFactory.EntityMapper.JSON_DIFF_ENTITY_MAPPER().toJsonNode(s);
            assertCreated(json);
            return s.uuid;
        } catch (IOException e) {
            return Sneak.sneakyThrow(e);
        }



    }
    private ChemicalSubstance createChemicalSubstanceWithMoietyStructures(String smiles, String... moietySmiles) throws IOException {
        UUID uuid = UUID.randomUUID();

        ChemicalSubstanceBuilder builder = new ChemicalSubstanceBuilder()
                .setUUID(uuid)
                .setStructureWithDefaultReference(smiles)
                .addName("a name" + (++nameCounter));

        for(String ms : moietySmiles){
            builder.addMoietyWithStructureAndDefaultReference(ms);
        }
        ChemicalSubstance s=builder.build();
        //have to structure process first to generate hashes

        s.getStructure().updateStructureFields(structureProcessor.instrument(s.getStructure().toChemical(), true));
        for(Moiety m : s.getMoieties()){
            m.structure.updateStructureFields(structureProcessor.instrument(m.structure.toChemical(), true));
        }
        return s;
    }

    @Test
    public void load5RecordsAndSearchForItShouldFindThem(){
        String smiles ="C1CC=CC=C1";
        Set<String> uuids = IntStream.range(0, 5)
                                    .mapToObj(i-> createAndPersistChemicalSubstanceWithStructure(smiles).toString())
                                    .collect(Collectors.toSet());




        assertTrue(keywordRepository.count() > 0);


        ChemicalSubstance s2 = createChemicalSubstanceWithStructure(smiles);

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference());
        Set<String> results = possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toSet());
        assertEquals(5, results.size());
        assertEquals(uuids, results);
    }

    @Test
    public void load5RecordsMoeitiesAndSearchForItShouldFindThem(){
        String smiles ="C1CC=CC=C1";
        Set<String> uuids = IntStream.range(0, 5)
                .mapToObj(i-> createAndPersistChemicalSubstanceWithMoietyStructures("[Na+][Cl-]", smiles).toString())
                .collect(Collectors.toSet());




        assertTrue(keywordRepository.count() > 0);


        ChemicalSubstance s2 = createChemicalSubstanceWithStructure(smiles);

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference());
        Set<String> results = possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toSet());
        assertEquals(5, results.size());
        assertEquals(uuids, results);
    }

    @Test
    public void load50RecordsMoeitiesAndSearchForItShouldFindAskedForMaxCount(){
        String smiles ="C1CC=CC=C1";
        Set<String> uuids = IntStream.range(0, 500)
                .mapToObj(i-> createAndPersistChemicalSubstanceWithMoietyStructures("[Na+][Cl-]", smiles).toString())
                .collect(Collectors.toSet());




        assertTrue(keywordRepository.count() > 0);


        ChemicalSubstance s2 = createChemicalSubstanceWithStructure(smiles);

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference(), 20);
        Set<String> results = possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toSet());
        assertEquals(20, results.size());
        results.forEach( uuid-> assertTrue(uuids.contains(uuid)));
    }

    @Test
    public void load1100RecordsMoeitiesAndSearchForItShouldFindAskedForMaxCount(){
        String smiles ="C1CC=CC=C1";
        Set<String> uuids = IntStream.range(0, 1100)
                .mapToObj(i-> createAndPersistChemicalSubstanceWithMoietyStructures("[Na+][Cl-]", smiles).toString())
                .collect(Collectors.toSet());




        assertTrue(keywordRepository.count() > 0);


        ChemicalSubstance s2 = createChemicalSubstanceWithStructure(smiles);

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference(), 20);
        Set<String> results = possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toSet());
        assertEquals(20, results.size());
        results.forEach( uuid-> assertTrue(uuids.contains(uuid)));
    }

    @Test
    public void load5RecordsAndSearchForMaxOf3ShouldReturnOnly3(){
        String smiles ="C1CC=CC=C1";
        Set<String> uuids = IntStream.range(0, 5)
                .mapToObj(i-> createAndPersistChemicalSubstanceWithStructure(smiles).toString())
                .collect(Collectors.toSet());




        assertTrue(keywordRepository.count() > 0);


        ChemicalSubstance s2 = createChemicalSubstanceWithStructure(smiles);

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference(), 3);
        Set<String> results = possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toSet());
        assertEquals(3, results.size());
        results.forEach( uuid-> assertTrue(uuids.contains(uuid)));
    }

    @Test
    public void load50RecordsAndSearchForMaxOf3ShouldReturnOnly3(){
        String smiles ="C1CC=CC=C1";
        Set<String> uuids = IntStream.range(0, 50)
                .mapToObj(i-> createAndPersistChemicalSubstanceWithStructure(smiles).toString())
                .collect(Collectors.toSet());




        assertTrue(keywordRepository.count() > 0);


        ChemicalSubstance s2 = createChemicalSubstanceWithStructure(smiles);

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference(), 3);
        Set<String> results = possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toSet());
        assertEquals(3, results.size());
        results.forEach( uuid-> assertTrue(uuids.contains(uuid)));
    }

    @Test
    public void load500RecordsAndSearchForMaxOf30ShouldReturnOnly30(){
        String smiles ="C1CC=CC=C1";
        Set<String> uuids = IntStream.range(0, 500)
                .mapToObj(i-> createAndPersistChemicalSubstanceWithStructure(smiles).toString())
                .collect(Collectors.toSet());




        assertTrue(keywordRepository.count() > 0);


        ChemicalSubstance s2 = createChemicalSubstanceWithStructure(smiles);

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference(), 30);
        Set<String> results = possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toSet());
        assertEquals(30, results.size());
        results.forEach( uuid-> assertTrue(uuids.contains(uuid)));
    }

    @Test
    public void load2000RecordsAndSearchForMaxOf300ShouldReturnOnly300(){
        String smiles ="C1CC=CC=C1";
        Set<String> uuids = IntStream.range(0, 2000)
                .mapToObj(i-> createAndPersistChemicalSubstanceWithStructure(smiles).toString())
                .collect(Collectors.toSet());




        assertTrue(keywordRepository.count() > 0);


        ChemicalSubstance s2 = createChemicalSubstanceWithStructure(smiles);

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference(), 300);
        Set<String> results = possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toSet());
        assertEquals(300, results.size());
        results.forEach( uuid-> assertTrue(uuids.contains(uuid)));
    }
}
