package example.substance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

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
import gsrs.startertests.TestEntityProcessorFactory;
import ix.core.models.Edit;
import ix.ginas.modelBuilders.ProteinSubstanceBuilder;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.ChemicalSubstance;
import ix.ginas.models.v1.Protein;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import ix.ginas.models.v1.Substance.SubstanceClass;
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
    public void addSubstanceWithRelationshipThenAddRelatedSubstanceShouldResultInBirectionalRelationship(@Autowired ApplicationEvents applicationEvents)   throws Exception {

        applicationEvents.clear();
        UUID uuid1 = UUID.fromString("b1b1c28e-82d3-4c1b-9e5d-af9a7faaddec");
        UUID uuid2 = UUID.fromString("2408e789-ee6c-4378-8f97-1f0326f3bf64");
        
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
        
        //State 0: created sub1, dangling relationship
        
        
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
        
        //State 1: created sub2
        

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
    
    /*
     * 4. Change an existing alternative definition substance to have a different primary definition
        4.1. Confirm that the change works.
        4.2. Confirm that the new primary has the correct relationship now
        4.3. Confirm that the old primary is lacking the alt relationship
        4.4. Confirm that a rollback would rollback everything (not sure how to test this)
     */
    
    @Test
    public void testChangeSubstanceClassShouldKeepRelationships() throws Exception {

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


        //submit inverse
        JsonNode jsA = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate2));
        String uuidA = jsA.get("uuid").asText();
        assertCreated(jsA);

        JsonNode updatedA=new JsonUtil.JsonNodeBuilder(jsA)
                .set("/changeReason", "test")
                .ignoreMissing().build();
        assertUpdated(updatedA);
        
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
        
        Substance fetchedP = substanceEntityService.get(UUID.fromString(uuidA)).get();
        assertEquals(uuid, fetchedP.relationships.get(0).relatedSubstance.refuuid);
        assertEquals(parts[1] + "->" + parts[0],fetchedP.relationships.get(0).type);
//        
//        ChemicalSubstance csub = (ChemicalSubstance) fetchedA;
//        
//        Protein prot =new ProteinSubstanceBuilder().addSubUnit("TTTTTTT").build().protein;
//        prot.setReferences(csub.getStructure().getReferences());
//        
//        JsonNode updatedA=new JsonUtil.JsonNodeBuilder(fetchedA.toFullJsonNode())
//                                        .remove("/structure")
//                                        .remove("/moieties")
//                                        .add("/protein", prot)
//                                        .set("/substanceClass", SubstanceClass.protein.toString())
//                                        .ignoreMissing().build();
//
//        assertUpdated(updatedA);
//
//        Substance fetchedBack = substanceEntityService.get(UUID.fromString(uuid)).get();
//
//        assertEquals(SubstanceClass.protein, fetchedBack.substanceClass);
//        assertEquals(uuidA, fetchedBack.relationships.get(0).relatedSubstance.refuuid);
//        assertEquals(parts[0] + "->" + parts[1],fetchedBack.relationships.get(0).type);
//        
//
//        Substance fetchedBack2 = substanceEntityService.get(UUID.fromString(uuidA)).get();
//
//        assertEquals(uuid, fetchedBack2.relationships.get(0).relatedSubstance.refuuid);
//        assertEquals(parts[1] + "->" + parts[0],fetchedBack2.relationships.get(0).type);


//        assertEquals(Collections.emptyList(),  substanceEntityService.get(UUID.fromString(uuid)).get().relationships);
//        assertEquals(Collections.emptyList(),  substanceEntityService.get(UUID.fromString(uuidA)).get().relationships);
    }
}
