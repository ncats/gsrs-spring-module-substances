package example.repository;

import example.substance.AbstractSubstanceJpaEntityTest;
import gsrs.module.substance.repository.RelationshipRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.processors.RelationshipProcessor;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.ginas.modelBuilders.SubstanceBuilder;
import ix.ginas.models.v1.Relationship;
import ix.ginas.models.v1.Substance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

//@GsrsJpaTest
//@ActiveProfiles("test")
public class RelationshipRepositoryITTest extends AbstractSubstanceJpaEntityTest {

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private SubstanceRepository substanceRepository;


    @Autowired
    private PlatformTransactionManager platformTransactionManager;



    @Test
    public void save(){
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        UUID uuid = UUID.randomUUID();
        Relationship saved =transactionTemplate.execute( s-> {
                    Relationship r = new Relationship();
                    r.type = "Foo -> Bar";

                    r.uuid = uuid;
                    return relationshipRepository.save(r);
                });
        assertEquals(uuid,saved.uuid);
        assertEquals(uuid.toString(), saved.originatorUuid);
        assertTrue(saved.isGenerator());

    }

    @Test
    public void fetchByOriginatorUUID(){

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        List<UUID> uuids = transactionTemplate.execute( status-> {
                    Substance s = new SubstanceBuilder()
                            .addName("testSubstance")
                            .build();
                    Substance savedSubstance = assertCreated(s.toFullJsonNode());

                    Substance sB = new SubstanceBuilder()
                            .addName("testSubstanceB")
                            .build();
                    Substance savedSubstanceB = assertCreated(sB.toFullJsonNode());
                    Relationship r = new Relationship();
                    r.type = "Foo->Bar";
                    UUID uuid = UUID.randomUUID();
                    r.uuid = uuid;
                    r.relatedSubstance = savedSubstanceB.asSubstanceReference();
                    r.setOwner(savedSubstance);


                    Relationship saved = relationshipRepository.save(r);

                    //Relationship other = relationshipProcessor.createAndAddInvertedRelationship(saved, savedSubstance.asSubstanceReference(), savedSubstanceB);
                    Relationship other = new Relationship();
                    other.type = "Bar->Foo";
                    other.originatorUuid = uuid.toString();
                    other.uuid = UUID.randomUUID();
                    other.relatedSubstance = savedSubstance.asSubstanceReference();
                    other.setOwner(savedSubstanceB);
                    Relationship savedOther = relationshipRepository.save(other);

                    return Arrays.asList(uuid, savedOther.uuid);
                });
        transactionTemplate.executeWithoutResult( s-> {
            List<Relationship> list = relationshipRepository.findByOriginatorUuid(uuids.get(0).toString());
            assertEquals(2, list.size());
            Set<UUID> actual = list.stream().map(Relationship::getUuid).collect(Collectors.toSet());

            assertEquals(new LinkedHashSet<>(uuids), actual);
        });

    }
}
