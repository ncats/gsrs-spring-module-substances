package gsrs.module.substance.substance.substance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.io.InputStreamSupplier;

import ix.ginas.models.v1.Substance;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;


import java.io.*;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class HotFixIssue871 extends AbstractSubstanceJpaEntityTest {
	 	@Test
		@WithMockUser(username = "admin", roles = "Admin")
	    public void viewingSubstanceWithNonExistentRelatedSubstanceInDatabaseWithConceptShouldNotFail()throws IOException {


				loadGsrsFile("/testdumps/smallMention.txt");

	              Optional<Substance> opt= substanceEntityService.flexLookup("8a184573");
	                assertTrue(opt.isPresent());
	                


	    }
		private static final Pattern SPLIT_PATTERN = Pattern.compile("\t");
	    private void loadGsrsFile(String filePath) throws IOException{
	    	ObjectMapper mapper = new ObjectMapper();
	 		try(BufferedReader reader = new BufferedReader(new InputStreamReader(InputStreamSupplier.forResourse(getClass().getResource(filePath)).get()))){
				String line;
				while(( line = reader.readLine()) !=null){
					String[] cols =SPLIT_PATTERN.split(line);
					String json = cols[2];
					assertCreated(mapper.readTree(json)).getUuid();
				}
			}
		}
}
