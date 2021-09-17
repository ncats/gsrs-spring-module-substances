package gsrs.module.substance.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.Unit;

@Repository
@Transactional
public interface StructuralUnitRepository extends GsrsVersionedRepository<Unit, UUID>{
}
