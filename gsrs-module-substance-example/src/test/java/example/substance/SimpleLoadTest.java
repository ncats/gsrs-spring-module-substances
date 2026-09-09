package example.substance;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.models.Keyword;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("fullstack")
public class SimpleLoadTest extends AbstractSubstanceJpaEntityTest {

    // Draft, trying to make a test that does a load more simply so we can debug without extraneous procedures. 

    @Test
    @WithMockUser(username = "admin", roles = "Admin")
    public void simpleLoad1() {
        SubstanceBuilder substanceBuilder = new SubstanceBuilder();
        Reference publicReference = new Reference();
        publicReference.publicDomain = true;
        publicReference.citation = "something public";
        publicReference.docType = "OTHER";
        publicReference.makePublicReleaseReference();
        Name openName = new Name();
        openName.name = "Open Name";
        openName.stdName = "oPeN nAmE";
        openName.languages.add(new Keyword("en"));
        openName.addReference(publicReference);
        substanceBuilder.addName(openName);
        substanceBuilder.addReference(publicReference);
        Substance testConcept = substanceBuilder.build();
        // testConcept.uuid = UUID.randomUUID();
        JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();
        JsonNode jsonNode  = mapper.valueToTree(testConcept);
        ((ObjectNode)jsonNode).put("uuid", UUID.randomUUID().toString());
        try {
            substanceEntityService.createEntity(jsonNode);
        } catch (Exception e){
            e.printStackTrace();
        }
        assertEquals(1, substanceRepository.count());
    }
}
