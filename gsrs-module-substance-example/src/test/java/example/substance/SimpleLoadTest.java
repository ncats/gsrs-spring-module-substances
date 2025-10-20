package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.common.sneak.Sneak;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubber;
import gsrs.module.substance.scrubbers.basic.BasicSubstanceScrubberParameters;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.core.models.Keyword;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode  = objectMapper.valueToTree(testConcept);
        ((ObjectNode)jsonNode).put("uuid", UUID.randomUUID().toString());
        try {
            substanceEntityService.createEntity(jsonNode);
        } catch (Exception e){
            e.printStackTrace();
        }
        assertEquals(1, substanceRepository.count());
    }
}
