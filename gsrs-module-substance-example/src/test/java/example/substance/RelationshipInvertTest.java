package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.events.CreateEditEvent;
import gsrs.junit.json.ChangeFilter;
import gsrs.junit.json.Changes;
import gsrs.junit.json.JsonUtil;
import gsrs.module.substance.processors.*;
import gsrs.module.substance.services.RelationshipService;
import gsrs.repository.EditRepository;
import gsrs.services.EditEventService;
import gsrs.startertests.TestEntityProcessorFactory;
import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import gsrs.substances.tests.SubstanceJsonUtil;
import ix.core.models.Edit;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;

//@GsrsJpaTest
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({RelationshipInvertTest.Configuration.class, RelationEventListener.class})
@WithMockUser(username = "admin", roles="Admin")
public class RelationshipInvertTest extends AbstractSubstanceJpaEntityTest {

    File invrelate1, invrelate2;

    @Autowired
    private TestEntityProcessorFactory testEntityProcessorFactory;

    @Autowired
    private SubstanceProcessor substanceProcessor;
    @Autowired
    private RelationshipProcessor relationshipProcessor;
    @Autowired
    private ReferenceProcessor referenceProcessor;

    @Autowired
    private EditRepository editRepository;

    @Autowired
    private EntityManager em;

    @Autowired
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
        //
        //        AutowireHelper.getInstance().autowire(substanceProcessor);
        //        AutowireHelper.getInstance().autowire(relationshipProcessor);
        //        AutowireHelper.getInstance().autowire(referenceProcessor);

    }


    @Test
    public void removeSourceRelationshipShouldRemoveInvertedRelationship(@Autowired ApplicationEvents applicationEvents)   throws Exception {

        applicationEvents.clear();
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

        //now submit with one sided reference, processors should add the other side.
        assertCreated(substance2.toFullJsonNode());


        List<TryToCreateInverseRelationshipEvent> inverseCreateEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class)
                .collect(Collectors.toList());
        //this will create 1 event 
        assertEquals(1, inverseCreateEvents.size());
        applicationEvents.clear();
        //
        //
        TransactionTemplate transactionTemplate = new TransactionTemplate( transactionManager);
        transactionTemplate.executeWithoutResult(s->
        relationshipService.createNewInverseRelationshipFor(inverseCreateEvents.get(0))
                );

        //these are 2 events for the same relationship to be created (one from relationship processor which
        //can't be handled because sub1 doesn't exist yet
        //and one from substance processor that does get handled


        List<TryToCreateInverseRelationshipEvent> secondPassEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).collect(Collectors.toList());
        assertEquals(0, secondPassEvents.size());
        applicationEvents.clear();
        em.flush();

        //now there should be no new createRelationship events
        assertEquals(0, applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).count());

        Substance fetchedSubstance2 = substanceEntityService.get(uuid2).get();
        Relationship relationshipA = fetchedSubstance2.relationships.get(0);

        //confirm that the dangled has a relationship to the dangler 

        assertEquals(uuid1.toString(), relationshipA.relatedSubstance.refuuid);
        assertEquals("bar->foo", relationshipA.type);


        //Remove the primary relationship, ensure the inverse is gone
        SubstanceBuilder.from(fetchedSubstance2.toFullJsonNode())
        .andThen(s-> {
            s.removeRelationshipByUUID(s.relationships.get(0).uuid);
        })
        .buildJsonAnd(this::assertUpdated);


        List<RemoveInverseRelationshipEvent> inverseRemoveEvents = applicationEvents.stream(RemoveInverseRelationshipEvent.class)
                .collect(Collectors.toList());

        //        System.out.println(inverseRemoveEvents);
        assertEquals(1, inverseRemoveEvents.size());
        applicationEvents.clear();
        transactionTemplate.executeWithoutResult(s->
        relationshipService.removeInverseRelationshipFor(inverseRemoveEvents.get(0))
                );
        Substance substance = substanceEntityService.get(uuid1).get();
        List<Relationship> relationships = substance.relationships;
        assertEquals(0, relationships.size());

        //        System.out.println("final app events = " + applicationEvents.stream().collect(Collectors.toList()));
    }


    @Test
    public void addRelationshipAfterAddingEachSubstanceShouldAddInvertedRelationshipAndIncrementVersion(@Autowired ApplicationEvents applicationEvents)   throws Exception {

        applicationEvents.clear();
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
        .buildJsonAnd(this::assertCreated);

        //now submit with one sided reference, processors should add the other side.
        assertCreated(substance2.toFullJsonNode());

        //add relationship
        Substance substance1 = substanceEntityService.get(uuid1).get();
        assertEquals("1", substance1.version);

        SubstanceBuilder.from(substance1.toFullJsonNode())
        .addRelationshipTo(substance2, "foo->bar")
        .buildJsonAnd(this::assertUpdated);

        List<TryToCreateInverseRelationshipEvent> inverseCreateEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class)
                .collect(Collectors.toList());
        assertEquals(1, inverseCreateEvents.size());
        applicationEvents.clear();

        TransactionTemplate transactionTemplate = new TransactionTemplate( transactionManager);
        transactionTemplate.executeWithoutResult(s->
        relationshipService.createNewInverseRelationshipFor(inverseCreateEvents.get(0))
                );

        Substance substance = substanceEntityService.get(uuid2).get();
        assertEquals("2", substance.version);
        List<Relationship> relationships = substance.relationships;
        assertEquals(1, relationships.size());

        assertEquals("bar->foo", relationships.get(0).type);
        assertEquals(uuid1.toString(), relationships.get(0).relatedSubstance.refuuid);

        List<TryToCreateInverseRelationshipEvent> secondPassEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class)
                .collect(Collectors.toList());
        assertEquals(0, secondPassEvents.size());


    }

    // Are we sure about this test?
    // It would increment twice in 2.X, and currently does twice here too. Due to an order of operations issue
    // with flushing before it used to only increment once, but that was more of a bug than a feature as it 
    // made other things break

    //Ignoring test for now, as it's not what 2.X did and shouldn't be critical
    //    @Test
    public void addTwoRelationshipsToSameSubstanceShouldOnlyIncrementVersionOnce(@Autowired ApplicationEvents applicationEvents)   throws Exception {

        applicationEvents.clear();
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
        .buildJsonAnd(this::assertCreated);

        //now submit with one sided reference, processors should add the other side.
        assertCreated(substance2.toFullJsonNode());

        //add relationship
        Substance substance1 = substanceEntityService.get(uuid1).get();
        assertEquals("1", substance1.version);

        SubstanceBuilder.from(substance1.toFullJsonNode())
        .addRelationshipTo(substance2, "foo->bar")
        .addRelationshipTo(substance2, "foo2->bar2")
        .buildJsonAnd(this::assertUpdated);

        List<TryToCreateInverseRelationshipEvent> inverseCreateEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class)
                .collect(Collectors.toList());
        assertEquals(2, inverseCreateEvents.size());
        applicationEvents.clear();

        TransactionTemplate transactionTemplate = new TransactionTemplate( transactionManager);
        transactionTemplate.executeWithoutResult(s-> {
            relationshipService.createNewInverseRelationshipFor(inverseCreateEvents.get(0));
            relationshipService.createNewInverseRelationshipFor(inverseCreateEvents.get(1));
        });
        //this actually creates 2 NEW events to make the other side of the inverted relationship

        List<TryToCreateInverseRelationshipEvent> inverseCreateEvents2 = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class)
                .collect(Collectors.toList());
        assertEquals(2, inverseCreateEvents2.size());
        applicationEvents.clear();
        //but when we run them through the relationshipService no new relationships are created...
        transactionTemplate.executeWithoutResult(s-> {
            relationshipService.createNewInverseRelationshipFor(inverseCreateEvents2.get(0));
            relationshipService.createNewInverseRelationshipFor(inverseCreateEvents2.get(1));
        });
        assertEquals(0, applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).count());
        //and we only updated the substance version once
        Substance substance = substanceEntityService.get(uuid2).get();
        assertEquals("2", substance.version);
        List<Relationship> relationships = substance.relationships;
        assertEquals(2, relationships.size());

        assertThat(relationships.stream().map(r-> r.type).collect(Collectors.toSet()), contains("bar2->foo2", "bar->foo"));

        assertEquals(uuid1.toString(), relationships.get(0).relatedSubstance.refuuid);
        assertEquals(uuid1.toString(), relationships.get(1).relatedSubstance.refuuid);


    }



    @Test
    public void addRelationshipAfterAddingEachSubstanceShouldAddInvertedRelationshipAndShouldBeInHistoryOnlyOnce(@Autowired ApplicationEvents applicationEvents)   throws Exception {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        //This is very hard to read right now. A substanceBuilder would make this easy.

        //submit primary
        JsonNode tmp = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate1));
        JsonNode newRelate = tmp.at("/relationships/0");
        JsonNode js = new JsonUtil.JsonNodeBuilder(tmp)
                .remove("/relationships/1")
                .remove("/relationships/0")
                .ignoreMissing()
                .build();

        String uuid = js.get("uuid").asText();


        //submit alternative
        JsonNode jsA = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate2));
        String uuidA = jsA.get("uuid").asText();


        applicationEvents.clear();
        transactionTemplate.executeWithoutResult( status-> {
            assertCreated(js);
            assertCreated(jsA);
        });

        transactionTemplate.executeWithoutResult( status->
        applicationEvents.stream(CreateEditEvent.class).collect(Collectors.toList()).forEach(editEventService::createNewEditFromEvent)
                );
        applicationEvents.clear();

        JsonNode updatedJson = transactionTemplate.execute(status-> {
            //add relationship To
            JsonNode updated = new JsonUtil.JsonNodeBuilder(substanceEntityService.get(UUID.fromString(uuid)).get().toFullJsonNode())
                    .add("/relationships/-", newRelate)
                    .ignoreMissing()
                    .build();
            assertUpdated(updated);
            //update the substance for real
            return updated;
        });


        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(updatedJson);
        String[] parts=type1.split("->");

        //check inverse relationship with primary
        //        Substance substanceA = substanceEntityService.get(UUID.fromString(uuidA)).get();
        //
        //        Relationship relationshipA = substanceA.relationships.get(0);
        //        String refUuidA = relationshipA.relatedSubstance.refuuid;
        //
        //        assertEquals(uuid, refUuidA);
        //        assertEquals(parts[1] + "->" + parts[0],relationshipA.type);
        //    	assertEquals("2",substanceA.version);

        List<TryToCreateInverseRelationshipEvent> createInverseEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).collect(Collectors.toList());

        Substance updatedSubstance = SubstanceBuilder.from(updatedJson).build();

        assertEquals(1, createInverseEvents.size());
        assertEquals(TryToCreateInverseRelationshipEvent.builder()
                .relationshipIdToInvert(updatedSubstance.relationships.get(0).uuid)
                .creationMode(TryToCreateInverseRelationshipEvent.CreationMode.CREATE_IF_MISSING_DEEP_CHECK)
                .fromSubstance(UUID.fromString(uuidA))
                .toSubstance(updatedSubstance.uuid)
                .originatorUUID(updatedSubstance.relationships.get(0).uuid)
                .build(),
                createInverseEvents.get(0));

        transactionTemplate.executeWithoutResult( status-> {
            relationshipService.createNewInverseRelationshipFor(createInverseEvents.get(0));

        });
        List<CreateEditEvent> editsFromInverseCreation = applicationEvents.stream(CreateEditEvent.class).collect(Collectors.toList());
        assertFalse(editsFromInverseCreation.isEmpty());
        transactionTemplate.executeWithoutResult( status->
        editsFromInverseCreation.forEach(editEventService::createNewEditFromEvent)
                );

        List<Edit> edits = editRepository.findByRefidOrderByCreatedDesc(uuid);
        //    	assertEquals( 2, edits.size());
        //
        assertEquals("1", edits.get(0).version);

    }

    @Test
    public void addRelationshipAfterAddingEachSubstanceShouldAddInvertedRelationshipAndShouldBeInHistory(@Autowired ApplicationEvents applicationEvents)   throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        //This is very hard to read right now. A substanceBuilder would make this easy.
        applicationEvents.clear();
        //submit primary
        JsonNode js = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate1));
        JsonNode newRelate = js.at("/relationships/0");
        js=new JsonUtil.JsonNodeBuilder(js)
                .remove("/relationships/1")
                .remove("/relationships/0")
                .ignoreMissing()
                .build();

        String uuid = js.get("uuid").asText();
        assertCreated(js);
        transactionTemplate.executeWithoutResult( status->
        applicationEvents.stream(CreateEditEvent.class).collect(Collectors.toList()).forEach(editEventService::createNewEditFromEvent)
                );
        applicationEvents.clear();

        //submit alternative
        JsonNode jsA = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate2));
        String uuidA = jsA.get("uuid").asText();

        assertCreated(jsA);
        transactionTemplate.executeWithoutResult( status->
        applicationEvents.stream(CreateEditEvent.class).collect(Collectors.toList()).forEach(editEventService::createNewEditFromEvent)
                );
        assertEquals(0L, applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).count());
        applicationEvents.clear();

        JsonNode beforeA = transactionTemplate.execute(s->{
            return substanceEntityService.get(UUID.fromString(uuidA)).get().toFullJsonNode();
        });


        //add relationship To
        JsonNode updated=new JsonUtil.JsonNodeBuilder(substanceEntityService.get(UUID.fromString(uuid)).get().toFullJsonNode())
                .add("/relationships/-", newRelate)
                .ignoreMissing()
                .build();

        //update the substance for real
        assertUpdated(updated);
        transactionTemplate.executeWithoutResult( status->
        {
            List<CreateEditEvent> list = applicationEvents.stream(CreateEditEvent.class).collect(Collectors.toList());


            list.forEach(editEventService::createNewEditFromEvent);
        }
                );
        List<TryToCreateInverseRelationshipEvent> createInverseEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).collect(Collectors.toList());
        applicationEvents.clear();
        assertEquals(1, createInverseEvents.size());
        transactionTemplate.executeWithoutResult( status-> {
            relationshipService.createNewInverseRelationshipFor(createInverseEvents.get(0));
            em.flush();
        });
        List<CreateEditEvent> editsFromInverseCreation = applicationEvents.stream(CreateEditEvent.class).collect(Collectors.toList());
        assertFalse(editsFromInverseCreation.isEmpty());
        transactionTemplate.executeWithoutResult( status-> {
            editsFromInverseCreation.forEach(editEventService::createNewEditFromEvent);
            em.flush();
        }
                );


        applicationEvents.clear();

        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(updated);
        String[] parts=type1.split("->");

        transactionTemplate.executeWithoutResult( s-> {
            //check inverse relationship with primary
            Substance substanceA = substanceEntityService.get(UUID.fromString(uuidA)).get();

            Relationship relationshipA = substanceA.relationships.get(0);
            String refUuidA = relationshipA.relatedSubstance.refuuid;

            assertTrue(refUuidA.equals(uuid));
            assertEquals(parts[1] + "->" + parts[0], relationshipA.type);
            assertEquals("2", substanceA.version);
        });

        //        em.flush();
        transactionTemplate.executeWithoutResult( s->{
            List<Edit> otherSubEdits = editRepository.findByRefidOrderByCreatedDesc(uuid);
            //            JsonNode historyFetchedForFirst=editRepository.findByRefidOrderByCreatedDesc(uuid).get(0).getOldValueReference().rawJson();
            List<Edit> byRefidOrderByCreatedDesc = editRepository.findByRefidOrderByCreatedDesc(uuidA);
            Edit edit = byRefidOrderByCreatedDesc.get(0);
            JsonNode historyFetchedForSecond= edit.getOldValueReference().rawJson();
            Changes changes= JsonUtil.computeChanges(beforeA, historyFetchedForSecond, new ChangeFilter[0]);

            assertEquals(beforeA,historyFetchedForSecond);
        });
        //TODO other edits might still not have their old value set?

        //    	JsonNode historyFetchedForSecond=editRepository.findByRefidOrderByCreatedDesc(uuidA).get(0).getOldValueReference().rawJson();
        //    	Changes changes= JsonUtil.computeChanges(beforeA, historyFetchedForSecond, new ChangeFilter[0]);

        //    	assertEquals(beforeA,historyFetchedForSecond);

    }

    @Test
    public void testAddRelationshipAfterAddingEachSubstanceThenRemovingFromPrimaryRelationshipShouldPass() throws Exception {

        //submit primary
        JsonNode js = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate1));
        JsonNode newRelate = js.at("/relationships/0");
        js=new JsonUtil.JsonNodeBuilder(js)
                .remove("/relationships/1")
                .remove("/relationships/0")
                .ignoreMissing()
                .build();

        String uuid = js.get("uuid").asText();
        assertCreated(js);


        //submit alternative
        JsonNode jsA = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate2));
        String uuidA = jsA.get("uuid").asText();
        assertCreated(jsA);

        //add relationship
        JsonNode updated=new JsonUtil.JsonNodeBuilder(js)
                .add("/relationships/-", newRelate)
                .ignoreMissing().build();
        assertUpdated(updated);
        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(updated);
        String[] parts=type1.split("->");

        //check inverse relationship with primary
        Substance fetchedA = substanceEntityService.get(UUID.fromString(uuid)).get();

        assertEquals(uuidA, fetchedA.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(parts[0] + "->" + parts[1],fetchedA.relationships.get(0).type);

        JsonNode updatedA=new JsonUtil.JsonNodeBuilder(fetchedA.toFullJsonNode())
                .remove("/relationships/0")
                .ignoreMissing().build();

        assertUpdated(updatedA);




        assertEquals(Collections.emptyList(),  substanceEntityService.get(UUID.fromString(uuid)).get().relationships);
        assertEquals(Collections.emptyList(),  substanceEntityService.get(UUID.fromString(uuidA)).get().relationships);
    }

}
