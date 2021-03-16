package gsrs.module.substance.repository;

import gsrs.model.GsrsApiAction;
import gsrs.module.substance.processors.RelationshipProcessor;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.utils.RelationshipUtil;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
@GsrsJpaTest
@ActiveProfiles("test")
public class RelationshipRepositoryITTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private SubstanceRepository substanceRepository;

    private RelationshipProcessor relationshipProcessor;

    @BeforeEach
    public void setup(){
        relationshipProcessor = new RelationshipProcessor();
        AutowireHelper.getInstance().autowire(relationshipProcessor);
    }

    @Test
    public void save(){
        Relationship r = new Relationship();
        r.type = "Foo -> Bar";
        UUID uuid = UUID.randomUUID();
        r.uuid = uuid;
        Relationship saved = relationshipRepository.save(r);

        assertEquals(uuid,saved.uuid);
        assertEquals(uuid.toString(), saved.originatorUuid);
        assertTrue(saved.isGenerator());

    }

    @Test
    public void fetchByOriginatorUUID(){

        Substance s = new SubstanceBuilder()
                            .addName("testSubstance")
                .build();
        Substance savedSubstance = substanceRepository.save(s);

        Substance sB = new SubstanceBuilder()
                .addName("testSubstanceB")
                .build();
        Substance savedSubstanceB = substanceRepository.save(sB);
        Relationship r = new Relationship();
        r.type = "Foo->Bar";
        UUID uuid = UUID.randomUUID();
        r.uuid = uuid;
        r.relatedSubstance = savedSubstanceB.asSubstanceReference();
        r.setOwner(savedSubstance);


        Relationship saved = relationshipRepository.save(r);

        Relationship other = relationshipProcessor.createAndAddInvertedRelationship(saved, savedSubstance.asSubstanceReference(), savedSubstanceB);

        Relationship savedOther = relationshipRepository.save(other );

        List<Relationship> list = relationshipRepository.findByOriginatorUuid(uuid.toString());
        assertEquals(2, list.size());
        Set<UUID> actual =list.stream().map(Relationship::getUuid).collect(Collectors.toSet());

        assertEquals(new LinkedHashSet<>(Arrays.asList(uuid, savedOther.uuid)), actual);

    }
}
