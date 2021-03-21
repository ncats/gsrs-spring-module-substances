package gsrs.module.substance.substance;

import com.fasterxml.jackson.databind.JsonNode;

import gsrs.junit.json.JsonUtil;
import gsrs.module.substance.SubstanceJsonUtil;
import gsrs.module.substance.processors.ReferenceProcessor;
import gsrs.module.substance.processors.RelationshipProcessor;
import gsrs.module.substance.processors.SubstanceProcessor;
import gsrs.repository.EditRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.service.GsrsEntityService;
import gsrs.services.PrincipalService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.TestEntityProcessorFactory;
import ix.core.EntityProcessor;
import ix.core.models.Edit;
import ix.core.models.Principal;
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

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
@GsrsJpaTest
@ActiveProfiles("test")
@Import(RelationshipInvertTest.Configuration.class)
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

        AutowireHelper.getInstance().autowire(substanceProcessor);
        AutowireHelper.getInstance().autowire(relationshipProcessor);
        AutowireHelper.getInstance().autowire(referenceProcessor);
        principalService.registerIfAbsent("admin");

        em.flush();
    }
    
    @Test
    public void addSubstanceWithRelationshipThenAddRelatedSubstanceShouldResultInBirectionalRelationship()   throws Exception {
        //submit primary, with dangling relationship
        JsonNode js = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate1));
        String uuid = js.get("uuid").asText();
        this.assertCreated(js);

        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(js);
        String[] parts=type1.split("->");
        
        //submit the dangled
        JsonNode jsA = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate2));
        String uuidA = jsA.get("uuid").asText();
        this.assertCreated(jsA);

        //confirm that the dangled has a relationship to the dangler 
        Relationship relationship = substanceEntityService.get(UUID.fromString(uuidA)).get().relationships.get(0);

        assertEquals(uuid, relationship.relatedSubstance.refuuid);
        assertEquals(parts[1] + "->" + parts[0],relationship.type);
    }
    

    @Test
    public void removeSourceRelationshipShouldRemoveInvertedRelationship()   throws Exception {
    	
	    	 //submit primary, with dangling relationship
	        JsonNode js = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate1));
	        String uuid = js.get("uuid").asText();
	       assertCreated(js);
	        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(js);
	        String[] parts=type1.split("->");
	        //submit the dangled
	
	        JsonNode jsA = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(invrelate2));
	        String uuidA = jsA.get("uuid").asText();
	        assertCreated(jsA);

	        Substance fetchedA = substanceEntityService.get(UUID.fromString(uuidA)).get();
	        Relationship relationshipA = fetchedA.relationships.get(0);
	        //confirm that the dangled has a relationship to the dangler 

	        assertEquals(uuid, relationshipA.relatedSubstance.refuuid);
	        assertEquals(parts[1] + "->" + parts[0], relationshipA.type);


	        //Remove the primary relationship, ensure the inverse is gone
	        JsonNode update=new JsonUtil.JsonNodeBuilder(substanceEntityService.get(UUID.fromString(uuid)).get().toFullJsonNode())
									.remove("/relationships/0")
									.ignoreMissing()
									.build();
	        
	       assertUpdated(update);
	        List<Relationship> relationships = substanceEntityService.get(UUID.fromString(uuid)).get().relationships;


	        assertEquals(update.at("/relationships").size(),relationships.size());

	        
	        assertEquals(0,substanceEntityService.get(UUID.fromString(uuidA)).get().relationships.size());
	     
    }
    

    @Test
    public void addRelationshipAfterAddingEachSubstanceShouldAddInvertedRelationship()   throws Exception {
    	
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
        JsonNode updated=new JsonUtil.JsonNodeBuilder(substanceEntityService.get(UUID.fromString(uuid)).get().toFullJsonNode())
                        .add("/relationships/-", newRelate)
                        .ignoreMissing().build();
        assertUpdated(updated);
        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(updated);
        String[] parts=type1.split("->");
        
        //check inverse relationship with primary
        List<Relationship> relationships = substanceEntityService.get(UUID.fromString(uuidA)).get().relationships;
        Relationship rel = relationships.get(0);
       assertEquals(uuid, rel.relatedSubstance.refuuid);

        assertEquals(parts[1] + "->" + parts[0],rel.type);
    
    	
    }

    @Test
    public void addRelationshipAfterAddingEachSubstanceShouldAddInvertedRelationshipAndIncrementVersion()   throws Exception {
    	
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
        JsonNode updated=new JsonUtil.JsonNodeBuilder(substanceEntityService.get(UUID.fromString(uuid)).get().toFullJsonNode())
                                            .add("/relationships/-", newRelate)
                                            .ignoreMissing().build();
        assertUpdated(updated);

        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(updated);
        String[] parts=type1.split("->");
        
        //check inverse relationship with primary
        Substance substanceA = substanceEntityService.get(UUID.fromString(uuidA)).get();
        Relationship relationshipA = substanceA.relationships.get(0);

        assertEquals(uuid, relationshipA.relatedSubstance.refuuid);
        assertEquals(parts[1] + "->" + parts[0],relationshipA.type);
    	assertEquals("2", substanceA.version);
    	
    }
    


    @Test
    public void addRelationshipAfterAddingEachSubstanceShouldAddInvertedRelationshipAndShouldBeInHistoryOnlyOnce()   throws Exception {
    	
    		//This is very hard to read right now. A substanceBuilder would make this easy.
    		
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

        //add relationship To
        JsonNode updated=new JsonUtil.JsonNodeBuilder(substanceEntityService.get(UUID.fromString(uuid)).get().toFullJsonNode())
								.add("/relationships/-", newRelate)
								.ignoreMissing()
								.build();
        assertUpdated(updated);
        //update the substance for real

        
        
        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(updated);
        String[] parts=type1.split("->");
        
        //check inverse relationship with primary
        Substance substanceA = substanceEntityService.get(UUID.fromString(uuidA)).get();

        Relationship relationshipA = substanceA.relationships.get(0);
        String refUuidA = relationshipA.relatedSubstance.refuuid;

        assertEquals(uuid, refUuidA);
        assertEquals(parts[1] + "->" + parts[0],relationshipA.type);
    	assertEquals("2",substanceA.version);
    	
    	
    	//This part is broken?
    	//Doesn't even return? 
    	//Probably not actually being considered an edit!

    	List<Edit> edits = editRepository.findByRefidOrderByCreatedDesc(uuid);
    	assertEquals(1, edits.size());

    	assertEquals("1", edits.get(0).version);
    	
    }

    @Test
    public void addRelationshipAfterAddingEachSubstanceShouldAddInvertedRelationshipAndShouldBeInHistory()   throws Exception {
    	
    		//This is very hard to read right now. A substanceBuilder would make this easy.
    		
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

        entityManager.flush();

        JsonNode beforeA = substanceEntityService.get(UUID.fromString(uuidA)).get().toFullJsonNode();
        
       
        //add relationship To
        JsonNode updated=new JsonUtil.JsonNodeBuilder(substanceEntityService.get(UUID.fromString(uuid)).get().toFullJsonNode())
								.add("/relationships/-", newRelate)
								.ignoreMissing()
								.build();
        
        //update the substance for real
        assertUpdated(updated);
        
        
        String type1=SubstanceJsonUtil.getTypeOnFirstRelationship(updated);
        String[] parts=type1.split("->");
        
        //check inverse relationship with primary
        Substance substanceA = substanceEntityService.get(UUID.fromString(uuidA)).get();

        Relationship relationshipA = substanceA.relationships.get(0);
        String refUuidA = relationshipA.relatedSubstance.refuuid;
        
        assertTrue(refUuidA.equals(uuid));
        assertEquals(parts[1] + "->" + parts[0],relationshipA.type);
    	assertEquals("2",substanceA.version);
    	
    	
    	//This part is broken?
    	//Doesn't even return? 
    	//Probably not actually being considered an edit!
    	JsonNode historyFetchedForFirst=editRepository.findByRefidOrderByCreatedDesc(uuid).get(0).getOldValueReference().rawJson();
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
    public void removeRelationshipWithActiveMoeityGsrs587FromOriginal() throws Exception{


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

       GsrsEntityService.CreationResult<Substance> createdInhibitor = substanceEntityService.createEntity(inhibitor.toFullJsonNode());

        em.flush();
        Substance storedTransporter = substanceEntityService.createEntity(transporter.toFullJsonNode()).getCreatedEntity();


        SubstanceBuilder.from(substanceRepository.getOne(parentUUID).toFullJsonNode())
                .addRelationshipTo(createdInhibitor.getCreatedEntity(), "INFRASPECIFIC->PARENT ORGANISM")
                .buildJsonAnd( this::assertUpdated);



        SubstanceBuilder.from(substanceRepository.getOne(inhibitor.getUuid()).toFullJsonNode())
                .addRelationshipTo(storedTransporter, "INHIBITOR -> TRANSPORTER")
                .buildJsonAnd(this::assertUpdated);

        em.flush();

        Principal admin = principalService.registerIfAbsent("admin");
        assertNotNull(admin.id);

        Substance actualTransporter = substanceRepository.findById(storedTransporter.getUuid()).get();

        assertEquals(1, actualTransporter.relationships.size());
        Substance storedWithRel = SubstanceBuilder.from(substanceRepository.findById(inhibitor.getUuid()).get().toFullJsonNode()).build();

        Relationship removedRel = storedWithRel.relationships.remove(2);
        System.out.println("removed relationship relatedSubstance with uuid " + removedRel.relatedSubstance.uuid + "  ref= " + removedRel.relatedSubstance);
        assertUpdated(storedWithRel.toFullJsonNode());



        Substance actualInhibitor =substanceRepository.findById(inhibitor.getUuid()).get();

        assertEquals(2, actualInhibitor.relationships.size());
        assertFalse(actualInhibitor.relationships.stream()
                .filter(r -> r.type.equals("INHIBITOR -> TRANSPORTER"))
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
