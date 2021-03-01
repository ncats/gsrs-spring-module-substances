package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.core.models.Structure;
import ix.ginas.models.v1.Reference;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReferenceRepository extends GsrsVersionedRepository<Reference, UUID> {

}
