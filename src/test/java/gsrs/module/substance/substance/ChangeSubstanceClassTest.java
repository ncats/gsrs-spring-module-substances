package gsrs.module.substance.substance;

import com.fasterxml.jackson.databind.JsonNode;


import gsrs.junit.json.JsonUtil;
import gsrs.module.substance.SubstanceJsonUtil;
import gsrs.module.substance.SubstanceTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;


import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
@WithMockUser(username = "admin", roles="Admin")
@Transactional
public class ChangeSubstanceClassTest extends AbstractSubstanceJpaEntityTest {


     File resource;

     @BeforeEach
     public void setup() throws IOException {
         resource=new ClassPathResource("testJSON//toedit.json").getFile();
     }

    @Test
    public void changeProteinToConceptTest(){
		JsonNode entered = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(resource));

        JsonNode retrievedProtein=assertCreated(entered).toFullJsonNode();
        JsonNode toSubmitConcept=
            		new JsonUtil.JsonNodeBuilder(retrievedProtein)
            		.remove("/protein")
            		.set("/substanceClass", "concept")
            		.build();
        JsonNode retrievedConcept=assertUpdated(toSubmitConcept).toFullJsonNode();
        assertEquals("concept",retrievedConcept.at("/substanceClass").asText());


    }
    @Test
    public void changeProteinToChemicalTest(){
        JsonNode entered = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(resource));

        JsonNode achemical= SubstanceTestUtil.makeChemicalSubstance("C1CCCCCCCC1").toFullJsonNode();
            JsonNode retrievedProtein=assertCreated(entered).toFullJsonNode();
           
            
            JsonNode toSubmitConcept=
            		new JsonUtil.JsonNodeBuilder(retrievedProtein)
            		.remove("/protein")
            		.set("/substanceClass", "chemical")
            		.set("/structure", achemical.at("/structure"))
            		.set("/moieties", achemical.at("/moieties"))
                    .append("/references/-", achemical.at("/references"))
//            		.add("/references/-", achemical.at("/references/0"))
            		.build();

            JsonNode retrievedChemical=assertUpdated(toSubmitConcept).toFullJsonNode();
            assertEquals("chemical",retrievedChemical.at("/substanceClass").asText());
            assertTrue("New chemical should have structure",retrievedChemical.at("/structure/molfile").asText().length()>0);

    }
    
    @Test
   	public void testPromoteConceptToProtein() throws Exception {
        JsonNode entered = SubstanceJsonUtil.prepareUnapprovedPublic(JsonUtil.parseJsonFile(resource));

        JsonNode concept=new JsonUtil
            .JsonNodeBuilder(entered)
            .remove("/protein")
            .set("/substanceClass", "concept")
            .build();
           	String uuid=entered.get("uuid").asText();

            JsonNode fetched=assertCreated(concept).toFullJsonNode();

            assertEquals(fetched.at("/substanceClass").asText(), "concept");
            JsonNode updated = new JsonUtil
                    .JsonNodeBuilder(fetched)
                    .add("/protein",entered.at("/protein"))
                    .set("/substanceClass", "protein")
                    .build();

            JsonNode fetchedagain=assertUpdated(updated).toFullJsonNode();
            assertEquals(fetchedagain.at("/substanceClass").asText(), "protein");
            
            

   	}
}
