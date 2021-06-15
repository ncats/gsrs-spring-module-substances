package example.substance.validation;

import example.substance.AbstractSubstanceJpaEntityTest;
import ix.core.chem.StructureProcessor;
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
@Disabled("substance repository query doesn't work yet")
public class DuplicateChemicalStructureFinderTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    ChemicalDuplicateFinder sut;

    @Autowired
    StructureProcessor structureProcessor;

    @Test
    public void noRecordsLoadedShouldNotFindAnyDups(){
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance s = new ChemicalSubstanceBuilder()
                                .setUUID(uuid)
                                .setStructure("c1ccccc1")
                                .addName("a name")
                                .build();
        //have to structure process first to generate hashes
        Structure structure = structureProcessor.instrument(s.getStructure().toChemical(), true);
        s.getStructure().properties = structure.properties;

        assertTrue(sut.findPossibleDuplicatesFor(s.asSubstanceReference()).isEmpty());
    }

    @Test
    public void loadOneRecordAndSearchForItShouldFindIt(){
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance s = new ChemicalSubstanceBuilder()
                .setUUID(uuid)
                .setStructure("c1ccccc1")
                .addName("a name")
                .build();

        assertCreated(s.toFullJsonNode());

        ChemicalSubstance s2 = new ChemicalSubstanceBuilder()

                .setStructure("c1ccccc1")
                .addName("different name")
                .build();
        //have to structure process first to generate hashes
        Structure structure = structureProcessor.instrument(s2.getStructure().toChemical(), true);
        s2.getStructure().properties = structure.properties;

        List<SubstanceReference> possibleDuplicatesFor = sut.findPossibleDuplicatesFor(s2.asSubstanceReference());
        assertEquals(Arrays.asList(uuid.toString()),
                possibleDuplicatesFor.stream().map(r -> r.refuuid).collect(Collectors.toList()));
    }

    @Test
    public void searchForDifferentRecordShouldNotFindIt(){
        UUID uuid = UUID.randomUUID();
        ChemicalSubstance s = new ChemicalSubstanceBuilder()
                .setUUID(uuid)
                .setStructure("c1ccccc1")
                .addName("a name")
                .build();
        Structure structure = structureProcessor.instrument(s.getStructure().toChemical(), true);
        s.getStructure().properties = structure.properties;

        Substance saved = assertCreated(s.toFullJsonNode());

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
