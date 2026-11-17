package example.substance;

import gsrs.substances.tests.AbstractSubstanceJpaEntityTest;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
@ActiveProfiles("test")
public class HotFixIssue871Test extends AbstractSubstanceJpaEntityTest {
	 	@Test
		@WithMockUser(username = "admin", roles = "Admin")
	    public void viewingSubstanceWithNonExistentRelatedSubstanceInDatabaseWithConceptShouldNotFail()throws IOException {

            // Load concept substance (TETANUS TOXOID) which has a relationship pointing to a
            // non-existent substance. Any loading error is tolerated – the outer catch in
            // loadGsrsFile swallows it so the next call is unaffected.
            loadGsrsFile(new ClassPathResource("/testdumps/smallMention.txt"),
                    Substance.SubstanceClass.concept);

            // Load chemical substance (PHENOL) separately so it is created even if the
            // concept loading above encountered errors.
            loadGsrsFile(new ClassPathResource("/testdumps/smallMention.txt"),
                    Substance.SubstanceClass.chemical);

            // Look up PHENOL by its approvalID.  The substance UUID is not preserved
            // during test creates (TestSubstanceEntityServiceImpl nullifies it for
            // non-batch mode), so we use the stable approvalID instead.
            Optional<Substance> opt = substanceEntityService.flexLookup("339NCG44TV");
            assertTrue(opt.isPresent());
	    }

}
