package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.core.models.Structure;
import ix.ginas.models.v1.Substance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Stream;

@Repository
@Transactional
public interface StructureRepository extends GsrsVersionedRepository<Structure, UUID> {
    @Query("select s from Structure s")
    Stream<Structure> streamAll();
}
