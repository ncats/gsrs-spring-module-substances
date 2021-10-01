package example.substance;

import gsrs.module.substance.processors.*;
import gsrs.module.substance.services.RelationshipService;
import gsrs.repository.EditRepository;
import gsrs.services.EditEventService;
import gsrs.services.PrincipalService;
import gsrs.startertests.TestEntityProcessorFactory;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@ActiveProfiles("test")
@RecordApplicationEvents
@Import({RelationshipInvertFullStackTest.Configuration.class, RelationEventListener.class})
@WithMockUser(username = "admin", roles="Admin")
public class RelationshipInvertFullStackTest  extends AbstractSubstanceJpaFullStackEntityTest {

    File invrelate1, invrelate2;

    @Autowired
    private TestEntityProcessorFactory testEntityProcessorFactory;

    @SpyBean
    private SubstanceProcessor substanceProcessor;
    @SpyBean
    private RelationshipProcessor relationshipProcessor;
    @SpyBean
    private ReferenceProcessor referenceProcessor;

    @Autowired
    private EditRepository editRepository;


    @Autowired
    private PrincipalService principalService;


    @SpyBean
    private RelationshipService relationshipService;

    @Autowired
    private EditEventService editEventService;



    @TestConfiguration
    public static class Configuration{
        @Bean
        public RelationshipProcessor relationshipProcessor(){
            return new RelationshipProcessor();
        }

        @Bean
        public ReferenceProcessor referenceProcessor(){
            return new ReferenceProcessor();
        }

        @Bean
        public SubstanceProcessor substanceProcessor(){
            return new SubstanceProcessor();
        }
    }

    @BeforeEach
    public void setup() throws IOException {
        invrelate1 = new ClassPathResource("testJSON/invrelate1.json").getFile();
        invrelate2 = new ClassPathResource("testJSON/invrelate2.json").getFile();



        testEntityProcessorFactory.addEntityProcessor(substanceProcessor);
        testEntityProcessorFactory.addEntityProcessor(relationshipProcessor);
        testEntityProcessorFactory.addEntityProcessor(referenceProcessor);

    }

    @Test
    public void addSubstanceWithRelationshipThenAddRelatedSubstanceShouldResultInBirectionalRelationship()   throws Exception {

        UUID uuid1 = UUID.randomUUID();

        UUID uuid2 = UUID.randomUUID();
        Substance substance2 = new SubstanceBuilder()
                .addName("sub2")
                .setUUID(uuid2)
                .build();
        //submit primary, with dangling relationship
        new SubstanceBuilder()
                .addName("sub1")
                .setUUID(uuid1)
                .addRelationshipTo(substance2, "foo->bar")
                .buildJsonAnd(this::assertCreated);

        Substance originalFetchedSubstance = substanceEntityService.get(uuid1).get();
        assertEquals("1", originalFetchedSubstance.version);
        //now submit with one sided reference, processors should add the other side.
        assertCreated(substance2.toFullJsonNode());


        Mockito.verify(relationshipService, Mockito.times(1)).createNewInverseRelationshipFor(Mockito.any(TryToCreateInverseRelationshipEvent.class));

        Substance fetchedSubstance2 = substanceEntityService.get(uuid2).get();
        Relationship relationshipA = fetchedSubstance2.relationships.get(0);

        //confirm that the dangled has a relationship to the dangler

        assertEquals(uuid1.toString(), relationshipA.relatedSubstance.refuuid);
        assertEquals("bar->foo", relationshipA.type);

        assertEquals("2", fetchedSubstance2.version);

        Substance fetchedSubstance1 = substanceEntityService.get(uuid1).get();
        assertEquals("1", fetchedSubstance1.version);
        assertEquals(1, fetchedSubstance1.relationships.size());


    }
}
