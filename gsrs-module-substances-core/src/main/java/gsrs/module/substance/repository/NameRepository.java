package gsrs.module.substance.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.Name;

@Repository
@Transactional
public interface NameRepository extends GsrsVersionedRepository<Name, UUID> {


    //TODO: use summary object
    @Query("select s from Name s where s.owner.uuid = ?1 and s.displayName = true")
    Optional<Name> findDisplayNameByOwnerID(UUID uuid);

    @Query("select s from Name s where s.owner.uuid = ?1")
    List<Name> findNameByOwnerIDDisplayNameTrue(UUID uuid);
    
    List<Name> findByNameIgnoreCase(String name);

    //hibernate query will not convert uuid into a string so we have to concatenate it with empty string for this to work.
    @Query("select s from Name s where CONCAT(s.uuid, '') like ?1%")
    List<Name> findByUuidStartingWith(String partialUUID);

    @Query("select n.uuid from Name n")
    List<String> getAllUuids();

}
