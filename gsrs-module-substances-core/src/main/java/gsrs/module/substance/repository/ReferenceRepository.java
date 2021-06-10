package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.core.models.Structure;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Reference;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface ReferenceRepository extends GsrsVersionedRepository<Reference, UUID> {

    //hibernate query will not convert uuid into a string so we have to concatenate it with empty string for this to work.
    @Query("select s from Reference s where CONCAT(s.uuid, '') like ?1%")
    List<Reference> findByUuidStartingWith(String partialUUID);
}
