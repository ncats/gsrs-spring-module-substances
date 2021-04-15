package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.Subunit;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubunitRepository extends GsrsVersionedRepository<Subunit, UUID>{
}
