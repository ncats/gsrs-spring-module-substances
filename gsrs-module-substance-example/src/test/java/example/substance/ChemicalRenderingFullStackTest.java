package example.substance;

import example.GsrsModuleSubstanceApplication;
import gsrs.module.substance.controllers.SubstanceController;
import gsrs.module.substance.processors.RelationEventListener;
import gsrs.substances.tests.AbstractSubstanceJpaFullStackEntityTest;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({ChemicalRenderingFullStackTest.Configuration.class, RelationEventListener.class})
@WithMockUser(username = "admin", roles="Admin")
public class ChemicalRenderingFullStackTest  extends AbstractSubstanceJpaFullStackEntityTest {



    @Autowired
    protected SubstanceController substanceController;



    @TestConfiguration
    public static class Configuration{
        //TODO

    }

    private String getSVGFrom(UUID id,String version) throws Exception {
        byte[] bod2=(byte[]) ((ResponseEntity)substanceController.render(id.toString(), "svg", version, false, null, 200,
                null, null, null, null, null,
                false, null))
                .getBody();
        String xml = Arrays.stream(new String(bod2).split("\n"))
                //remove description from svg which contains timestamp
                .filter(dd->!dd.contains("<desc>"))
                .collect(Collectors.joining("\n"));
        return xml;
    }


    @Test
    public void changeStructureAndRetrieveOldRenderedVersionShouldBeSameAsOriginal() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        new SubstanceBuilder()
        .asChemical()
        .setStructureWithDefaultReference("CCCC")
        .addName("chem1")
        .setUUID(uuid1)
        .buildJsonAnd(this::assertCreatedAPI);
        String xml1=getSVGFrom(uuid1,null);

        //        
        substanceEntityService.get(uuid1)
        .map(s->(ChemicalSubstance)s)
        .get()
        .toChemicalBuilder()
        .addName("chem syn")
        .buildJsonAnd(this::assertUpdatedAPI);
        String xml2=getSVGFrom(uuid1,null);


        assertEquals(xml1,xml2, "only adding a name shouldn't change the structure image");

        substanceEntityService.get(uuid1)
        .map(s->(ChemicalSubstance)s)
        .get()
        .toChemicalBuilder()
        .setStructureWithDefaultReference("CCOCC")
        .buildJsonAnd(this::assertUpdatedAPI);

        String xml3=getSVGFrom(uuid1,null);
        assertNotEquals(xml1,xml3, "Changing structure should change image");
        String xml1v=getSVGFrom(uuid1,"1");
        assertEquals(xml1,xml1v, "Old version should be the same");
    }


}
