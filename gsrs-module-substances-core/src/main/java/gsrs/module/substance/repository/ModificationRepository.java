package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.core.models.Structure;
import ix.ginas.models.v1.Modifications;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface ModificationRepository extends GsrsVersionedRepository<Modifications, UUID> {

    List<Modifications> findByStructuralModifications_molecularFragment_refuuid(String refuuid);
}
