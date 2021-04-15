package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface NameRepository extends GsrsVersionedRepository<Name, UUID> {

    List<Name> findByNameIgnoreCase(String name);

    //hibernate query will not convert uuid into a string so we have to concatenate it with empty string for this to work.
    @Query("select s from Name s where CONCAT(s.uuid, '') like ?1%")
    List<Name> findByUuidStartingWith(String partialUUID);
}
