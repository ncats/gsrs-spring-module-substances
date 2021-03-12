package gsrs.module.substance.substance.processor;

import com.fasterxml.jackson.databind.JsonNode;

import gsrs.module.substance.processors.PublicTagFlagger;
import gsrs.module.substance.substance.AbstractSubstanceJpaEntityTest;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestEntityProcessorFactory;
import ix.core.controllers.EntityFactory;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Reference;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class PublicTagAddITTest extends AbstractSubstanceJpaEntityTest {



	@Autowired
	private TestEntityProcessorFactory entityProcessorFactory;

	@BeforeEach
	public void setup() {
		PublicTagFlagger sut = new PublicTagFlagger();
		AutowireHelper.getInstance().autowire(sut);
		entityProcessorFactory.setEntityProcessors(sut);
	}




	@Test
	@WithMockUser(username = "admin", roles="Admin")
	public void ensureMissingPublicTagGetsAddedWithProcessor() throws IOException {
		try {
			UUID uuid = UUID.randomUUID();
			new SubstanceBuilder().addName("Test Guy")
					.setUUID(uuid)
					.andThenMutate(s -> {
				s.references.stream().forEach(r -> {
					r.tags.clear();
				});
			}).buildJsonAnd(this::assertCreated);


			JsonNode jsn = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().toJsonNode(substanceEntityService.get(uuid).get());
			long publicRefs= StreamSupport.stream(jsn.at("/references").spliterator(),false)
					.filter(js->{
						JsonNode jsl=js.at("/tags");
						if(!jsl.isMissingNode()){
							boolean hasPub=false;
							boolean hasAuto=false;
							for(JsonNode tag: jsl){
								if(tag.asText().equals(Reference.PUBLIC_DOMAIN_REF)){
									hasPub=true;
								}
								if(tag.asText().equals(PublicTagFlagger.AUTO_SELECTED)){
									hasAuto=true;
								}
							}
							return hasPub&&hasAuto;
						}
						return false;
					}).count();
			assertEquals(1,publicRefs);
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}

	}

}
