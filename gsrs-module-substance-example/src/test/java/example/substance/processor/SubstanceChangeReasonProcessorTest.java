package example.substance.processor;


import gsrs.module.substance.processors.RelationshipProcessor;
import gsrs.module.substance.processors.SubstanceProcessor;
import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.repository.GroupRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.services.PrincipalService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestEntityProcessorFactory;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SubstanceChangeReasonProcessorTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private TestEntityProcessorFactory entityProcessorFactory;

    @MockBean
    private RelationshipProcessor relationshipProcessor;

    @Autowired
    private TestEntityManager em;
    @BeforeEach
    public void addEntityProcessor(){
        SubstanceProcessor substanceProcessor = new SubstanceProcessor();
        AutowireHelper.getInstance().autowire(substanceProcessor);
        entityProcessorFactory.setEntityProcessors(substanceProcessor);


    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void newRecordShouldHaveChangeReasonSetToNull(){

            UUID uuid = UUID.randomUUID();
             new SubstanceBuilder()
                    .setUUID(uuid)
                    .addName("myName")
                    .andThen(s -> {s.changeReason ="A Change Reason"; return s;} )
                    .buildJsonAnd(this::assertCreated);

            Substance saved = this.substanceEntityService.get(uuid).get();


            assertNull(saved.changeReason);

    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void updatedRecordShouldHaveChangeReason() {
        UUID uuid = UUID.randomUUID();
         new SubstanceBuilder()
                .setUUID(uuid)
                .addName("myName")

                .buildJsonAnd(this::assertCreated);




        this.substanceEntityService.get(uuid).get().toBuilder()
                .andThen(s -> {s.changeReason ="testing change"; return s;} )
                .buildJsonAnd(this::assertUpdated);

        Substance fetched2 = this.substanceEntityService.get(uuid).get();

        assertEquals("testing change", fetched2.changeReason);

    }
}
