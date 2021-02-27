package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.Substance;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StructureRepository extends GsrsVersionedRepository<Structure, UUID> {
}
