package example.substance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.io.InputStreamSupplier;

import ix.ginas.models.v1.Substance;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;


import java.io.*;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class HotFixIssue871 extends AbstractSubstanceJpaEntityTest {
	 	@Test
		@WithMockUser(username = "admin", roles = "Admin")
	    public void viewingSubstanceWithNonExistentRelatedSubstanceInDatabaseWithConceptShouldNotFail()throws IOException {


				loadGsrsFile(new ClassPathResource("/testdumps/smallMention.txt"));

	              Optional<Substance> opt= substanceEntityService.flexLookup("8a184573");
	                assertTrue(opt.isPresent());
	                


	    }

}
