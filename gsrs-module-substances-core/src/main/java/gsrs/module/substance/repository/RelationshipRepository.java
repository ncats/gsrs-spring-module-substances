package gsrs.module.substance.repository;

import gsrs.repository.GsrsRepository;
import ix.ginas.models.v1.Relationship;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
@Repository

public interface RelationshipRepository extends GsrsRepository<Relationship, UUID> {

    List<Relationship> findByOriginatorUuid(String originatorUuid);

    List<Relationship> findByRelatedSubstance_Refuuid(String refuuid);
}
