package gsrs.module.substance.substance.processor;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.module.substance.processors.PublicTagFlagger;
import gsrs.module.substance.substance.AbstractSubstanceJpaEntityTest;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.TestEntityProcessorFactory;
import ix.core.controllers.EntityFactory;
import ix.core.models.Group;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Reference;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class PublicTagAddTest {



    PublicTagFlagger sut = new PublicTagFlagger();




	@Test
	public void hasPublicReferenceNoChange() throws Exception {
		Reference r = new Reference();
		r.publicDomain = true;
		r.addTag(Reference.PUBLIC_DOMAIN_REF);
		Substance s  =new Substance();
		s.references.add(r);
		sut.prePersist(s);

		assertEquals(Arrays.asList(r), s.references);


	}
	@Test
	public void hasPublicDomainReferenceButNotMarkedPublicDomainRefTagAdd() throws Exception {
		Reference r = new Reference();
		r.publicDomain = true;

		Substance s  =new Substance();
		s.references.add(r);

		assertFalse(r.isPublicReleaseReference());

		sut.prePersist(s);

		assertTrue(r.isPublicDomain());
		assertTrue(r.isPublicReleaseReference());


	}

	@Test
	public void notPublicNoChange() throws Exception {
		Reference r = new Reference();
		Substance s = new Substance();
		s.setAccess(Collections.singleton(new Group("foo")));
		sut.prePersist(s);

		assertTrue(s.references.isEmpty());


	}

}
