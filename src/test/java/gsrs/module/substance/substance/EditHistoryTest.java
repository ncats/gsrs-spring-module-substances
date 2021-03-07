package gsrs.module.substance.substance;

import com.fasterxml.jackson.databind.JsonNode;

import gov.nih.ncats.common.sneak.Sneak;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.junit.json.Changes;
import gsrs.junit.json.ChangesBuilder;
import gsrs.junit.json.JsonUtil;
import gsrs.module.substance.GsrsModuleSubstanceApplication;
import gsrs.module.substance.SubstanceEntityService;
import gsrs.repository.EditRepository;
import gsrs.service.GsrsEntityService;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.TestGsrsValidatorFactory;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.Edit;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.core.util.EntityUtils;
import ix.core.validator.ValidationResponse;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Substance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EditHistoryTest extends AbstractSubstanceJpaEntityTest {







	@Test
	@WithMockUser(username = "admin", roles = "Admin")
	public void testRecordHistoryEditCanProduceDiff() throws Exception {
    			UUID uuid = UUID.randomUUID();
    			
    			new SubstanceBuilder()
    				.addName("Concept Name")
    				.setUUID(uuid)
    				.buildJsonAnd(this::assertCreated);
    			System.out.println("fetching uuid " + uuid);
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
