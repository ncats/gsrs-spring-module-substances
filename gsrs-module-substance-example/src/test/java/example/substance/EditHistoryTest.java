package example.substance;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.junit.json.Changes;
import gsrs.junit.json.ChangesBuilder;
import gsrs.junit.json.JsonUtil;
import ix.core.util.EntityUtils;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditHistoryTest extends AbstractSubstanceJpaEntityTest {


	@Test
	@WithMockUser(username = "admin", roles = "Admin")
	public void testRecordHistoryEditCanProduceDiff() {
    			UUID uuid = UUID.randomUUID();
    			
    			new SubstanceBuilder()
    				.addName("Concept Name")
    				.setUUID(uuid)
    				.buildJsonAnd(this::assertCreated);

    			Optional<Substance> old= substanceEntityService.get(uuid);

		JsonNode oldJson = EntityUtils.EntityWrapper.of(old.get()).toJsonDiffJsonNode();
    			old.get().toBuilder()
    				.addName("another name")
    				.buildJsonAnd(this::assertUpdated);
		Optional<Substance> updated = substanceEntityService.get(uuid);
		JsonNode newJson = EntityUtils.EntityWrapper.of(updated.get()).toJsonDiffJsonNode();


		Changes actualChanges = JsonUtil.computeChanges(oldJson, newJson);



		Changes expectedChanges = new ChangesBuilder(oldJson, newJson)
										.replace("/lastEdited")
										.replace("/version")
										.added("/names/1/displayName")
										.build();

//					System.out.println("actual changes = " + actualChanges);

		Changes missingFrom = expectedChanges.missingFrom(actualChanges);
		assertTrue(missingFrom.isEmpty(), () ->missingFrom.toString() +"\n expected = " + expectedChanges + "\nactual = " + actualChanges +  "\nmissingFrom = " + missingFrom);



    	
	}
/*
	@Test
	public void approvingASubstanceMakesAEditForThePreviousVersion() throws Exception {
		UUID uuid = UUID.randomUUID();
		try(RestSession session = ts.newRestSession(ts.createUser(Role.SuperDataEntry,
				Role.SuperUpdate))){
			SubstanceAPI api2 = new SubstanceAPI(session);

			new SubstanceBuilder()
					.asChemical()
					.setStructure("C1CCCCC1CCCCO")
					.addName("Chemical Name")
					.setUUID(uuid)
					.buildJsonAnd(c->{
						ensurePass(api2.submitSubstance(c));
					});
		}

		try(RestSession session = ts.newRestSession(ts.createUser(Role.SuperDataEntry,
				Role.SuperUpdate, Role.Approver))){
			SubstanceAPI api2 = new SubstanceAPI(session);

			api2.approveSubstance(uuid.toString());
			JsonNode afterApprove=api2.fetchSubstanceJsonByUuid(uuid.toString());
			assertEquals("2", afterApprove.get("version").asText());

			JsonHistoryResult oldVersion = api2.fetchSubstanceJsonByUuid(uuid.toString(),1);

			assertEquals("1", oldVersion.getHistoryNode().get("version").asText());

		}


	}
    */
    
    
}
