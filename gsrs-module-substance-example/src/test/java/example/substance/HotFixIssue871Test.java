package example.substance;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
//@ActiveProfiles("test")
//@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class HotFixIssue871Test extends AbstractSubstanceJpaEntityTest {
	 	@Test
		@WithMockUser(username = "admin", roles = "Admin")
	    public void viewingSubstanceWithNonExistentRelatedSubstanceInDatabaseWithConceptShouldNotFail()throws IOException {


				loadGsrsFile(new ClassPathResource("/testdumps/smallMention.txt"));

	              Optional<Substance> opt= substanceEntityService.flexLookup("8a184573");
	                assertTrue(opt.isPresent());
	                


	    }

}
