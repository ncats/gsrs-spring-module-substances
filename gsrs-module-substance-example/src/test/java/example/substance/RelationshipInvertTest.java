package example.substance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.junit.Ignore;
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

import com.fasterxml.jackson.databind.JsonNode;

import example.SubstanceJsonUtil;
import gsrs.events.CreateEditEvent;
import gsrs.junit.json.ChangeFilter;
import gsrs.junit.json.Changes;
import gsrs.junit.json.JsonUtil;
import gsrs.module.substance.processors.ReferenceProcessor;
import gsrs.module.substance.processors.RelationEventListener;
import gsrs.module.substance.processors.RelationshipProcessor;
import gsrs.module.substance.processors.RemoveInverseRelationshipEvent;
import gsrs.module.substance.processors.SubstanceProcessor;
import gsrs.module.substance.processors.TryToCreateInverseRelationshipEvent;
import gsrs.module.substance.services.RelationshipService;
import gsrs.repository.EditRepository;
import gsrs.services.EditEventService;
import gsrs.services.PrincipalService;
import gsrs.startertests.TestEntityProcessorFactory;
import ix.core.models.Edit;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
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
    private PrincipalService principalService;


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
    public void addSubstanceWithRelationshipThenAddRelatedSubstanceShouldResultInBirectionalRelationship(@Autowired ApplicationEvents applicationEvents)   throws Exception {

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
        //inverse relationship event is made but the relationship service can't do anything about it since substance2 isn't in DB yet
        List<TryToCreateInverseRelationshipEvent> inverseCreateEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class)
                .collect(Collectors.toList());
        assertEquals(1, inverseCreateEvents.size());

        applicationEvents.clear();

        TransactionTemplate transactionTemplate = new TransactionTemplate( transactionManager);
        transactionTemplate.executeWithoutResult(s->
                relationshipService.createNewInverseRelationshipFor(inverseCreateEvents.get(0))
        );
        assertEquals(0, applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).count());

        applicationEvents.clear();
        //now submit with one sided reference, processors should add the other side.
        assertCreated(substance2.toFullJsonNode());

        //this also makes an inverse event because the substance processor finds the dangling relationship
        List<TryToCreateInverseRelationshipEvent> inverseCreateEvents2 = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class)
                .collect(Collectors.toList());
        assertEquals(1, inverseCreateEvents2.size());
        applicationEvents.clear();

        transactionTemplate.executeWithoutResult(s->
                relationshipService.createNewInverseRelationshipFor(inverseCreateEvents2.get(0))
        );

        //this makes ANOTHER create inverse event and the relationship service doesnt do anything since it already exists
        List<TryToCreateInverseRelationshipEvent> inverseCreateEvents3 = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class)
                .collect(Collectors.toList());
        assertEquals(1, inverseCreateEvents3.size());
        applicationEvents.clear();

        transactionTemplate.executeWithoutResult(s->
                relationshipService.createNewInverseRelationshipFor(inverseCreateEvents3.get(0))
        );
        assertEquals(0, applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).count());


        Substance fetchedSubstance2 = substanceEntityService.get(uuid2).get();
        Relationship relationshipA = fetchedSubstance2.relationships.get(0);

        //confirm that the dangled has a relationship to the dangler

        assertEquals(uuid1.toString(), relationshipA.relatedSubstance.refuuid);
        assertEquals("bar->foo", relationshipA.type);
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
	        //this will create  2 events because we can't check if we need to create the inverse event or not yet
        //(that check is done in the relationship service)
	        assertEquals(2, inverseCreateEvents.size());
        applicationEvents.clear();
//
//
        TransactionTemplate transactionTemplate = new TransactionTemplate( transactionManager);
        transactionTemplate.executeWithoutResult(s->
	        relationshipService.createNewInverseRelationshipFor(inverseCreateEvents.get(0))
        );
        transactionTemplate.executeWithoutResult(s->
                relationshipService.createNewInverseRelationshipFor(inverseCreateEvents.get(1))
        );
        //these are 2 events for the same relationship to be created (one from relationship processor which
        //can't be handled because sub1 doesn't exist yet
        //and one from substance processor that does get handled


        List<TryToCreateInverseRelationshipEvent> secondPassEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).collect(Collectors.toList());
        assertEquals(1, secondPassEvents.size());
        applicationEvents.clear();
        secondPassEvents.forEach(e->{
            transactionTemplate.executeWithoutResult(s->
                    relationshipService.createNewInverseRelationshipFor(e)
            );
        });
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

        //we don't have a way to check if we need to create this inverted relationship or not yet so
        // another event is created. the relationship service will figure out if it needs to be made or not.
        List<TryToCreateInverseRelationshipEvent> secondPassEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class)
                .collect(Collectors.toList());
        assertEquals(1, secondPassEvents.size());
        applicationEvents.clear();
        secondPassEvents.forEach(e->{
            transactionTemplate.executeWithoutResult(s->
                    relationshipService.createNewInverseRelationshipFor(e)
            );
        });
        //the relationship service should see there is nothing else to make

        assertEquals(0, applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).count());


    	
    }

    // Are we sure about this test?
    // It would increment twice in 2.X, and currently does twice here too. Due to an order of operations issue
    // with flushing before it used to only increment once, but that was more of a bug than a feature as it 
    // made other things break
    @Ignore
    @Test
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
                        .creationMode(TryToCreateInverseRelationshipEvent.CreationMode.CREATE_IF_MISSING)
                        .fromSubstance(UUID.fromString(uuidA))
                        .toSubstance(updatedSubstance.uuid)
                        .originatorSubstance(updatedSubstance.uuid)
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
    	assertEquals("2", edits.get(0).version);
    	
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
    
    /*
    @Test
    public void testAddRelationshipAfterAddingEachSubstanceThenRemovingInvertedRelationshipShouldFail() throws Exception {

	        //submit primary
	        JsonNode js = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate1));
	        JsonNode newRelate = js.at("/relationships/0");
	        js=new JsonUtil.JsonNodeBuilder(js)
				.remove("/relationships/1")
				.remove("/relationships/0")
				.ignoreMissing()
				.build();
	        
	        String uuid = js.get("uuid").asText();
	        SubstanceAPI.ValidationResponse validationResult = api.validateSubstance(js);
	        assertTrue(validationResult.isValid());
	        ensurePass(api.submitSubstance(js));
	        js =api.fetchSubstanceJsonByUuid(uuid);
	        
	        
	        //submit alternative
	        JsonNode jsA = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate2));
	        String uuidA = jsA.get("uuid").asText();
	        SubstanceAPI.ValidationResponse validationResultA = api.validateSubstance(js);
	        assertTrue(validationResultA.isValid());
	        ensurePass(api.submitSubstance(jsA));
	        
	        //add relationship
	        JsonNode updated=new JsonUtil.JsonNodeBuilder(js)
			.add("/relationships/-", newRelate)
			.ignoreMissing().build();
	        ensurePass(api.updateSubstance(updated));
	        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(updated);
	        String[] parts=type1.split("->");
	        
	        //check inverse relationship with primary
	        JsonNode fetchedA = api.fetchSubstanceJsonByUuid(uuidA);
	        String refUuidA = SubstanceJsonUtil.getRefUuidOnFirstRelationship(fetchedA);
	        assertTrue(refUuidA.equals(uuid));
	        assertEquals(parts[1] + "->" + parts[0],SubstanceJsonUtil.getTypeOnFirstRelationship(fetchedA));
	        
	        JsonNode updatedA=new JsonUtil.JsonNodeBuilder(fetchedA)
			.remove("/relationships/0")
			.ignoreMissing().build();
	        
	        ensurePass(api.updateSubstance(updatedA));

        assertEquals(Collections.emptyList(),  SubstanceBuilder.from(api.fetchSubstanceJsonByUuid(uuid)).build().relationships);
        assertEquals("substance with uuid " + uuidA, Collections.emptyList(),  SubstanceBuilder.from(api.fetchSubstanceJsonByUuid(uuidA)).build().relationships);


    }
*/
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
    /*
    @Test
    public void testDontAddRelationshipIfOneLikeItAlreadyExists()   throws Exception {
    	
        //submit primary
        JsonNode js = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate1));
        JsonNode newRelate = js.at("/relationships/0");
        String uuid = js.get("uuid").asText();
        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(js);
        String[] parts=type1.split("->");
        
        newRelate=new JsonUtil.JsonNodeBuilder(newRelate)
			.set("/relatedSubstance/refuuid",uuid)
			.set("/relatedSubstance/refPname",js.at("/names/0/name").asText())
			.set("/type",parts[1] + "->" + parts[0])
			.ignoreMissing()
			.build();


        SubstanceAPI.ValidationResponse validationResult = api.validateSubstance(js);
        assertTrue(validationResult.isValid());
        ensurePass(api.submitSubstance(js));
        js =api.fetchSubstanceJsonByUuid(uuid);
        
        
        //submit alternative
        JsonNode jsA = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate2));
        String uuidA = jsA.get("uuid").asText();
        jsA=new JsonUtil.JsonNodeBuilder(jsA)
		.add("/relationships/-",newRelate)
		.ignoreMissing()
		.build();
        SubstanceAPI.ValidationResponse validationResultA = api.validateSubstance(js);
        assertTrue(validationResultA.isValid());
        ensurePass(api.submitSubstance(jsA));
        
        
        //check inverse relationship with primary
        JsonNode fetchedA = api.fetchSubstanceJsonByUuid(uuidA);
        String refUuidA = SubstanceJsonUtil.getRefUuidOnFirstRelationship(fetchedA);
        assertTrue(refUuidA.equals(uuid));
        assertEquals(parts[1] + "->" + parts[0],SubstanceJsonUtil.getTypeOnFirstRelationship(fetchedA));
        assertEquals(1,fetchedA.at("/relationships").size());
        
    }
    
    @Test
    public void testDontAddRelationshipIfOneLikeItAlreadyExistsAndDontMakeEdit()   throws Exception {
    	
        //submit primary
        JsonNode js = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate1));
        JsonNode newRelate = js.at("/relationships/0");
        String uuid = js.get("uuid").asText();
        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(js);
        String[] parts=type1.split("->");
        
        newRelate=new JsonUtil.JsonNodeBuilder(newRelate)
			.set("/relatedSubstance/refuuid",uuid)
			.set("/relatedSubstance/refPname",js.at("/names/0/name").asText())
			.set("/type",parts[1] + "->" + parts[0])
			.ignoreMissing()
			.build();

        SubstanceAPI.ValidationResponse validationResponse = api.validateSubstance(js);
        assertTrue(validationResponse.isValid());
        ensurePass(api.submitSubstance(js));

        api.fetchSubstanceJsonByUuid(uuid);
        
        
        //submit alternative
        JsonNode jsA = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate2));
        String uuidA = jsA.get("uuid").asText();
        jsA=new JsonUtil.JsonNodeBuilder(jsA)
			.add("/relationships/-",newRelate)
			.ignoreMissing()
			.build();

        SubstanceAPI.ValidationResponse validationResponseA = api.validateSubstance(jsA);

        assertTrue(validationResponseA.isValid());
        ensurePass(api.submitSubstance(jsA));
        
        
        //check inverse relationship with primary
        JsonNode fetchedA = api.fetchSubstanceJsonByUuid(uuidA);
        String refUuidA = SubstanceJsonUtil.getRefUuidOnFirstRelationship(fetchedA);
        assertTrue(refUuidA.equals(uuid));
        assertEquals(parts[1] + "->" + parts[0],SubstanceJsonUtil.getTypeOnFirstRelationship(fetchedA));
        assertEquals(1,fetchedA.at("/relationships").size());
        
        //Shouldn't be any edit history either
        assertEquals(404,api.fetchAllSubstanceHistory(uuid).getStatus());
    }

    /**
     * On trying to delete a INHIBITOR -> TRANSPORTER relationship,
     * the validation rules portion of the form returned:
     *
     * ERROR
     Error updating entity [Error ID:e86fed46]:java.lang.IllegalStateException: java.lang.IllegalStateException: java.lang.IllegalStateException: javax.persistence.OptimisticLockException: Data has changed. updated [0] rows sql[update ix_ginas_substance set current_version=?, last_edited=?, version=?, internal_version=? where uuid=? and internal_version=?] bind[null]. See applicaiton log for more details.


     root cause is

     Caused by: javax.persistence.OptimisticLockException: Data has changed. updated [0] rows sql[update ix_ginas_substance set current_version=?, last_edited=?, version=?, internal_version=? where uuid=? and internal_version=?] bind[null]
     at com.avaje.ebeaninternal.server.persist.dml.DmlHandler.checkRowCount(DmlHandler.java:95) ~[org.avaje.ebeanorm.avaje-ebeanorm-3.3.4.jar:na]
     at com.avaje.ebeaninternal.server.persist.dml.UpdateHandler.execute(UpdateHandler.java:81) ~[org.avaje.ebeanorm.avaje-ebeanorm-3.3.4.jar:na]

     */

    @Test
    public void removeRelationshipWithActiveMoeityGsrs587FromOriginal(@Autowired ApplicationEvents applicationEvents)   throws Exception {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        applicationEvents.clear();

        UUID parentUUID = UUID.fromString(substanceEntityService.createEntity( new SubstanceBuilder()
                .addName("parent", name -> name.displayName =true)
                .generateNewUUID()
                .buildJson())
                .getCreatedEntity().uuid.toString());

        Substance inhibitor = new SubstanceBuilder()
                                        .addName("inhibitor", name -> name.displayName =true)
                                        .addActiveMoiety()
                                        .generateNewUUID()
                                        .build();

        List<Relationship> activeMoeity = inhibitor.getActiveMoieties();

        assertEquals(1, activeMoeity.size());

        Substance transporter = new SubstanceBuilder()
                .addName("transporter", name -> name.displayName =true)
                .generateNewUUID()
                .build();

       Substance createdInhibitor = assertCreated(inhibitor.toFullJsonNode());

        Set<TryToCreateInverseRelationshipEvent> createInverseRelationshipEvents = applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).collect(Collectors.toSet());
        //active moeities don't have inverse relationships
        assertEquals(0, createInverseRelationshipEvents.size());

        Substance storedTransporter = substanceEntityService.createEntity(transporter.toFullJsonNode()).getCreatedEntity();


        SubstanceBuilder.from(substanceRepository.getOne(parentUUID).toFullJsonNode())
                .addRelationshipTo(createdInhibitor, "INFRASPECIFIC->PARENT ORGANISM")
                .buildJsonAnd( this::assertUpdated);



        SubstanceBuilder.from(substanceRepository.getOne(createdInhibitor.getUuid()).toFullJsonNode())
                .addRelationshipTo(storedTransporter, "INHIBITOR->TRANSPORTER")
                .buildJsonAnd(this::assertUpdated);


        transactionTemplate.executeWithoutResult(s->{
            applicationEvents.stream(TryToCreateInverseRelationshipEvent.class).collect(Collectors.toList()).forEach(relationshipService::createNewInverseRelationshipFor);
        });


        transactionTemplate.executeWithoutResult(s-> {
            Substance actualTransporter = substanceRepository.findById(storedTransporter.getUuid()).get();

            assertEquals(1, actualTransporter.relationships.size());
            applicationEvents.clear();
            System.out.println("inhibior uuid = " + inhibitor.getUuid());
            SubstanceBuilder.from(substanceRepository.findById(inhibitor.getUuid()).get().toFullJsonNode())
                                                .andThen(substance->{
                                                    UUID relationshipIdToRemove=null;
                                                    try(Stream<Relationship> rels = substance.relationships.stream()){
                                                        relationshipIdToRemove= rels.filter(r->  r.type.equals("INHIBITOR->TRANSPORTER")).findAny().get().uuid;
                                                    }
                                                    assertNotNull(relationshipIdToRemove);
                                                    substance.removeRelationshipByUUID(relationshipIdToRemove);
                                                    return substance;
                                                })
                            .buildJsonAnd(this::assertUpdated);

                });
        Set<RemoveInverseRelationshipEvent> removeInverseRelationshipEvents = applicationEvents.stream(RemoveInverseRelationshipEvent.class).collect(Collectors.toSet());

        assertEquals(1, removeInverseRelationshipEvents.size());

        transactionTemplate.executeWithoutResult(s->{
            removeInverseRelationshipEvents.forEach(relationshipService::removeInverseRelationshipFor);
        });


        Substance actualInhibitor =substanceRepository.findById(inhibitor.getUuid()).get();

        assertEquals(2, actualInhibitor.relationships.size());
        assertFalse(actualInhibitor.relationships.stream()
                .filter(r -> r.type.equals("INHIBITOR->TRANSPORTER"))
                .findAny()
                .isPresent());

        Optional<Substance> opt = substanceRepository.findById(transporter.getUuid());
        Substance modifiedTransporter = opt.get();
        assertEquals(Collections.emptyList(), modifiedTransporter.relationships);


    }
/*
    @Test
    public void removeRelationshipWithActiveMoeityGsrs587FromOtherSide(){

        Substance parent = new SubstanceBuilder()
                                .addName("parent")
                                .generateNewUUID()
                                .build();

        api.submitSubstance(parent);

        Substance inhibitor = new SubstanceBuilder()
                .addName("inhibitor")
                .addActiveMoiety()

                .build();

        Substance transporter = new SubstanceBuilder()
                .addName("transporter")
                .build();

        JsonNode json = api.submitSubstance(inhibitor);

        Substance storedInhibitor = SubstanceBuilder.from(json).build();

        Substance storedTransporter = SubstanceBuilder.from(api.submitSubstance(transporter)).build();



        JsonNode withRel = new SubstanceBuilder(storedInhibitor)
                .addRelationshipTo(storedTransporter, "INHIBITOR -> TRANSPORTER")
                .buildJson();

        Substance storedWithRel = SubstanceBuilder.from(api.updateSubstanceJson(withRel)).build();

        SubstanceBuilder.from(api.fetchSubstanceJsonByUuid(parent.getUuid()))
                .addRelationshipTo(inhibitor, "INFRASPECIFIC->PARENT ORGANISM")
                .buildJsonAnd( js -> api.updateSubstanceJson(js));


        Substance actualTransporter = api.fetchSubstanceObjectByUuid(storedTransporter.getUuid().toString(), Substance.class);

        assertEquals(1, actualTransporter.relationships.size());

        actualTransporter.relationships.remove(0);

        api.updateSubstanceJson(new SubstanceBuilder(actualTransporter).buildJson());

        Substance actualInhibitor = api.fetchSubstanceObjectByUuid(storedInhibitor.getUuid().toString(), Substance.class);

        assertFalse(actualInhibitor.relationships.stream()
                                        .filter(r -> r.type.equals("INHIBITOR -> TRANSPORTER"))
                                        .findAny()
                                        .isPresent());
//        assertEquals(actualInhibitor.getActiveMoieties(), actualInhibitor.relationships);

        Substance modifiedTransporter = api.fetchSubstanceObjectByUuid(storedTransporter.getUuid().toString(), Substance.class);
        assertEquals(Collections.emptyList(), modifiedTransporter.relationships);


    }

    @Test
    public void changeCommentOrRelationshipOnGeneratorSideShouldAlsoBeReflectedOnOtherSide(){
        Substance inhibitor = new SubstanceBuilder()
                .addName("inhibitor")
                .generateNewUUID()
                .build();


        Substance transporter = new SubstanceBuilder()
                .addName("transporter")
                .generateNewUUID()
                .build();

        Substance storedTransporter = SubstanceBuilder.from(api.submitSubstance(transporter)).build();

        JsonNode withRel = SubstanceBuilder.from(api.submitSubstance(inhibitor))
                .addRelationshipTo(storedTransporter, "INHIBITOR->TRANSPORTER")
                .buildJson();

        Substance storedWithRel = SubstanceBuilder.from(api.updateSubstanceJson(withRel)).build();


        assertEquals(Arrays.asList("INHIBITOR->TRANSPORTER"), storedWithRel.relationships.stream()
                                                                                .map(r-> r.type)
                                                                                 .collect(Collectors.toList()));

        Substance actualTransporter = api.fetchSubstanceObjectByUuid(transporter.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("TRANSPORTER->INHIBITOR"), actualTransporter.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));

        storedWithRel.relationships.get(0).comments= "BEAN ME UP SCOTTY";
        Substance storedWithRel2 = SubstanceBuilder.from(api.updateSubstanceJson(storedWithRel.toFullJsonNode())).build();


        assertEquals(Arrays.asList("BEAN ME UP SCOTTY"), storedWithRel2.relationships.stream()
                .map(r-> r.comments)
                .collect(Collectors.toList()));


        Substance actualTransporter2 = api.fetchSubstanceObjectByUuid(transporter.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("BEAN ME UP SCOTTY"), actualTransporter2.relationships.stream()
                .map(r-> r.comments)
                .collect(Collectors.toList()));


    }

    @Test
    public void changeCommentOnRelationshipOnNonGeneratorSideShouldAlsoBeReflectedOnOtherSide(){
        Substance inhibitor = new SubstanceBuilder()
                .addName("inhibitor")
                .generateNewUUID()
                .build();


        Substance transporter = new SubstanceBuilder()
                .addName("transporter")
                .generateNewUUID()
                .build();

        Substance storedTransporter = SubstanceBuilder.from(api.submitSubstance(transporter)).build();

        JsonNode withRel = SubstanceBuilder.from(api.submitSubstance(inhibitor))
                .addRelationshipTo(storedTransporter, "INHIBITOR->TRANSPORTER")
                .buildJson();

        Substance storedWithRel = SubstanceBuilder.from(api.updateSubstanceJson(withRel)).build();


        assertEquals(Arrays.asList("INHIBITOR->TRANSPORTER"), storedWithRel.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));

        Substance actualTransporter = api.fetchSubstanceObjectByUuid(transporter.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("TRANSPORTER->INHIBITOR"), actualTransporter.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));

        actualTransporter.relationships.get(0).comments= "BEAN ME UP SCOTTY";
        Substance storedWithRel2 = SubstanceBuilder.from(api.updateSubstanceJson(actualTransporter.toFullJsonNode())).build();


        assertEquals(Arrays.asList("BEAN ME UP SCOTTY"), storedWithRel2.relationships.stream()
                .map(r-> r.comments)
                .collect(Collectors.toList()));


        Substance actualInhibitor2 = api.fetchSubstanceObjectByUuid(inhibitor.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("BEAN ME UP SCOTTY"), actualInhibitor2.relationships.stream()
                .map(r-> r.comments)
                .collect(Collectors.toList()));


    }

    @Test
    public void changeTypeOnRelationshipOnGeneratorSideShouldAlsoBeReflectedOnOtherSide(){
        Substance inhibitor = new SubstanceBuilder()
                .addName("inhibitor")
                .generateNewUUID()
                .build();


        Substance transporter = new SubstanceBuilder()
                .addName("transporter")
                .generateNewUUID()
                .build();

        Substance storedTransporter = SubstanceBuilder.from(api.submitSubstance(transporter)).build();

        JsonNode withRel = SubstanceBuilder.from(api.submitSubstance(inhibitor))
                .addRelationshipTo(storedTransporter, "INHIBITOR->TRANSPORTER")
                .buildJson();

        Substance storedWithRel = SubstanceBuilder.from(api.updateSubstanceJson(withRel)).build();


        assertEquals(Arrays.asList("INHIBITOR->TRANSPORTER"), storedWithRel.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));

        Substance actualTransporter = api.fetchSubstanceObjectByUuid(transporter.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("TRANSPORTER->INHIBITOR"), actualTransporter.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));

        storedWithRel.relationships.get(0).type= "KIRK->TRANSPORTER";
        Substance storedWithRel2 = SubstanceBuilder.from(api.updateSubstanceJson(storedWithRel.toFullJsonNode())).build();


        assertEquals(Arrays.asList("KIRK->TRANSPORTER"), storedWithRel2.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));


        Substance actualTransporter2 = api.fetchSubstanceObjectByUuid(transporter.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("TRANSPORTER->KIRK"), actualTransporter2.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));


    }

    @Test
    public void changeTypeOnRelationshipOnNonGeneratorSideShouldAlsoBeReflectedOnOtherSide(){
        Substance inhibitor = new SubstanceBuilder()
                .addName("inhibitor")
                .generateNewUUID()
                .build();


        Substance transporter = new SubstanceBuilder()
                .addName("transporter")
                .generateNewUUID()
                .build();

        Substance storedTransporter = SubstanceBuilder.from(api.submitSubstance(transporter)).build();

        JsonNode withRel = SubstanceBuilder.from(api.submitSubstance(inhibitor))
                .addRelationshipTo(storedTransporter, "INHIBITOR->TRANSPORTER")
                .buildJson();

        Substance storedWithRel = SubstanceBuilder.from(api.updateSubstanceJson(withRel)).build();


        assertEquals(Arrays.asList("INHIBITOR->TRANSPORTER"), storedWithRel.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));

        Substance actualTransporter = api.fetchSubstanceObjectByUuid(transporter.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("TRANSPORTER->INHIBITOR"), actualTransporter.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));

        actualTransporter.relationships.get(0).type= "TRANSPORTER->KIRK";
        Substance storedWithRel2 = SubstanceBuilder.from(api.updateSubstanceJson(actualTransporter.toFullJsonNode())).build();


        assertEquals(Arrays.asList("TRANSPORTER->KIRK"), storedWithRel2.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));


        Substance actualInhibitor2 = api.fetchSubstanceObjectByUuid(inhibitor.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("KIRK->TRANSPORTER"), actualInhibitor2.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));


    }

    @Test
    public void multipleChangesAllOnOneSide(){
        Substance inhibitor = new SubstanceBuilder()
                .addName("inhibitor")
                .generateNewUUID()
                .build();


        Substance transporter = new SubstanceBuilder()
                .addName("transporter")
                .generateNewUUID()
                .build();

        Substance storedTransporter = SubstanceBuilder.from(api.submitSubstance(transporter)).build();

        JsonNode withRel = SubstanceBuilder.from(api.submitSubstance(inhibitor))
                .addRelationshipTo(storedTransporter, "INHIBITOR->TRANSPORTER")
                .addRelationshipTo(storedTransporter, "SOMETHING->DIFFERENT")
                .buildJson();

        Substance storedWithRel = SubstanceBuilder.from(api.updateSubstanceJson(withRel)).build();


        //using TreeSets so they are sorted because
        // the relationships can be stored or come back in any order...
        assertEquals(setOf("INHIBITOR->TRANSPORTER","SOMETHING->DIFFERENT"), storedWithRel.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toCollection(TreeSet::new)));

        Substance actualTransporter = api.fetchSubstanceObjectByUuid(transporter.getUuid().toString(), Substance.class);

        assertEquals(setOf("DIFFERENT->SOMETHING", "TRANSPORTER->INHIBITOR" ), actualTransporter.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toCollection(TreeSet::new)));

        storedWithRel.relationships.get(0).comments= "BEAN ME UP SCOTTY";
        storedWithRel.relationships.remove(1);
        Substance storedWithRel2 = SubstanceBuilder.from(api.updateSubstanceJson(storedWithRel.toFullJsonNode())).build();


        assertEquals(Arrays.asList("BEAN ME UP SCOTTY"), storedWithRel2.relationships.stream()
                .map(r-> r.comments)
                .collect(Collectors.toList()));


        Substance actualTransporter2 = api.fetchSubstanceObjectByUuid(transporter.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("BEAN ME UP SCOTTY"), actualTransporter2.relationships.stream()
                .map(r-> r.comments)
                .collect(Collectors.toList()));


    }

    @Test
    public void relatedSubstanceAddedLaterShouldGetWaitingRelationshipAddedOnLoad() {
        Substance inhibitor = new SubstanceBuilder()
                .addName("inhibitor")
                .generateNewUUID()
                .build();


        Substance transporter = new SubstanceBuilder()
                .addName("transporter")
                .generateNewUUID()
                .build();


        JsonNode withRel = SubstanceBuilder.from(api.submitSubstance(inhibitor))
                .addRelationshipTo(transporter, "INHIBITOR->TRANSPORTER")
                .buildJson();

        Substance storedWithRel = SubstanceBuilder.from(api.updateSubstanceJson(withRel)).build();

        List<Relationship> relationships = storedWithRel.relationships;
        assertEquals(Arrays.asList(transporter.getOrGenerateUUID().toString()), relationships.stream()
                                                                            .map(r-> r.relatedSubstance.refuuid)
                                                                            .collect(Collectors.toList()));

        Substance storedTransporter = SubstanceBuilder.from(api.submitSubstance(transporter)).build();

        List<Relationship> otherRelationships = storedTransporter.relationships;
        assertEquals(Arrays.asList(inhibitor.getOrGenerateUUID().toString()), otherRelationships.stream()
                .map(r-> r.relatedSubstance.refuuid)
                .collect(Collectors.toList()));

    }

        private static <T> Set<T> setOf(T... ts){
        Set<T> s = new HashSet<>();
        for(T t : ts){
            s.add(t);
        }
        return s;
    }

    /**
     * This tests to make sure if for some reason
     * the first commit fails,
     * follow up attempts still work - our procesor's set of ids in progress handle rollbacks
     *
     */
    /*
    @Test
    public void rollbackFirstTryAllow2ndFixed() {
        Substance inhibitor = new SubstanceBuilder()
                .addName("inhibitor")
                .generateNewUUID()
                .build();


        Substance transporter = new SubstanceBuilder()
                .addName("transporter")
                .generateNewUUID()
                .build();

        Substance storedTransporter = SubstanceBuilder.from(api.submitSubstance(transporter)).build();

        JsonNode withRel = SubstanceBuilder.from(api.submitSubstance(inhibitor))
                .addRelationshipTo(storedTransporter, "INHIBITOR->TRANSPORTER")
                .buildJson();

        Substance storedWithRel = SubstanceBuilder.from(api.updateSubstanceJson(withRel)).build();


        assertEquals(Arrays.asList("INHIBITOR->TRANSPORTER"), storedWithRel.relationships.stream()
                .map(r -> r.type)
                .collect(Collectors.toList()));

        Substance actualTransporter = api.fetchSubstanceObjectByUuid(transporter.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("TRANSPORTER->INHIBITOR"), actualTransporter.relationships.stream()
                .map(r -> r.type)
                .collect(Collectors.toList()));


        ts.addEntityProcessor(Relationship.class, ProcessorThatFailsFirstTime.class );

        logout();

        ts.restart();

        login();

        storedWithRel.relationships.get(0).type= "KIRK->TRANSPORTER";
        JsonNode updatedJson = storedWithRel.toFullJsonNode();

        SubstanceJsonUtil.ensureFailure(api.updateSubstance(updatedJson));

        Substance storedWithRel2 = SubstanceBuilder.from(api.updateSubstanceJson(updatedJson)).build();


        assertEquals(Arrays.asList("KIRK->TRANSPORTER"), storedWithRel2.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));


        Substance actualTransporter2 = api.fetchSubstanceObjectByUuid(transporter.getUuid().toString(), Substance.class);

        assertEquals(Arrays.asList("TRANSPORTER->KIRK"), actualTransporter2.relationships.stream()
                .map(r-> r.type)
                .collect(Collectors.toList()));


    }

    /**
     * processor that is used in a test that will throw an exception
     * and therefore rollback the transaction the first time it's update method is called.
     */
    /*
    public static class ProcessorThatFailsFirstTime implements EntityProcessor<Relationship> {

        private boolean shouldFail=true;

        @Override
        public void preUpdate(Relationship obj) throws FailProcessingException {
            try {
                if(shouldFail) {
                    throw new FailProcessingException("supposed to fail");
                }
            }finally {
                //only fail the 1st time
                shouldFail = false;
            }
        }
    }
*/
}
